# Projet INFO-F203 

## Composition du projet et nécessité

Le dossier du projet est composé de différents sous-dossiers :

### Dossier `src`
Contient tous les fichiers source du projet.

### Dossier `build`
Contient les fichiers `.class` compilés à partir des sources.  
Ce dossier est généré par la commande `javac` et sert de point de départ à l’exécution du programme (`java -cp build ...`).

### Dossier `data`
Doit contenir les données nécessaires au fonctionnement du programme, notamment :
- les fichiers GTFS (`stops.csv`, `trips.csv`, `routes.csv`, `stop_times.csv`)  
- par agence de transport (`STIB`, `SNCB`, `TEC`, `DELIJN`)  

---

## Compilation et exécution

### Compiler

À partir du dossier racine du projet, tapez :
```
javac -d build $(find src -name "*.java")
```

Cette commande compile tous les fichiers `.java` du dossier `src` et place les fichiers `.class` générés dans le dossier `build`.

---

### Exécuter

Pour lancer le programme avec une requête d’itinéraire, tapez :
```
java -Xmx2G -cp build Main "ArretDepart" "ArretArrivee" HH:mm
```

Exemple :
```
java -Xmx2G -cp build Main "GARE DE L'OUEST" "DELTA" 08:00
```

---

### Lancer les tests

Un script de test automatisé (`Test.java`) est fourni pour tester plusieurs scénarios :
```
java -cp build Test
```

Il exécute des requêtes prédéfinies en appelant directement `Main.main(...)` avec différents arguments.

---

## Remarques

- Le programme peut nécessiter une mémoire importante (d’où l’option `-Xmx2G`) à cause du volume des données GTFS.
- Le dossier `data` est volontairement fourni vide. Il est donc important d’y copier l’ensemble du contenu du dossier GTFS **tel quel**, sans le modifier. Le dossier `data` doit donc contenir le dossier STIB, le dossier DELIJN, le dossier TEC et le dossier SNCB.
