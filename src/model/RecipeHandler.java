package model;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RecipeHandler {

    public void createTable() {
        // Added 'instructions' text to match our updated recipe class
        String sql = "CREATE TABLE IF NOT EXISTS recipes(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "preptime INTEGER," +
                "instructions TEXT" +
                ");";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("STEP RECIPE: recipe table is ready");

        } catch (SQLException e) {
            System.out.println("error creating recipe table: " + e.getMessage());
        }
    }

    public void addRecipe(Recipe recipe) {
        String sql = "INSERT INTO recipes (name, preptime, instructions) VALUES(?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, recipe.getName());
            pstmt.setInt(2, recipe.getPrepTime());
            pstmt.setString(3, recipe.getInstructions());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                long newId = rs.getLong(1);
                recipe.setRecipeId(newId);
            }

            System.out.println("STEP RECIPE: recipe '" + recipe.getName() + "' added to database!");

        } catch (SQLException e) {
            System.out.println("error adding recipe: " + e.getMessage());
        }
    }

    public Recipe getRecipeById(long recipeId) {
        String sql = "SELECT * FROM recipes WHERE id = ?";
        Recipe foundRecipe = null;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, recipeId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                int prepTime = rs.getInt("preptime");
                String instructions = rs.getString("instructions");

                foundRecipe = new Recipe(recipeId, name, prepTime, new ArrayList<>(), instructions);
            }
        } catch (SQLException e) {
            System.out.println("error fetching recipe: " + e.getMessage());
        }
        return foundRecipe;
    }

    public void updateRecipeInstructions(long recipeId, String newInstructions) {
        String sql = "UPDATE recipes SET instructions = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newInstructions);
            pstmt.setLong(2, recipeId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("STEP UPDATE: recipe instructions updated successfully.");
            }
        } catch (SQLException e) {
            System.out.println("error updating recipe: " + e.getMessage());
        }
    }

    public void deleteRecipe(long recipeId) {
        String sql = "DELETE FROM recipes WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, recipeId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("STEP DELETE: recipe deleted successfully.");
            }
        } catch (SQLException e) {
            System.out.println("error deleting recipe: " + e.getMessage());
        }
    }

    public void createRecipeIngredientsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS recipe_ingredients(" +
                "recipe_id INTEGER," +
                "ingredient_name TEXT NOT NULL," +
                "required_quantity REAL," +
                "unit TEXT," +
                "FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                ");";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("STEP RECIPE: junction table 'recipe_ingredients' is ready");
        } catch (SQLException e) {
            System.out.println("error while creating a junction table: " + e.getMessage());
        }
    }

    public void addIngredientRequirement(long recipeId, String ingredientName, double required_quantity, String unit) {
        String sql = "INSERT INTO recipe_ingredients (recipe_id, ingredient_name, required_quantity, unit) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, recipeId);
            pstmt.setString(2, ingredientName);
            pstmt.setDouble(3, required_quantity);
            pstmt.setString(4, unit);
            pstmt.executeUpdate();
            System.out.println("STEP RECIPE: required ingredient '" + ingredientName + "' added to recipe " + recipeId + "!");
        } catch (SQLException e) {
            System.out.println("error while adding requirements to recipe: " + e.getMessage());
        }
    }

    // 5. canBeMade(): reads the requirements first, then checks and consumes stock
    //    (two separate connections to avoid SQLite database locks)
    public boolean canBeMade(long recipeId, IngredientHandler ingredientHandler) {

        class Requirement {
            String name; double qty; String unit;
            Requirement(String n, double q, String u) { name = n; qty = q; unit = u; }
        }

        List<Requirement> requirements = new ArrayList<>();
        String fetchReqSql = "SELECT ingredient_name, required_quantity, unit FROM recipe_ingredients WHERE recipe_id = ?";

        // STAGE 1: READ REQUIREMENTS (Open and immediately close the connection)
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(fetchReqSql)) {

            pstmt.setLong(1, recipeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requirements.add(new Requirement(
                            rs.getString("ingredient_name"),
                            rs.getDouble("required_quantity"),
                            rs.getString("unit")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("error while fetching requirements: " + e.getMessage());
            return false; // Safely exit if DB fails
        }

        if (requirements.isEmpty()) {
            System.out.println("x recipe " + recipeId + " has no required ingredients yet - nothing to cook.");
            return false;
        }

        // STAGE 2: CHECK AND CONSUME
        try {
            List<Ingredient> inventoryList = ingredientHandler.getAllIngredients();
            System.out.println("\n--- checking ingredients for recipe id: " + recipeId + " ---");

            // CHECK STEP
            for (Requirement req : requirements) {
                double totalAvailable = 0.0;

                for (Ingredient ing : inventoryList) {
                    if (ing.getName().equalsIgnoreCase(req.name)) {

                        if (!ing.getUnit().equalsIgnoreCase(req.unit)) {
                            System.out.println("   -> ignoring " + ing.getName() + " because units don't match (" + ing.getUnit() + " != " + req.unit + ")");
                            continue;
                        }

                        if (ing.getExpirationDate().isBefore(LocalDate.now())) {
                            System.out.println("   -> ignoring " + ing.getName() + " because it is expired!");
                            continue;
                        }

                        totalAvailable += ing.getQuantity();
                    }
                }

                if (totalAvailable < req.qty) {
                    double missingAmount = req.qty - totalAvailable;
                    System.out.println("x MISSING: " + req.name + " (you need " + req.qty + ", but you only have " + totalAvailable + ". missing " + missingAmount + "!)");
                    return false; // Exit immediately if we lack ingredients
                } else {
                    System.out.println("v OK: " + req.name + " (you need " + req.qty + ", and you have " + totalAvailable + ")");
                }
            }

            // CONSUME STEP
            System.out.println("SUCCESS: all required ingredients are available. Updating stock...");

            for (Requirement req : requirements) {
                double amountNeededToDeduct = req.qty;

                for (Ingredient ing : inventoryList) {
                    if (amountNeededToDeduct <= 0) break;

                    if (ing.getName().equalsIgnoreCase(req.name) &&
                            ing.getUnit().equalsIgnoreCase(req.unit) &&
                            !ing.getExpirationDate().isBefore(LocalDate.now()) &&
                            ing.getQuantity() > 0) {

                        double amountToTake = Math.min(ing.getQuantity(), amountNeededToDeduct);
                        double newQuantity = ing.getQuantity() - amountToTake;

                        // safe to update because the STAGE 1 connection is already closed
                        ingredientHandler.updateIngredientQuantity(ing.getIngredientId(), newQuantity);

                        amountNeededToDeduct -= amountToTake;
                    }
                }
            }
            System.out.println("RECIPE COMPLETE: stock updated successfully.");
            return true;

        } catch (SQLException e) {
            System.out.println("error while updating stock: " + e.getMessage());
            return false;
        }
    }

    public List<Recipe> getAllRecipes() throws SQLException {
        List<Recipe> recipeList = new ArrayList<>();
        String sql = "SELECT * FROM recipes";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                int prepTime = rs.getInt("prepTime");
                String instructions = rs.getString("instructions");

                Recipe recipe = new Recipe(name, prepTime, new ArrayList<>(), instructions);
                recipe.setRecipeId(id);

                recipeList.add(recipe);
            }
        }
        return recipeList;
    }
    public List<String> getRecipeRequirements(long recipeId) {
        List<String> reqs = new ArrayList<>();
        String sql = "SELECT ingredient_name, required_quantity, unit FROM recipe_ingredients WHERE recipe_id = ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, recipeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reqs.add(rs.getString("ingredient_name") + ": " +
                        rs.getDouble("required_quantity") + " " + rs.getString("unit"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reqs;
    }
}