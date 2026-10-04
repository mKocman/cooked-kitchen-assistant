package model;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Inventory {

    private IngredientHandler ingredientHandler;

    // Constructor: We connect the inventory to our database handler
    public Inventory(IngredientHandler ingredientHandler) {
        this.ingredientHandler = ingredientHandler;
    }

    // This method checks the database and returns a list of EXPIRED ingredients
    // We added 'throws SQLException' because getAllIngredients() requires it now
    public List<Ingredient> findExpired() throws SQLException {
        List<Ingredient> expiredList = new ArrayList<>();
        List<Ingredient> allIngredients = ingredientHandler.getAllIngredients();

        // We get today's date to compare
        LocalDate today = LocalDate.now();

        for (Ingredient ing : allIngredients) {
            // If the expiration date is BEFORE today, it means it is expired
            if (ing.getExpirationDate().isBefore(today)) {
                expiredList.add(ing);
            }
        }

        return expiredList;
    }

    // Prints how many items are currently in the database
    @Override
    public String toString() {
        try {
            // We must use try-catch here because an overridden toString() method cannot throw exceptions
            int totalItems = ingredientHandler.getAllIngredients().size();
            return "Inventory status: " + totalItems + " ingredients available in the database.";
        } catch (SQLException e) {
            // Returning a safe string if the database crashes while trying to print the status
            return "Inventory status: Error connecting to database.";
        }
    }

}