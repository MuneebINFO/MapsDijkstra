package utility;

import java.time.LocalTime;

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
