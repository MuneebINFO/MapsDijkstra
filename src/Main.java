import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import utility.CsvLoader;
import utility.Stop;
import utility.Route;
import utility.Trip;
import utility.StopTime;
import utility.Graph;
import utility.Edge;
import utility.Node;
import utility.Dijkstra;
import utility.GeoUtils;
import utility.ItineraryFinder;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Collection;
import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;

public class Main {

    static Map<String, Stop> stops = new HashMap<>();
    static Map<String, Route> routes = new HashMap<>();
    static Map<String, Trip> trips = new HashMap<>();
	static Map<String, List<StopTime>> stopTimes = new HashMap<>();

	// Charge tous les arrêts des agences et renvoie les IDs de départ et d'arrivée
    public static String[] loadStopData(String nomDepart, String nomArrivee) {
        try {
            System.out.println("Chargement des arrêts...");
            String[] agencies = {"STIB", "SNCB", "DELIJN", "TEC"};
            for (String agency : agencies) {
                stops.putAll(CsvLoader.loadStops("data/" + agency + "/stops.csv"));
            }
            System.out.println("Chargement des arrêts terminés.\n");

        } catch (IOException e) {
                System.err.println("Erreur lors du chargement des stops: " + e.getMessage());
                e.printStackTrace();
        }

		// Recherche des IDs d'arrêt correspondant aux noms donnés
        String stopStartID = null;
        String stopEndID = null;

        for (Stop stop : stops.values()) {
            if (stop.name.equals(nomDepart) && stopStartID == null) {
                stopStartID = stop.id;
            }
            if (stop.name.equals(nomArrivee) && stopEndID == null) {
                stopEndID = stop.id;
            }
        }

        return new String[] { stopStartID, stopEndID };
    }

	// Charge les données complémentaires pour chaque agence spécifiée
    public static void loadOtherData(LocalTime heureDepart, List<String> agencies) {
        try {
			System.out.println("\nChargement des données:");
            for (String agency : agencies) {
                System.out.println("   Chargement " + agency + "...");
                routes.putAll(CsvLoader.loadRoutes("data/" + agency + "/routes.csv"));
                trips.putAll(CsvLoader.loadTrips("data/" + agency + "/trips.csv"));
				stopTimes.putAll(CsvLoader.loadStopTimes("data/" + agency + "/stop_times.csv", heureDepart));
                System.out.println("   Chargement " + agency + " terminé.");
            }
			System.out.println("Chargement terminé.\n");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des fichiers : " + e.getMessage());
            e.printStackTrace();
        }
    }

	// Ajoute des arêtes WALK dans le graphe entre des arrêts proches
    public static void addWalkingEdges(Graph graph, Collection<Stop> stops, Set<String> relevantStopIds) {
        final double MAX_WALKING_DISTANCE = 3000.0; // Distance maximale à pied en mètres
        final double WALKING_SPEED = 1.4; // Vitesse de marche en m/s (~5 km/h)

        Map<String, List<Node>> stopIdToNodes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            stopIdToNodes.computeIfAbsent(node.stopId, k -> new ArrayList<>()).add(node);
        }

        List<Stop> stopList = new ArrayList<>(stops);

