package utility;

public class Trip {
    public String id;
    public String routeId;

    @Override
    public String toString() {
        return "Trip ID: " + id + ", Route: " + routeId;
    }
}
