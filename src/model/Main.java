package model;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class Main {
  public static void main(String[] args)throws SQLException {

    System.out.println("=== 1. INITIALIZING DATABASE AND TABLES ===");
    IngredientHandler ingredientHandler = new IngredientHandler();
    ingredientHandler.createTable();

    RecipeHandler recipeHandler = new RecipeHandler();
    recipeHandler.createTable();
    recipeHandler.createRecipeIngredientsTable();

    System.out.println("\n=== 2. ADDING 5 TEST INGREDIENTS TO INVENTORY ===");
    // creating 5 ingredients (one is intentionally expired to test the algorithm)
    Ingredient milk = new Ingredient("Milk", 2000.0, "ml", LocalDate.now().plusDays(10));
    Ingredient flour = new Ingredient("Flour", 1000.0, "g", LocalDate.now().plusMonths(6));
    Ingredient eggs = new Ingredient("Eggs", 12.0, "pcs", LocalDate.now().plusDays(14));
    Ingredient expiredCocoa = new Ingredient("Cocoa", 500.0, "g", LocalDate.now().minusDays(5)); // EXPIRED!
    Ingredient sugar = new Ingredient("Sugar", 2000.0, "g", LocalDate.now().plusYears(1));

    ingredientHandler.addIngredient(milk);
    ingredientHandler.addIngredient(flour);
    ingredientHandler.addIngredient(eggs);
    ingredientHandler.addIngredient(expiredCocoa);
    ingredientHandler.addIngredient(sugar);

    System.out.println("\n=== 3. ADDING 5 TEST RECIPES ===");
    // creating 5 recipes with instructions
    Recipe pancakes = new Recipe("Pancakes", 15, new ArrayList<>(), "Mix flour, milk, and eggs. Fry in a pan.");
    Recipe cake = new Recipe("Chocolate Cake", 60, new ArrayList<>(), "Mix flour, eggs, sugar, and cocoa. Bake for 45 mins.");
    Recipe omelette = new Recipe("Omelette", 10, new ArrayList<>(), "Beat eggs, fry in a hot pan.");
    Recipe hotChocolate = new Recipe("Hot Chocolate", 5, new ArrayList<>(), "Heat milk and mix with cocoa.");
    Recipe sweetMilk = new Recipe("Sweet Milk", 2, new ArrayList<>(), "Warm the milk and add sugar.");

    recipeHandler.addRecipe(pancakes);
    recipeHandler.addRecipe(cake);
    recipeHandler.addRecipe(omelette);
    recipeHandler.addRecipe(hotChocolate);
    recipeHandler.addRecipe(sweetMilk);

    System.out.println("\n=== 4. ADDING REQUIREMENTS (JUNCTION TABLE) ===");
    // pancakes need: 200g flour, 300ml milk, 2 eggs
    recipeHandler.addIngredientRequirement(pancakes.getRecipeId(), "Flour", 200.0, "g");
    recipeHandler.addIngredientRequirement(pancakes.getRecipeId(), "Milk", 300.0, "ml");
    recipeHandler.addIngredientRequirement(pancakes.getRecipeId(), "Eggs", 2.0, "pcs");

    // cake needs: 300g flour, 4 eggs, 200g sugar, 100g cocoa (this should fail because cocoa is expired!)
    recipeHandler.addIngredientRequirement(cake.getRecipeId(), "Flour", 300.0, "g");
    recipeHandler.addIngredientRequirement(cake.getRecipeId(), "Eggs", 4.0, "pcs");
    recipeHandler.addIngredientRequirement(cake.getRecipeId(), "Sugar", 200.0, "g");
    recipeHandler.addIngredientRequirement(cake.getRecipeId(), "Cocoa", 100.0, "g");

    // omelette needs: 3 eggs
    recipeHandler.addIngredientRequirement(omelette.getRecipeId(), "Eggs", 3.0, "pcs");

    // hot chocolate needs: 250ml milk, 20g cocoa (this should also fail because cocoa is expired)
    recipeHandler.addIngredientRequirement(hotChocolate.getRecipeId(), "Milk", 250.0, "ml");
    recipeHandler.addIngredientRequirement(hotChocolate.getRecipeId(), "Cocoa", 20.0, "g");

    System.out.println("\n=== 5. TESTING NEW CRUD OPERATIONS ===");
    // testing read
    Recipe fetchedRecipe = recipeHandler.getRecipeById(pancakes.getRecipeId());
    if (fetchedRecipe != null) {
      System.out.println("READ TEST SUCCESS: Fetched recipe -> " + fetchedRecipe.getName());
    }

    // testing update
    recipeHandler.updateRecipeInstructions(omelette.getRecipeId(), "Add salt and pepper to the beaten eggs before frying.");

    // testing delete (deleting sweet milk)
    recipeHandler.deleteRecipe(sweetMilk.getRecipeId());

    System.out.println("\n=== 6. TESTING THE BIG ALGORITHM (canBeMade) ===");
    // checking pancakes (should succeed)
    System.out.println("\n>> Trying to make Pancakes:");
    recipeHandler.canBeMade(pancakes.getRecipeId(), ingredientHandler);

    // checking cake (should fail because cocoa is expired)
    System.out.println("\n>> Trying to make Chocolate Cake:");
    recipeHandler.canBeMade(cake.getRecipeId(), ingredientHandler);

    System.out.println("\n=== ALL TESTS COMPLETED SUCCESSFULLY! ===");
  }
}