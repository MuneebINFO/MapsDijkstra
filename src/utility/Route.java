package utility;

public class Route {
    public String id;
    public String shortName;
    public String longName;
    public String type;

    @Override
    public String toString() {
        return shortName + ": " + longName + " (" + type + ")";
    }
}