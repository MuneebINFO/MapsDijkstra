package utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Loads GTFS CSV files into the in-memory model classes used by the application.
public class CsvLoader {

    // Loads data from a stops.csv file.
    public static Map<String, Stop> loadStops(String path) throws IOException {
        Map<String, Stop> stops = new HashMap<>();
        boolean isSNCB = path.contains("SNCB");

        // A large buffer helps speed up the parsing of large GTFS files.
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            String line = br.readLine(); // Skip the header row.

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 4) {
                    continue;
                }

                String id = tokens[0];

                // SNCB contains station variants that should not be imported as regular stops.
                if (isSNCB) {
                    if (id.contains("SNCB-S") || id.contains("_")) {
                        continue;
                    }
                }

                // Rebuild the full stop name when it contains commas inside the field.
                StringBuilder nameBuilder = new StringBuilder(tokens[1]);
                int j = 2;
                while (j < tokens.length - 2 && !isDouble(tokens[j])) {
                    nameBuilder.append(",").append(tokens[j]);
                    j++;
                }

                if (j + 1 >= tokens.length) {
                    continue;
                }

                String name = nameBuilder.toString();
                double lat = Double.parseDouble(tokens[j]);
                double lon = Double.parseDouble(tokens[j + 1]);

                Stop stop = new Stop();
                stop.id = id;
                stop.name = name;
                stop.lat = lat;
                stop.lon = lon;
                stops.put(id, stop);
            }
        }
        return stops;
    }

    // Loads data from a routes.csv file.
    public static Map<String, Route> loadRoutes(String path) throws IOException {
        Map<String, Route> routes = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 4) {
                    continue;
                }

                Route route = new Route();
                route.id = tokens[0];
                route.shortName = tokens[1];
                route.longName = tokens[2];
                route.type = tokens[3];
                routes.put(route.id, route);
            }
        }
        return routes;
    }

    // Loads data from a trips.csv file.
    public static Map<String, Trip> loadTrips(String path) throws IOException {
        Map<String, Trip> trips = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 2) {
                    continue;
                }

                Trip trip = new Trip();
                trip.id = tokens[0];
                trip.routeId = tokens[1];
                trips.put(trip.id, trip);
            }
        }
        return trips;
    }

    // Loads data from a stop_times.csv file and keeps only the useful time window.
    public static Map<String, List<StopTime>> loadStopTimes(String path, LocalTime referenceTime) throws IOException {
        Map<String, List<StopTime>> stopTimes = new HashMap<>();
        Set<String> relevantTrips = new HashSet<>(); // Avoid keeping trips that never match the search window.

        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 4) {
                    continue;
                }

                try {
                    String tripId = tokens[0].trim();
                    String timeStr = tokens[1].trim();
                    String stopId = tokens[2].trim();
                    int seq = Integer.parseInt(tokens[3].trim());

                    // Parse the HH:mm:ss time stored in the GTFS file.
                    String[] parts = timeStr.split(":");
                    if (parts.length != 3) {
                        continue;
                    }
                    int hour = Integer.parseInt(parts[0]) % 24;
                    int minute = Integer.parseInt(parts[1]);
                    int second = Integer.parseInt(parts[2]);
                    LocalTime departureTime = LocalTime.of(hour, minute, second);

                    // Keep only stop times that fall within the selected search window.
                    if (departureTime.isBefore(referenceTime) || departureTime.isAfter(referenceTime.plusHours(1))) {
                        continue;
                    }

                    StopTime st = new StopTime();
                    st.tripId = tripId;
                    st.departureTime = departureTime;
                    st.stopId = stopId;
                    st.stopSequence = seq;

                    stopTimes.computeIfAbsent(tripId, k -> new ArrayList<>()).add(st);
                    relevantTrips.add(tripId);

                } catch (Exception e) {
                    System.err.println("Skipped line due to parsing error: " + line);
                }
            }
        }

        // Remove trips that ended up with no useful stop times.
        stopTimes.keySet().retainAll(relevantTrips);

        // Sort each trip by stop sequence to preserve the travel order.
        for (List<StopTime> list : stopTimes.values()) {
            list.sort(Comparator.comparingInt(st -> st.stopSequence));
        }
        return stopTimes;
    }

    // Returns true when the provided string can be parsed as a double.
    private static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
