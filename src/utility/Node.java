package utility;

import java.time.LocalTime;
import java.util.Objects;

// Représente un arrêt
public class Node {
    public String stopId;
    public LocalTime time;

    public Node(String stopId, LocalTime time) {
        this.stopId = stopId;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return stopId.equals(node.stopId) && time.equals(node.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopId, time);
    }

    @Override
    public String toString() {
        return stopId + " @ " + time;
    }
}
