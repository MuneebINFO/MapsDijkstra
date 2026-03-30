package utility;

import java.util.Objects;

// Represents a directed connection between two nodes in the graph.
public class Edge {
    public Node from;
    public Node to;
    public int duration; // Duration in seconds.
    public String mode; // BUS, TRAIN, WALK, and so on.
    public String company; // Operator name, for example STIB or SNCB.
    public String line;

    public Edge(Node from, Node to, int duration, String mode, String company, String line) {
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.mode = mode;
        this.company = company;
        this.line = line;
    }

    // Convenience constructor for edges that do not need company or line information.
    public Edge(Node from, Node to, int duration, String mode) {
        this(from, to, duration, mode, null, null);
    }

    @Override
    public String toString() {
        return String.format("[%s -> %s | %s (%s) %s, %ds]", from, to, mode, company, line, duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge edge = (Edge) o;
        return duration == edge.duration
                && from.equals(edge.from)
                && to.equals(edge.to)
                && mode.equals(edge.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, duration, mode);
    }
}
