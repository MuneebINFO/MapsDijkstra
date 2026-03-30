import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import utility.CsvLoader;
import utility.Edge;
import utility.GeoUtils;
import utility.Graph;
import utility.ItineraryFinder;
import utility.Node;
import utility.Route;
import utility.Stop;
import utility.StopTime;
import utility.Trip;

// Application entry point for loading GTFS data and computing itineraries.
public class Main {

    private static final double MAX_WALKING_DISTANCE = 750.0;
    private static final double WALKING_SPEED = 1.4;
    private static final int MIN_WAIT_DURATION = 60;
    private static final int MIN_TRANSFER_DURATION = 90;

    private static Map<String, Stop> stops = new HashMap<>();
    private static Map<String, Route> routes = new HashMap<>();
    private static Map<String, Trip> trips = new HashMap<>();
    private static Map<String, List<StopTime>> stopTimes = new HashMap<>();

    // Loads all GTFS datasets that are needed for the requested departure time.
    private static void loadData(LocalTime timeDeparture) {
        try {
            System.out.println("\nLoading data:");
            List<String> agencies = List.of("STIB", "SNCB", "DELIJN", "TEC");
            for (String agency : agencies) {
                System.out.println("   Loading " + agency + "...");
                stops.putAll(CsvLoader.loadStops("data/GTFS/" + agency + "/stops.csv"));
                routes.putAll(CsvLoader.loadRoutes("data/GTFS/" + agency + "/routes.csv"));
                trips.putAll(CsvLoader.loadTrips("data/GTFS/" + agency + "/trips.csv"));
                stopTimes.putAll(CsvLoader.loadStopTimes("data/GTFS/" + agency + "/stop_times.csv", timeDeparture));
                System.out.println("   Finished loading " + agency + ".");
            }
            System.out.println("Loading complete.\n");
        } catch (IOException e) {
            System.err.println("Error while loading files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Returns the IDs of the departure and arrival stops.
    private static List<List<String>> getStopIDs(String nomDepart, String nomArrivee) {
        List<String> allStopStartID = new ArrayList<>();
        List<String> allStopEndID = new ArrayList<>();
        List<List<String>> allStopIDs = new ArrayList<>();

        for (Stop stop : stops.values()) {
            if (stop.name.equals(nomDepart)) {
                allStopStartID.add(stop.id);
            }
            if (stop.name.equals(nomArrivee)) {
                allStopEndID.add(stop.id);
            }
        }

        allStopIDs.add(allStopStartID);
        allStopIDs.add(allStopEndID);

        return allStopIDs;
    }

    // Adds walking edges between nearby stops that are reachable from the start area.
    private static void addWalkingEdges(Graph graph, Collection<Stop> stops, Set<String> relevantStopIds) {
        Map<String, List<Node>> stopIdToNodes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            stopIdToNodes.computeIfAbsent(node.stopId, k -> new ArrayList<>()).add(node);
        }

        List<Stop> stopList = new ArrayList<>(stops);

        for (int i = 0; i < stopList.size(); i++) {
            Stop stopA = stopList.get(i);
            if (!relevantStopIds.contains(stopA.id)) {
                continue;
            }

            for (int j = i + 1; j < stopList.size(); j++) {
                Stop stopB = stopList.get(j);
                if (!relevantStopIds.contains(stopB.id)) {
                    continue;
                }

                double dist = GeoUtils.distance(stopA.lat, stopA.lon, stopB.lat, stopB.lon);
                if (dist > MAX_WALKING_DISTANCE) {
                    continue;
                }

                int estimatedTime = (int) (dist / WALKING_SPEED);
                List<Node> nodesA = stopIdToNodes.getOrDefault(stopA.id, List.of());
                List<Node> nodesB = stopIdToNodes.getOrDefault(stopB.id, List.of());
                addTimedEdges(graph, nodesA, nodesB, estimatedTime, "WALK");
                addTimedEdges(graph, nodesB, nodesA, estimatedTime, "WALK");
            }
        }
    }

    // Adds transfer edges between distinct stop IDs that are physically very close.
    private static void addTransferEdges(Graph graph, Collection<Stop> stops) {
        final double MAX_DISTANCE_METERS = 100.0;
        final double GRID_SIZE_METERS = 100.0;

        Map<String, List<Stop>> grid = new HashMap<>();
        for (Stop stop : stops) {
            String key = spatialKey(stop.lat, stop.lon, GRID_SIZE_METERS);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(stop);
        }

        Map<String, List<Node>> stopIdToNodes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            stopIdToNodes.computeIfAbsent(node.stopId, k -> new ArrayList<>()).add(node);
        }

        for (Stop stop : stops) {
            String baseKey = spatialKey(stop.lat, stop.lon, GRID_SIZE_METERS);
            int[] dx = {-1, 0, 1};
            int[] dy = {-1, 0, 1};

            for (int ix : dx) {
                for (int iy : dy) {
                    String neighborKey = shiftKey(baseKey, ix, iy);
                    List<Stop> candidates = grid.getOrDefault(neighborKey, List.of());

                    for (Stop other : candidates) {
                        if (stop.id.equals(other.id)) {
                            continue;
                        }

                        double dist = GeoUtils.distance(stop.lat, stop.lon, other.lat, other.lon);
                        if (dist > MAX_DISTANCE_METERS) {
                            continue;
                        }

                        List<Node> nodesA = new ArrayList<>(stopIdToNodes.getOrDefault(stop.id, List.of()));
                        List<Node> nodesB = new ArrayList<>(stopIdToNodes.getOrDefault(other.id, List.of()));
                        addTimedEdges(graph, nodesA, nodesB, MIN_TRANSFER_DURATION, "TRANSFER");
                        addTimedEdges(graph, nodesB, nodesA, MIN_TRANSFER_DURATION, "TRANSFER");
                    }
                }
            }
        }
    }

    // Adds wait edges inside the same stop so the pathfinder can board later trips.
    private static void addWaitingEdges(Graph graph) {
        Map<String, List<Node>> stopIdToNodes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            stopIdToNodes.computeIfAbsent(node.stopId, k -> new ArrayList<>()).add(node);
        }

        for (List<Node> nodes : stopIdToNodes.values()) {
            nodes.sort(Comparator.comparing(n -> n.time));

            for (int i = 0; i < nodes.size() - 1; i++) {
                Node from = nodes.get(i);
                Node to = nodes.get(i + 1);
                int delta = (int) Duration.between(from.time, to.time).getSeconds();

                if (delta >= MIN_WAIT_DURATION) {
                    graph.addEdge(from, to, delta, "WAIT", null, null);
                }
            }
        }
    }

