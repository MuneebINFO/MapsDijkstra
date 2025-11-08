package utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalTime;
import java.time.Duration;

public class CsvLoader {

    // Charge les fichiers stops.csv
    public static Map<String, Stop> loadStops(String path) throws IOException {
        Map<String, Stop> stops = new HashMap<>();
		boolean isSNCB = path.contains("SNCB");

        // Utilisation d’un BufferedReader avec un buffer large pour accélérer la lecture
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            String line = br.readLine(); // ignore la ligne d’en-tête

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 4) continue;

                String id = tokens[0];

                // Cas particulier pour SNCB
				if (isSNCB) {
					if (id.contains("SNCB-S") || id.contains("_")) continue;
				}

                // Reconstruction du nom complet (qui peut peut contenir des virgules internes)
                StringBuilder nameBuilder = new StringBuilder(tokens[1]);
                int j = 2;
                while (j < tokens.length - 2 && !isDouble(tokens[j])) {
                    nameBuilder.append(",").append(tokens[j]);
                    j++;
                }

                if (j + 1 >= tokens.length) continue;

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

    // Charge les fichiers routes.csv
    public static Map<String, Route> loadRoutes(String path) throws IOException {
        Map<String, Route> routes = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 4) continue;

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

    // Charge les fichiers trips.csv
    public static Map<String, Trip> loadTrips(String path) throws IOException {
        Map<String, Trip> trips = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 2) continue;

                Trip trip = new Trip();
                trip.id = tokens[0];
                trip.routeId = tokens[1];
                trips.put(trip.id, trip);
            }
        }
        return trips;
    }

    // Charge les fichiers stop_times.csv
	public static Map<String, List<StopTime>> loadStopTimes(String path, LocalTime referenceTime) throws IOException {
        Map<String, List<StopTime>> stopTimes = new HashMap<>();
        Set<String> relevantTrips = new HashSet<>(); // pour ne pas trier des trips inutiles

        try (BufferedReader br = new BufferedReader(new FileReader(path), 32768)) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",", -1);
                if (tokens.length < 4) continue;

                try {
                    String tripId = tokens[0].trim();
                    String timeStr = tokens[1].trim();
                    String stopId = tokens[2].trim();
                    int seq = Integer.parseInt(tokens[3].trim());

                    // Parsing de l'heure
                    String[] parts = timeStr.split(":");
                    if (parts.length != 3) continue;
                    int hour = Integer.parseInt(parts[0]) % 24;
                    int minute = Integer.parseInt(parts[1]);
                    int second = Integer.parseInt(parts[2]);
                    LocalTime departureTime = LocalTime.of(hour, minute, second);

                    // Garder uniquement si l’heure est dans la bonne fenêtre
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
                    System.err.println("Ligne ignorée (erreur parsing) : " + line);
                }
            }
        }

        // Supprimer les trips sans aucun arrêt utile
        stopTimes.keySet().retainAll(relevantTrips);

        // Tri par ordre de séquence
        for (List<StopTime> list : stopTimes.values()) {
            list.sort(Comparator.comparingInt(st -> st.stopSequence));
        }
        return stopTimes;
    }

    // Vérifie si une chaîne peut être convertie en nombre à virgule flottante
    private static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
