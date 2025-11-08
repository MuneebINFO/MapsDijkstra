package utility;

import java.util.Objects;

// Représente une arête du graphe (une liaison entre deux nœuds) 
public class Edge {
    public Node from;
    public Node to;
    public int duration; // en secondes
    public String mode; // BUS, TRAIN, WALK, etc.
    public String company; // STIB, SNCB, ...
    public String line;

    public Edge(Node from, Node to, int duration, String mode, String company, String line) {
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.mode = mode;
        this.company = company;
        this.line = line;
    }

    public Edge(Node from, Node to, int duration, String mode) {
        this(from, to, duration, mode, null, null);
    }

    @Override
    public String toString() {
        return String.format("[%s -> %s | %s (%s) %s, %ds]", from, to, mode, company, line, duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return duration == edge.duration &&
           from.equals(edge.from) &&
           to.equals(edge.to) &&
           mode.equals(edge.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, duration, mode);
    }

}