		// Parcours chaque paire unique d'arrêts
        for (int i = 0; i < stopList.size(); i++) {
            Stop stopA = stopList.get(i);
            if (!relevantStopIds.contains(stopA.id)) continue;  // Ignore les arrêts non pertinents

            for (int j = i + 1; j < stopList.size(); j++) {
                Stop stopB = stopList.get(j);
                if (!relevantStopIds.contains(stopB.id)) continue;

                double dist = GeoUtils.distance(stopA.lat, stopA.lon, stopB.lat, stopB.lon);
                if (dist > MAX_WALKING_DISTANCE) continue;   // Ignore les distances trop longues

                int estimatedTime = (int)(dist / WALKING_SPEED);

                List<Node> nodesA = stopIdToNodes.getOrDefault(stopA.id, List.of());
                List<Node> nodesB = stopIdToNodes.getOrDefault(stopB.id, List.of());

				for (Node a : nodesA) {
                    for (Node b : nodesB) {
                        if (a.time.isBefore(b.time)) {
                            int delta = (int) Duration.between(a.time, b.time).getSeconds();

							// Connecter les deux nœuds que si la durée réelle est proche de la durée estimée
                            if (Math.abs(delta - estimatedTime) <= 90) {
                                graph.addEdge(a, b, delta, "WALK", null, null);
                            }
                        }
                    }
                }   
           }
        }
    }


    public static void addTransferEdges(Graph graph, Collection<Stop> stops) {
        final double MAX_DISTANCE_METERS = 100.0;
        final int TRANSFER_DURATION = 60;
        final int TIME_TOLERANCE = 60;    // tolérance d'erreur en secondes pour la durée du tranfert
        final double GRID_SIZE_METERS = 100.0;  // taille de la grille spatiale

        // Ranger les arrêts dans une grille selon leur position
        Map<String, List<Stop>> grid = new HashMap<>();
        for (Stop stop : stops) {
            String key = spatialKey(stop.lat, stop.lon, GRID_SIZE_METERS);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(stop);
        }

        // Associer chaque stopId à la liste des Node qui lui correspondent
        Map<String, List<Node>> stopIdToNodes = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            stopIdToNodes.computeIfAbsent(node.stopId, k -> new ArrayList<>()).add(node);
        }

        // Pour chaque arrêt, regarder les arrêts proches
        for (Stop stop : stops) {
            String baseKey = spatialKey(stop.lat, stop.lon, GRID_SIZE_METERS);
            int[] dx = {-1, 0, 1};
            int[] dy = {-1, 0, 1};

            for (int ix : dx) {
                for (int iy : dy) {
                    String neighborKey = shiftKey(baseKey, ix, iy);
                    List<Stop> candidates = grid.getOrDefault(neighborKey, List.of());

                    for (Stop other : candidates) {
                        if (stop.id.equals(other.id)) continue; // pas de transfert vers soi-même

                        double dist = GeoUtils.distance(stop.lat, stop.lon, other.lat, other.lon);
                        if (dist > MAX_DISTANCE_METERS) continue;  // trop loin

						// Les Node (horaire) pour les deux arrêt
                        List<Node> nodesA = new ArrayList<>(stopIdToNodes.getOrDefault(stop.id, List.of()));
                        List<Node> nodesB = new ArrayList<>(stopIdToNodes.getOrDefault(other.id, List.of()));

                        // Trier les deux listes par heure
                        nodesA.sort(Comparator.comparing(n -> n.time));
                        nodesB.sort(Comparator.comparing(n -> n.time));

                        int i = 0, j = 0;
                        while (i < nodesA.size() && j < nodesB.size()) {
                            Node n1 = nodesA.get(i);
                            Node n2 = nodesB.get(j);

                            int delta = (int) Duration.between(n1.time, n2.time).getSeconds();

                            if (Math.abs(delta - TRANSFER_DURATION) <= TIME_TOLERANCE) {
                                graph.addEdge(n1, n2, TRANSFER_DURATION, "TRANSFER", null, null);
                                graph.addEdge(n2, n1, TRANSFER_DURATION, "TRANSFER", null, null);
                                i++;
                                j++;
                            } else if (n1.time.isBefore(n2.time)) {
                                i++;
                            } else {
                                j++;
                            }
                        }
                    }
                }
            }
        }
    }

    // Convertit les coordonnées en clé de cellule
    private static String spatialKey(double lat, double lon, double gridSizeMeters) {
        int x = (int) (lat * 100000 / gridSizeMeters);
        int y = (int) (lon * 100000 / gridSizeMeters);
        return x + "_" + y;
    }

    // Pour passer à une cellule voisine
    private static String shiftKey(String baseKey, int dx, int dy) {
        String[] parts = baseKey.split("_");
        int x = Integer.parseInt(parts[0]) + dx;
        int y = Integer.parseInt(parts[1]) + dy;
        return x + "_" + y;
    }


    public static Set<String> buildReachableStopIds(Graph graph, String stopFrom, LocalTime startTime) {
        Set<String> reachable = new HashSet<>();

        // Trouver tous les Node (stopId == stopFrom) avec heure >= startTime
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparing(n -> n.time));
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.getAllNodes()) {
            if (node.stopId.equals(stopFrom) && !node.time.isBefore(startTime)) {
                queue.add(node);
                visited.add(node);
                reachable.add(node.stopId);
            }
        }

        // BFS/DFS simplifié sur les arêtes existantes
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

	// Construit le graphe à partir des horaires
	public static void buildGraph(Graph graph) {
        for (List<StopTime> stopTime : stopTimes.values()) {
            if (stopTime.size() < 2) continue;

            // Tri des arrêts par leur ordre dans le trajet
            stopTime.sort(Comparator.comparingInt(st -> st.stopSequence));

			// Création d’une arête entre chaque paire d’arrêts consécutifs
            for (int i = 0; i < stopTime.size() - 1; i++) {
                StopTime from = stopTime.get(i);
                StopTime to = stopTime.get(i + 1);

                Stop s1 = stops.get(from.stopId);
                Stop s2 = stops.get(to.stopId);
                if (s1 == null || s2 == null) {
                    continue;
                }

                int duration = (int) Duration.between(from.departureTime, to.departureTime).getSeconds();
                if (duration < 0) continue;

                Trip trip = trips.get(from.tripId);
                if (trip == null) continue;

                Route route = routes.get(trip.routeId);
                String mode = route.type;
                String line = route.shortName;

                String company = "";
                if (trip.routeId != null && trip.routeId.contains("-")) {
                    company = trip.routeId.split("-")[0];
                }

                Node fromNode = new Node(from.stopId, from.departureTime);
                Node toNode = new Node(to.stopId, to.departureTime);

                graph.addEdge(fromNode, toNode, duration, mode, company, line);
            }
        }
    }


    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Format : <sourceStop> <targetStop> <HH:mm>");
            System.exit(1);
        }
		
        String stopStart = args[0];
        String stopEnd = args[1];
        LocalTime heureDepart = LocalTime.parse(args[2]);

        String[] stopIds = loadStopData(stopStart, stopEnd);
        String stopStartID = stopIds[0];
        String stopEndID = stopIds[1];
		System.out.println("ID trouvé pour " + stopStart + " : " + stopStartID);
		System.out.println("ID trouvé pour " + stopEnd + " : " + stopEndID);

		// Détermine quelles sont les agences des arrêts
		String agenceDepart = stopStartID.split("-")[0];
        String agenceArrivee = stopEndID.split("-")[0];

		List<String> agenciesToLoad;
		// Si les arrêts sont de la même agence
        if (agenceDepart.equals(agenceArrivee)) {
            agenciesToLoad = List.of(agenceDepart);
        } else {
            agenciesToLoad = List.of("STIB", "SNCB", "DELIJN", "TEC");
        }

		loadOtherData(heureDepart, agenciesToLoad);

		System.out.println("Nombre de trips chargés : " + stopTimes.size());
        int totalStopTimes = stopTimes.values().stream().mapToInt(List::size).sum();
        System.out.println("Nombre total de StopTime : " + totalStopTimes);

		System.out.println("\nCréation du graphe...");
		Graph graph = new Graph();
		buildGraph(graph);
		System.out.println("Création du graphe terminé.\n");

        System.out.println("Recherche des arrêts atteignables depuis " + stopStart);
        Set<String> reachableStops = buildReachableStopIds(graph, stopStartID, heureDepart);
        System.out.println("Nombre d’arrêts pertinents pour WALK : " + reachableStops.size());

        System.out.println("\nAjout des arêtes de marche...");
        addWalkingEdges(graph, stops.values(), reachableStops);
        System.out.println("Ajout terminé.\n");

        System.out.println("Ajout des arêtes TRANSFER...");
        addTransferEdges(graph, stops.values());
        System.out.println("Ajout TRANSFER terminé.\n");

        Map<String, String> stopNames = new HashMap<>();
        for (Stop s : stops.values()) {
            stopNames.put(s.id, s.name);
        }

		System.out.println("\nRecherche d'itinéraire de " + stopStart + " à " + stopEnd + " à " + heureDepart + ".\n");
        List<Node> chemin = ItineraryFinder.findItinerary(graph, stopStartID, stopEndID, heureDepart, stopNames);

        if (chemin == null || chemin.size() < 2) {
            System.out.println("Aucun itinéraire trouvé.");
            return;
        }

        for (int i = 0; i < chemin.size() - 1; i++) {
            Node from = chemin.get(i);
            Node to = chemin.get(i + 1);

            Stop stopFrom = stops.get(from.stopId);
            Stop stopTo = stops.get(to.stopId);
            String fromName = stopFrom != null ? stopFrom.name : "?";
            String toName = stopTo != null ? stopTo.name : "?";

            // Ne pas afficher une transition d’un arrêt vers lui-même
            if (fromName.equals(toName)) {
                continue;
            }

            Edge edge = graph.getEdges(from).stream()
                .filter(e -> e.to.equals(to))
                .findFirst().orElse(null);
            if (edge == null) continue;

            String mode = edge.mode;
            String operator = edge.company != null ? edge.company : "?";
            String lineNumber = edge.line != null ? edge.line : "?";

            if (mode == "WALK" || mode == "TRANSFER") {
                System.out.printf("Walk from %s (%s) to %s (%s) %n",
                    fromName, from.time,
                    toName, to.time);
            } else {
                System.out.printf("Take %S %s %s from %s (%s) to %s (%s) %n",
                    operator, mode, lineNumber,
                    fromName, from.time,
                    toName, to.time);
            }
        }
	}
}