# Cooked – Kitchen Assistant

A Java desktop application that keeps track of what is in your kitchen – including expiration dates – and tells you which recipes you can cook with the ingredients you have. It is built as a client–server system with a JavaFX GUI and an SQLite database.

Developed as a university project (Fachprojekt, University of Duisburg-Essen, summer semester 2026).

## Features

- Manage pantry items (add, delete) with quantity, unit and expiration date
- List all expired ingredients
- Create recipes and attach the ingredients they require (quantity + unit)
- **Make recipe:** checks whether every required ingredient is in stock and not expired, then deducts the used amounts from the pantry
- Several clients can be connected to the server at the same time

## Tech stack

Java 25 · JavaFX 17 · SQLite (JDBC) · TCP sockets · multithreading · Git

## Architecture

```
JavaFX GUI (view)
      │
MainController ──► Client ──── TCP :8080 ────► Server
                                                 │  one ClientHandler thread per client
                                                 ├─► IngredientHandler ─┐
                                                 └─► RecipeHandler ─────┴─► SQLite (Mydatabase.db)
```

- **MVC:** `view` contains only the JavaFX layout, `controller` handles user actions and talks to the server, `model` holds data classes, DTOs, database access and networking.
- **Client–server communication:** serializable `Request` / `Response` objects carrying DTOs over object streams (TCP, port 8080).
- **Concurrency:** the server handles each client in its own thread; shared database handlers are protected with `synchronized` blocks to prevent race conditions.
- **Responsive GUI:** network calls run in JavaFX `Task`s on background threads; the UI is updated via `setOnSucceeded` / `Platform.runLater()`.
- **Database:** three tables – `ingredients`, `recipes` and the junction table `recipe_ingredients` (n:m between recipes and ingredients). All queries use `PreparedStatement`s.

## Getting started

**Requirements**

- JDK 25
- JavaFX 17 (`org.openjfx:javafx-controls:17.0.6`)
- SQLite JDBC driver (`org.xerial:sqlite-jdbc:3.50.1.0`)

The project is set up as an IntelliJ IDEA project (no Maven/Gradle build yet).

1. Clone the repository and open the folder in IntelliJ IDEA.
2. Add the two libraries: *File → Project Structure → Libraries → + → From Maven…* and enter the coordinates above.
3. *(Optional)* Run `model.Main` once – it creates the tables, inserts demo ingredients and recipes and prints a console test of all CRUD operations and the recipe check.
4. Run `model.Server` – the server starts on port 8080 and creates the database tables if needed.
5. Run `view.Launcher` – the GUI opens and connects to the server on `localhost`.

## Project structure

```
src/
├── controller/   MainController – connects GUI events to server requests
├── model/        data classes, DTOs, database handlers, Server, Client, ClientHandler
└── view/         MainGUI (JavaFX layout), Launcher
docs/
└── Projektdokumentation.md   detailed project documentation (German)
```

## Possible next steps

- Unit tests (JUnit 5) for `canBeMade()` and the CRUD operations
- Maven or Gradle build
- Link recipe requirements to ingredients by ID instead of by name

## Author

Meliksah Kocman – Computer Science student, University of Duisburg-Essen
