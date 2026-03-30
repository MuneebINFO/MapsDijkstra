# MapsDijkstra

MapsDijkstra is a Java project that computes a public transport itinerary between two stops using a graph-based shortest-path approach. It was developed as part of a second-year bachelor's course and is built around GTFS datasets from several Belgian transport operators.

The application loads timetable data, builds a time-expanded graph, and searches for the best route based on the requested departure time. It currently works with data from `STIB`, `SNCB`, `DELIJN`, and `TEC`.

## Project overview

This project is organized into the following main folders:

### `src`

Contains all Java source files for the application:

- the main entry point
- graph and pathfinding logic
- GTFS CSV loaders
- model classes such as stops, trips, routes, and stop times

### `build`

Contains the compiled `.class` files generated from the source code.

This directory is created when you compile the project and is used to run the program with `java -cp build ...`.

### `ressources` and `data`

Download the zip file located in the folder `ressources`, then extract the file and put it in the folder `data`.

`data` must contain the GTFS data used by the program with the following structure :

```
.
└── GTFS
    ├── DELIJN
    │   ├── routes.csv
    │   ├── stop_times.csv
    │   ├── stops.csv
    │   └── trips.csv
    ├── SNCB
    │   ├── routes.csv
    │   ├── stop_times.csv
    │   ├── stops.csv
    │   └── trips.csv
    ├── STIB
    │   ├── routes.csv
    │   ├── stop_times.csv
    │   ├── stops.csv
    │   └── trips.csv
    └── TEC
        ├── routes.csv
        ├── stop_times.csv
        ├── stops.csv
        └── trips.csv
```


## How it works

At a high level, the application:

1. loads GTFS data for the available operators
2. filters stop times around the requested departure time
3. builds a time-expanded graph
4. adds travel, waiting, walking, and transfer edges
5. runs Dijkstra's algorithm to find the best itinerary

The output is printed step by step, including transport segments, waiting periods, and transfers when needed.

## Compilation

Compile the project from the root directory.

### PowerShell

```powershell
New-Item -ItemType Directory -Force -Path build | Out-Null
javac -d build (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### Unix-like shell

```bash
mkdir -p build
javac -d build $(find src -name "*.java")
```

This compiles all `.java` files from `src/` into the `build/` folder.

## Running the application

Run the main program with:

```bash
java -Xmx2G -cp build Main
```

The program will ask for:

- the departure stop
- the arrival stop
- the departure time in `HH:mm` format

Example:

```text
Please enter the departure stop:
GARE DE L'OUEST
Please enter the arrival stop:
LOUISE
Please enter the departure time <HH:mm>:
10:00
```

## Running the test program

A test launcher is provided in `Test.java`:

```bash
java -cp build Test
```

It runs several predefined route searches by calling `Main.main(...)` with sample inputs.

## Notes

- The program may require a significant amount of memory because GTFS datasets are large. This is why the example command uses `-Xmx2G`.
- Stop names must be entered exactly as they appear in the GTFS data.
- The project relies on the `data/GTFS` folder being present and correctly structured.
- The folder name is `ressources/` in this repository, not `resources/`.
