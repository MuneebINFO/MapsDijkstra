package utility;

// Stores the identity and coordinates of a stop.
public class Stop {
    public String id;
    public String name;
    public double lat;
    public double lon;

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
