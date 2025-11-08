package utility;

import java.util.*;

// Représente un graphe orienté
public class Graph {

    //chaque noeud est associé à l’ensemble de ses arêtes sortantes
    private Map<Node, Set<Edge>> adj;

    public Graph() {
        this.adj = new HashMap<>();
    }    

    public void addNode(Node node) {
        adj.putIfAbsent(node, new HashSet<>());
    }
    
    public void addEdge(Node from, Node to, int duration, String mode, String company, String line) {
        addNode(from);
        addNode(to);
        adj.get(from).add(new Edge(from, to, duration, mode, company, line));
    }

    public void addEdge(Node from, Node to, int duration, String mode) {
        addEdge(from, to, duration, mode, null, null);
    }

    public List<Edge> getEdges(Node node) {
        return new ArrayList<>(adj.getOrDefault(node, Set.of()));
    }    

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
