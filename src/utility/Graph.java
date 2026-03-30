package utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Represents the directed graph used for pathfinding.
public class Graph {

    // Associates each node with the set of its outgoing edges.
    private Map<Node, Set<Edge>> adj;

    public Graph() {
        this.adj = new HashMap<>();
    }

    // Ensures that a node exists in the adjacency map.
    public void addNode(Node node) {
        adj.putIfAbsent(node, new HashSet<>());
    }

    // Adds a fully described directed edge to the graph.
    public void addEdge(Node from, Node to, int duration, String mode, String company, String line) {
        addNode(from);
        addNode(to);
        adj.get(from).add(new Edge(from, to, duration, mode, company, line));
    }

    // Adds a directed edge when operator metadata is not required.
    public void addEdge(Node from, Node to, int duration, String mode) {
        addEdge(from, to, duration, mode, null, null);
    }

    // Returns the outgoing edges of a node as a list.
    public ArrayList<Edge> getEdges(Node node) {
        return new ArrayList<>(adj.getOrDefault(node, Set.of()));
    }

    // Returns every node currently present in the graph.
    public Set<Node> getAllNodes() {
        return adj.keySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node node : adj.keySet()) {
            for (Edge edge : adj.get(node)) {
                sb.append(edge).append("\n");
            }
        }
        return sb.toString();
    }
}
