package utility;

import java.time.LocalTime;
import java.util.*;

public class ItineraryFinder {

    // Trouve le plus court itinéraire entre deux arrêts
    public static List<Node> findItinerary(Graph graph, String stopFrom, String stopTo, LocalTime time, Map<String, String> stopNames) {
        // Construire la liste des candidats au départ
        List<Node> candidateStarts = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            if (node.stopId.equals(stopFrom) && !node.time.isBefore(time)) {
                candidateStarts.add(node);
            }
        }

        // Trier les candidats
        candidateStarts.sort(Comparator.comparing(n -> n.time));

        // Tester les départs dans les 20 minutes maximum
        LocalTime seuilMax = time.plusMinutes(20);

        for (Node startNode : candidateStarts) {
            if (startNode.time.isAfter(seuilMax)) break;

            // lancer Dijkstra
            Dijkstra.findPath(graph, startNode);

            // trouver le Node d’arrivée atteignable le plus tôt
            Node endNode = null;
            int minDuration = Integer.MAX_VALUE;
            String targetName = stopNames.getOrDefault(stopTo, "?");

            for (Node node : graph.getAllNodes()) {
                if (!Dijkstra.distances.containsKey(node)) continue;

                String nodeName = stopNames.getOrDefault(node.stopId, "?");

                // Chercher tous les nœuds associés au bon arrêt d’arrivée
                if (nodeName.equals(targetName)) {
                    int duration = Dijkstra.getDurationTo(node);

                    if (duration < minDuration) {
                        minDuration = duration;
                        endNode = node;  // garde le plus rapide
                    }
                }
            }

            // Si un chemin valide a été trouvé
            if (endNode != null) {
                return Dijkstra.getShortestPathTo(endNode);
            }
        }

        // Aucun chemin trouvé
        System.out.println("Aucun itinéraire trouvé de " + stopFrom + " vers " + stopTo + ".");
        return List.of();
    }

}
