# Cooked - Küchenassistent

## 1. Gewählte Projektidee
Ich habe mich **ausdrücklich für die erste Projektidee ("Cooked - Küchenassistent") entschieden**. Die zweite Idee (SmartVending) wurde verworfen. 

Das Projekt ist ein digitaler Küchenassistent für zu Hause. Das Programm speichert, welche Lebensmittel man aktuell hat und wie lange sie noch haltbar sind. Basierend darauf prüft das System, ob man bestimmte Rezepte mit den vorhandenen Zutaten kochen kann.

---

## 2. Klassen- und Modulübersicht

| Klassenname | Aufgabe / Verantwortung |
| :--- | :--- |
| `Main` | Der Einstiegspunkt der Anwendung. Hier werden die Dummy-Daten generiert und alle Funktionen getestet. |
| `Ingredient` | Datenmodell für eine Zutat (enthält id, name, quantity, unit, expirationDate). |
| `Recipe` | Datenmodell für ein Rezept (enthält id, name, prepTime). |
| `Inventory` | Service-Klasse, die Geschäftslogik ausführt (z. B. Überprüfung abgelaufener Zutaten). |
| `DatabaseConnection`| Verwaltet ausschließlich den Verbindungsaufbau zur SQLite-Datenbank (Separation of Concerns). |
| `IngredientHandler` | Führt alle CRUD-Operationen für die `ingredients`-Tabelle aus. |
| `RecipeHandler` | Führt CRUD-Operationen für Rezepte und die Verknüpfungstabelle aus. Enthält zudem den `canBeMade`-Algorithmus. |

---

## 3. Datenbankarchitektur und Tabellen
Die Datenbank verwendet SQLite und besteht aus drei Tabellen, um eine n:m-Beziehung zwischen Rezepten und Zutaten sauber zu modellieren.

### A. Tabelle `ingredients` (Der Vorrat / Kühlschrank)
Speichert die tatsächlichen Lebensmittel, die der Nutzer zu Hause hat.
* **id**: INTEGER (Primary Key)
* **name**: TEXT
* **quantity**: REAL
* **unit**: TEXT
* **expirationDate**: TEXT (Wird in Java in ein `LocalDate` umgewandelt)

### B. Tabelle `recipes` (Das Rezeptbuch)
Speichert die grundlegenden Metadaten der Rezepte.
* **id**: INTEGER (Primary Key)
* **name**: TEXT
* **prepTime**: INTEGER

### C. Tabelle `recipe_ingredients` (Junction Table / Verknüpfungstabelle)
Diese Tabelle fungiert als Brücke und modelliert die **n:m Beziehung** zwischen Rezepten und den benötigten Zutaten. 
* **recipe_id**: INTEGER (Foreign Key - referenziert `recipes(id)`)
* **ingredient_name**: TEXT (Der Name der benötigten Zutat, z.B. "Milk")
* **required_quantity**: REAL
* **unit**: TEXT

---

## 4. Herkunft und Verarbeitung der Rezepte
**Woher kommen die Rezepte?**
Aktuell werden die Rezepte und ihre benötigten Zutaten direkt im Code (via `Main.java` als Dummy-Daten) in die SQLite-Datenbank initialisiert ("Seeding"). 

Das System ist jedoch durch die `RecipeHandler`-Klasse so strukturiert, dass Nutzer in Zukunft über eine Eingabe (z.B. Konsole oder GUI) eigene Rezepte einspeisen können. Die Verknüpfungstabelle (`recipe_ingredients`) erlaubt es, dynamisch beliebig viele Zutaten zu einem Rezept (über die `recipe_id`) hinzuzufügen. Der Algorithmus `canBeMade()` vergleicht anschließend die Anforderungen aus dieser Tabelle mit den physischen Beständen in der `ingredients`-Tabelle.
## Blatt 3: Netzwerkprogrammierung und Nebenläufigkeit

### 1. Socket-Wahl (TCP vs UDP)
Für dieses Projekt haben wir uns für `java.net.Socket` (TCP) anstelle von `DatagramSocket` (UDP) entschieden. Der Grund dafür ist, dass TCP eine verbindungsorientierte und zuverlässige Kommunikation garantiert. Bei einem Rezept-Management-System ist es kritisch, dass Daten (wie Zutaten oder neue Rezepte) vollständig und fehlerfrei in der Datenbank ankommen. Paketverluste oder eine falsche Reihenfolge (was bei UDP passieren könnte) würden die Datenbankintegrität zerstören.