    // Connects each node to the earliest compatible node that satisfies the minimum duration.
    private static void addTimedEdges(Graph graph, List<Node> fromNodes, List<Node> toNodes, int minimumDuration, String mode) {
        if (fromNodes.isEmpty() || toNodes.isEmpty()) {
            return;
        }

        fromNodes.sort(Comparator.comparing(n -> n.time));
        toNodes.sort(Comparator.comparing(n -> n.time));

        int targetIndex = 0;
        for (Node from : fromNodes) {
            LocalTime earliestArrival = from.time.plusSeconds(minimumDuration);

            while (targetIndex < toNodes.size() && toNodes.get(targetIndex).time.isBefore(earliestArrival)) {
                targetIndex++;
            }

            if (targetIndex >= toNodes.size()) {
                break;
            }

            Node to = toNodes.get(targetIndex);
            int delta = (int) Duration.between(from.time, to.time).getSeconds();
            if (delta >= minimumDuration) {
                graph.addEdge(from, to, delta, mode, null, null);
            }
        }
    }

    // Converts geographic coordinates to a simple grid key used for nearby-stop lookup.
    private static String spatialKey(double lat, double lon, double gridSizeMeters) {
        int x = (int) (lat * 100000 / gridSizeMeters);
        int y = (int) (lon * 100000 / gridSizeMeters);
        return x + "_" + y;
    }

    // Returns the neighboring grid cell key relative to a base key.
    private static String shiftKey(String baseKey, int dx, int dy) {
        String[] parts = baseKey.split("_");
        int x = Integer.parseInt(parts[0]) + dx;
        int y = Integer.parseInt(parts[1]) + dy;
        return x + "_" + y;
    }

