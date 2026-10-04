package model;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IngredientHandler {

    public void createTable() {
        // Here I am creating a table which will hold the ingredients
        // id will be increasing automatically and the rest will be same as ingredient class
        String sql = "CREATE TABLE IF NOT EXISTS ingredients(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL, " +
                "quantity REAL, " +
                "unit TEXT, " +
                "expirationDate TEXT" +
                ");";

        // Classic connection check
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Ingredient table is ready!");

        } catch (SQLException e) {
            System.out.println("problem while creating a table: " + e.getMessage());
        }
    }

    public void addIngredient(Ingredient ingredient) throws SQLException {
        String sql = "INSERT INTO ingredients(name, quantity, unit, expirationDate) VALUES(?, ?, ?, ?)";

        // Asking database to return the generated keys (auto-incremented id)
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, ingredient.getName());
            pstmt.setDouble(2, ingredient.getQuantity());
            pstmt.setString(3, ingredient.getUnit());

            // LocalDate needs to be converted to String for SQLite
            pstmt.setString(4, ingredient.getExpirationDate().toString());

            pstmt.executeUpdate();

            // Getting the newly generated id from database
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                long newId = rs.getLong(1);
                // Updating our Java object with the real database id
                ingredient.setIngredientId(newId);
            }
        }
    }

    public List<Ingredient> getAllIngredients() throws SQLException {
        List<Ingredient> ingredientList = new ArrayList<>();
        String sql = "SELECT * FROM ingredients";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                double quantity = rs.getDouble("quantity");
                String unit = rs.getString("unit");

                // Parsing string back to LocalDate
                LocalDate expirationDate = LocalDate.parse(rs.getString("expirationDate"));

                Ingredient ingredient = new Ingredient(id, name, quantity, unit, expirationDate);

                // Setting the ID explicitly so later updates target the correct row
                ingredient.setIngredientId(id);

                ingredientList.add(ingredient);
            }
        }

        return ingredientList;
    }

    public void updateIngredientQuantity(Long ingredientId, double newQuantity) throws SQLException {
        String sql = "UPDATE ingredients SET quantity = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Now we will fill the ?
            pstmt.setDouble(1, newQuantity);
            pstmt.setLong(2, ingredientId);

            // To see if it was successful or not
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("STEP UPDATE: ingredient quantity successfully updated to " + newQuantity);
            } else {
                System.out.println("STEP UPDATE: could not find an ingredient with ID: " + ingredientId);
            }
        }
    }

    // To remove an ingredient from our database
    public void deleteIngredient(Long ingredientId) throws SQLException {
        String sql = "DELETE FROM ingredients WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, ingredientId);

            // Execute the delete command
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("STEP DELETE: ingredient with ID " + ingredientId + " successfully deleted!");
            } else {
                System.out.println("STEP DELETE: could not find an ingredient with ID: " + ingredientId + " to delete.");
            }
        }
    }

    // Fetching a single ingredient by its id
    public Ingredient getIngredientById(Long searchId) throws SQLException {
        String sql = "SELECT * FROM ingredients WHERE id = ?";
        Ingredient foundIngredient = null;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, searchId);
            ResultSet rs = pstmt.executeQuery();

            // If we find a row, extract data and build the object
            if (rs.next()) {
                String name = rs.getString("name");
                double quantity = rs.getDouble("quantity");
                String unit = rs.getString("unit");

                // Getting date from db as string and parsing it to LocalDate
                String expDateString = rs.getString("expirationDate");
                LocalDate expDate = LocalDate.parse(expDateString);

                // Passing this LocalDate when creating the object
                foundIngredient = new Ingredient(name, quantity, unit, expDate);

                // Setting the id explicitly
                foundIngredient.setIngredientId(rs.getLong("id"));
            }
        }

        return foundIngredient; // Returns null if nothing is found
    }
}