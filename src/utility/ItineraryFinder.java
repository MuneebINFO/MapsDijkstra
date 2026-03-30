package utility;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Selects the best itinerary between two stops within a limited departure window.
public class ItineraryFinder {

    // Finds the shortest itinerary between two stops for the requested departure time.
    public static List<Node> findItinerary(Graph graph, String stopFrom, String stopTo, LocalTime time, Map<String, String> stopNames) {
        // Build the list of candidate departure nodes.
        List<Node> candidateStarts = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            if (node.stopId.equals(stopFrom) && !node.time.isBefore(time)) {
                candidateStarts.add(node);
            }
        }

        // Sort candidates chronologically so the search checks the earliest departures first.
        candidateStarts.sort(Comparator.comparing(n -> n.time));

        // Only test departures within the next 20 minutes.
        LocalTime seuilMax = time.plusMinutes(20);

        for (Node startNode : candidateStarts) {
            if (startNode.time.isAfter(seuilMax)) {
                break;
            }

            // Run Dijkstra from the current departure candidate.
            Dijkstra.findPath(graph, startNode);

            // Find the earliest reachable arrival node for the requested destination name.
            Node endNode = null;
            int minDuration = Integer.MAX_VALUE;
            String targetName = stopNames.getOrDefault(stopTo, "?");

            for (Node node : graph.getAllNodes()) {
                if (!Dijkstra.distances.containsKey(node)) {
                    continue;
                }

                String nodeName = stopNames.getOrDefault(node.stopId, "?");

                // Check every node that belongs to the requested destination stop name.
                if (nodeName.equals(targetName)) {
                    int duration = Dijkstra.getDurationTo(node);

                    if (duration < minDuration) {
                        minDuration = duration;
                        endNode = node; // Keep the fastest arrival found so far.
                    }
                }
            }

            // Return immediately once a valid path has been found for this start node.
            if (endNode != null) {
                return Dijkstra.getShortestPathTo(endNode);
            }
        }

        return List.of();
    }
}
