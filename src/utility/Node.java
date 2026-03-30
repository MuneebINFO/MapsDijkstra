package utility;

import java.time.LocalTime;
import java.util.Objects;

// Represents a stop event in the time-expanded graph.
public class Node {
    public String stopId;
    public LocalTime time;
    public String tripId;

    // Creates a node without trip information when only stop and time matter.
    public Node(String stopId, LocalTime time) {
        this(stopId, time, null);
    }

    // Creates a node tied to a specific trip occurrence.
    public Node(String stopId, LocalTime time, String tripId) {
        this.stopId = stopId;
        this.time = time;
        this.tripId = tripId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(stopId, node.stopId)
                && Objects.equals(time, node.time)
                && Objects.equals(tripId, node.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopId, time, tripId);
    }

    @Override
    public String toString() {
        return stopId + " @ " + time + (tripId != null ? " [" + tripId + "]" : "");
    }
}
