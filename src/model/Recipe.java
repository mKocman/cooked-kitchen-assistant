package model;
import java.util.List;

public class Recipe {
    // again I will be using this unique id since i want to have multiple variations of the same recipe
    private Long recipeId;
    private String name;
    // list for required ingredients that contains Ingredients from other class
    private List<Ingredient> requiredIngredients;
    private int prepTime;
    private String instructions;

    // constructor 1 without id
    public Recipe(String name, int prepTime, List<Ingredient> requiredIngredients, String instructions) {
        this.recipeId = null;
        this.name = name;
        this.prepTime = prepTime;
        this.requiredIngredients = requiredIngredients;
        this.instructions = instructions;
    }

    // constructor 2 with id and instructions included
    public Recipe(Long recipeId, String name, int prepTime, List<Ingredient> requiredIngredients, String instructions) {
        this.recipeId = recipeId;
        this.name = name;
        this.prepTime = prepTime;
        this.requiredIngredients = requiredIngredients;
        this.instructions = instructions;
    }

    // getter setter methods
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrepTime() { return prepTime; }
    public void setPrepTime(int prepTime) { this.prepTime = prepTime; }

    public List<Ingredient> getRequiredIngredients() { return requiredIngredients; }
    public void setRequiredIngredients(List<Ingredient> requiredIngredients) {
        // TODO: support updating a single ingredient instead of replacing the whole list
        this.requiredIngredients = requiredIngredients;
    }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    // instructions getter setter
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    // rewriting equals so it checks the id, not just the name. null ids mean they are new and not equal
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recipe recipe = (Recipe) o;

        // if either id is null we cant be sure so return false
        if (this.recipeId == null || recipe.recipeId == null) {
            return false;
        }

        return this.recipeId.equals(recipe.recipeId);
    }

    @Override
    public int hashCode() {
        return recipeId != null ? recipeId.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return "Recipe " + name + " takes : " + prepTime + " minutes to make. Instructions: " + instructions;
    }
}