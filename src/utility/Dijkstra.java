package utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// Runs Dijkstra's algorithm on the time-expanded transit graph.
public class Dijkstra {

    // Stores the currently known shortest distance from the source to each node.
    public static Map<Node, Integer> distances;

    // Stores the previous node for each node in the shortest-path tree.
    private static Map<Node, Node> previous;

    // Computes the shortest paths from the source node to every reachable node.
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

                // Update the queue when a shorter path to a neighbor is found.
                if (!distances.containsKey(neighbor) || newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
    }

    // Reconstructs the shortest path from the source to a destination node.
    public static List<Node> getShortestPathTo(Node destination) {
        List<Node> path = new ArrayList<>();
        for (Node at = destination; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    // Returns the computed duration to a destination, or Integer.MAX_VALUE when unreachable.
    public static int getDurationTo(Node destination) {
        return distances.getOrDefault(destination, Integer.MAX_VALUE);
    }
}