### 2. DTO-Kommunikation
Die Kommunikation zwischen Client und Server erfolgt nicht mehr über reine Strings, sondern über serialisierbare `Request`- und `Response`-Objekte. Komplexe Datenstrukturen wie Zutaten werden in `IngredientDTO`-Klassen gekapselt. Dies sorgt für eine saubere Trennung von Geschäftslogik und Netzwerk-Payload.

### 3. Nebenläufigkeit und Synchronisation (Race Condition)
Der Server nutzt eine `ClientHandler`-Klasse, die das `Runnable`-Interface implementiert. Jeder verbundene Client wird in einem eigenen Thread bedient, sodass der Server nicht blockiert.

**Gefahr einer Race Condition:** Wenn zwei Clients gleichzeitig versuchen, die Menge einer Zutat zu verändern oder ein neues Rezept hinzuzufügen, könnten sie sich gegenseitig überschreiben oder inkonsistente Datenbankzustände verursachen.

**Lösung:** Um dies zu verhindern, haben wir die gemeinsam genutzten Ressourcen (`ingredientHandler` und `recipeHandler`) in der `ClientHandler`-Klasse mit `synchronized` Blöcken geschützt.
Beispiel beim Hinzufügen einer Zutat:
```java
synchronized (ingredientHandler) {
    ingredientHandler.addIngredient(newIng);
}
```
## Blatt 4: GUI, MVC und Projektvorführung

### 1. Model-View-Controller (MVC) Struktur
Das Projekt wurde nach dem MVC-Prinzip umstrukturiert, um die Logik sauber von der Benutzeroberfläche zu trennen:
* **Model:** Beinhaltet alle Datenklassen (`Recipe`, `Ingredient`), DTOs sowie die Server- und Datenbanklogik.
* **View:** Die Klasse `MainGUI` enthält ausschließlich das Layout (JavaFX) und die UI-Komponenten (Buttons, Textfelder, Listen). Sie enthält keine Datenbankzugriffe.
* **Controller:** Die Klasse `MainController` fungiert als Vermittler. Sie fängt die Button-Klicks aus der GUI ab, erstellt die entsprechenden DTOs/Requests und kommuniziert mit dem Server.

### 2. Hintergrund-Ausführung (Tasks) & GUI-Updates
Damit die grafische Oberfläche (GUI) während der Serveranfragen (z. B. beim Abrufen von Rezepten oder Kochen) nicht einfriert, wurden längere Netzwerkoperationen in `Task`-Objekte ausgelagert. Diese Tasks laufen in separaten Hintergrund-Threads.
Sobald der Server eine Antwort liefert, wird die GUI (z. B. eine Erfolgsmeldung in der `TextArea` oder das Aktualisieren der `ListView`) sicher über die `setOnSucceeded`-Methode des Tasks oder mittels `Platform.runLater()` aktualisiert.

### 3. Neue Klassen für die GUI
* `MainGUI`: Baut die JavaFX-Oberfläche mit BorderPane, VBox und StackPane auf.
* `MainController`: Verbindet die GUI-Elemente mit der Server-Kommunikation.
* `Launcher`: Eine einfache Hilfsklasse zum Starten der JavaFX-Anwendung, um Modul-Einschränkungen zu umgehen.
### Quellen / Hilfsmittel
Für die Umsetzung dieses Projekts wurden folgende Ressourcen als theoretische und praktische Hilfestellung herangezogen:

- **Vorlesungsmaterialien & Beispielcode:** Zur Orientierung und Implementierung der Netzwerkarchitektur (Client/Server) sowie zum Verständnis von Nebenläufigkeit (Threads und das `Runnable`-Interface), exakt wie in der Vorlesung behandelt.
- **SQLite JDBC Driver Docs:** Für die korrekte Anbindung der Datenbank und die Nutzung von `RETURN_GENERATED_KEYS`.
- **Online-Tutorials & Video-Plattformen (z. B. YouTube):** Verschiedene allgemeine Tutorials zu den Themen "Java Client-Server Architektur", "DTO (Data Transfer Object) Patterns" und "Synchronisation / Race Conditions in Java".
- **KI-Assistenz (Google Gemini):** Ausschließlich genutzt als Übersetzungs- und Formulierungshilfe (z. B. zum besseren Verständnis komplexer deutscher Aufgabenstellungen) sowie zur Strukturierung dieses ReadMe-Dokuments. Die Logik, Architektur und der eigentliche Java-Code wurden eigenständig erarbeitet.
