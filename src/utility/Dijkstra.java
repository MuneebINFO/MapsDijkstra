package utility;

import java.util.*;

public class Dijkstra {

    // Stocke la distance minimale connue depuis la source vers chaque nœud
    public static Map<Node, Integer> distances;

    // Stocke le noeud précédent dans le plus court chemin pour chaque nœud
    public static Map<Node, Node> previous;

    // Calcule les plus courts chemins depuis le nœud source vers tous les autres nœuds du graphe
    public static void findPath(Graph graph, Node source) {
        distances = new HashMap<>();
        previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        distances.put(source, 0);
        queue.add(source);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            for (Edge edge : graph.getEdges(current)) {

                Node neighbor = edge.to;
                int newDist = distances.get(current) + edge.duration;

                // Si un chemin plus court vers le voisin est trouvé
                if (!distances.containsKey(neighbor) || newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
    }

    // Construit le plus court chemin depuis la source vers un nœud donné
    public static List<Node> getShortestPathTo(Node destination) {
        List<Node> path = new ArrayList<>();
        for (Node at = destination; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public static int getDurationTo(Node destination) {
        return distances.getOrDefault(destination, Integer.MAX_VALUE);
    }
}