    // Collects the stop IDs that can be reached from a start stop after the requested time.
    private static Set<String> buildReachableStopIds(Graph graph, String stopFrom, LocalTime startTime) {
        Set<String> reachable = new HashSet<>();

        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparing(n -> n.time));
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.getAllNodes()) {
            if (node.stopId.equals(stopFrom) && !node.time.isBefore(startTime)) {
                queue.add(node);
                visited.add(node);
                reachable.add(node.stopId);
            }
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            reachable.add(current.stopId);

            for (Edge edge : graph.getEdges(current)) {
                Node neighbor = edge.to;
                if (!visited.contains(neighbor) && !neighbor.time.isBefore(current.time)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return reachable;
    }

    // Looks up the graph edge that connects two consecutive nodes in a computed path.
    private static Edge getEdgeBetween(Graph graph, Node from, Node to) {
        return graph.getEdges(from).stream()
                .filter(e -> e.to.equals(to))
                .findFirst()
                .orElse(null);
    }

    // Prints the itinerary step by step, including waits and transfers.
    private static void printItinerary(Graph graph, List<Node> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            Edge edge = getEdgeBetween(graph, from, to);

            if (edge == null) {
                continue;
            }

            Stop stopFrom = stops.get(from.stopId);
            Stop stopTo = stops.get(to.stopId);
            String fromName = stopFrom != null ? stopFrom.name : "?";
            String toName = stopTo != null ? stopTo.name : "?";

            if ("WAIT".equals(edge.mode)) {
                System.out.printf("Wait at %s from %s to %s%n",
                        fromName, from.time, to.time);
                continue;
            }

            if ("WALK".equals(edge.mode) || "TRANSFER".equals(edge.mode)) {
                if (fromName.equals(toName)) {
                    System.out.printf("Transfer at %s from %s to %s%n",
                            fromName, from.time, to.time);
                } else {
                    System.out.printf("Walk from %s (%s) to %s (%s)%n",
                            fromName, from.time,
                            toName, to.time);
                }
                continue;
            }

            String operator = edge.company != null ? edge.company : "?";
            String lineNumber = edge.line != null ? edge.line : "?";
            System.out.printf("Take %s %s %s from %s (%s) to %s (%s)%n",
                    operator, edge.mode, lineNumber,
                    fromName, from.time,
                    toName, to.time);
        }
    }

    // Builds the time-expanded graph from the stop times of each trip.
    private static void buildGraph(Graph graph) {
        for (List<StopTime> stopTime : stopTimes.values()) {
            if (stopTime.size() < 2) {
                continue;
            }

            stopTime.sort(Comparator.comparingInt(st -> st.stopSequence));

            for (int i = 0; i < stopTime.size() - 1; i++) {
                StopTime from = stopTime.get(i);
                StopTime to = stopTime.get(i + 1);

                Stop s1 = stops.get(from.stopId);
                Stop s2 = stops.get(to.stopId);
                if (s1 == null || s2 == null) {
                    continue;
                }

                int duration = (int) Duration.between(from.departureTime, to.departureTime).getSeconds();
                if (duration < 0) {
                    continue;
                }

                Trip trip = trips.get(from.tripId);
                if (trip == null) {
                    continue;
                }

                Route route = routes.get(trip.routeId);
                if (route == null) {
                    continue;
                }

                String mode = route.type;
                String line = route.shortName;

                String company = "";
                if (trip.routeId != null && trip.routeId.contains("-")) {
                    company = trip.routeId.split("-")[0];
                }

                Node fromNode = new Node(from.stopId, from.departureTime, from.tripId);
                Node toNode = new Node(to.stopId, to.departureTime, to.tripId);

                graph.addEdge(fromNode, toNode, duration, mode, company, line);
            }
        }
    }

    // Clears all previously loaded GTFS data before computing a new route.
    private static void resetData() {
        stops.clear();
        routes.clear();
        trips.clear();
        stopTimes.clear();
    }

    // Reads the user input, builds the graph, and prints the best itinerary found.
    public static void main(String[] args) {
        resetData();

        Scanner input = new Scanner(System.in);

        System.out.println("Please enter the departure stop:");
        String stopStart = input.nextLine();

        System.out.println("Please enter the arrival stop:");
        String stopEnd = input.nextLine();

        System.out.println("Please enter the departure time <HH:mm>:");
        String timeDepartureString = input.nextLine();
        LocalTime timeDeparture;
        try {
            timeDeparture = LocalTime.parse(timeDepartureString);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid time format. Please use HH:mm.");
            return;
        }

        loadData(timeDeparture);
        List<List<String>> stopIds = getStopIDs(stopStart, stopEnd);

        List<String> startStopIds = stopIds.get(0);
        List<String> endStopIds = stopIds.get(1);

        if (startStopIds.isEmpty()) {
            System.out.println("No departure stop found for: " + stopStart);
            return;
        }

        if (endStopIds.isEmpty()) {
            System.out.println("No arrival stop found for: " + stopEnd);
            return;
        }

        // Build the route graph before adding waiting, walking, and transfer edges.
        Graph graph = new Graph();
        buildGraph(graph);
        addWaitingEdges(graph);
        Set<String> reachableStops = new HashSet<>();
        for (String currentStartStopId : startStopIds) {
            reachableStops.addAll(buildReachableStopIds(graph, currentStartStopId, timeDeparture));
        }
        addWalkingEdges(graph, stops.values(), reachableStops);
        addTransferEdges(graph, stops.values());

        Map<String, String> stopNames = new HashMap<>();
        for (Stop s : stops.values()) {
            stopNames.put(s.id, s.name);
        }

        System.out.println("\nSearching itinerary from " + stopStart + " to " + stopEnd + " at " + timeDeparture + ".\n");
        List<List<Node>> ways = new ArrayList<>();

        for (String currentStartStopId : startStopIds) {
            for (String currentEndStopId : endStopIds) {
                List<Node> way = ItineraryFinder.findItinerary(graph, currentStartStopId, currentEndStopId, timeDeparture, stopNames);

                if (way != null && way.size() >= 2) {
                    ways.add(way);
                }
            }
        }

        if (ways.isEmpty()) {
            System.out.println("No itinerary found.");
            return;
        }

        List<Node> bestWay = ways.stream()
                .min(Comparator.<List<Node>, LocalTime>comparing(way -> way.get(way.size() - 1).time)
                        .thenComparingInt(List::size))
                .orElse(null);

        if (bestWay == null || bestWay.size() < 2) {
            System.out.println("No itinerary found.");
            return;
        }

        printItinerary(graph, bestWay);
    }
}
