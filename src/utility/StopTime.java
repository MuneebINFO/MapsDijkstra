package utility;

import java.time.LocalTime;

// Stores the scheduled time of a trip at a given stop.
public class StopTime {
    public String tripId;
    public LocalTime departureTime;
    public String stopId;
    public int stopSequence;

    @Override
    public String toString() {
        return stopId + " at " + departureTime + " (sequence " + stopSequence + ")";
    }
}
