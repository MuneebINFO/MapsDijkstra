package utility;

// Stores the link between a trip instance and its route.
public class Trip {
    public String id;
    public String routeId;

    @Override
    public String toString() {
        return "Trip ID: " + id + ", Route: " + routeId;
    }
}
