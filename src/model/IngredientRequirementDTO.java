package model;
import java.io.Serializable;

public class IngredientRequirementDTO implements Serializable {
    public Long recipeId;
    public String name;
    public double qty;
    public String unit;

    public IngredientRequirementDTO(Long recipeId, String name, double qty, String unit) {
        this.recipeId = recipeId;
        this.name = name;
        this.qty = qty;
        this.unit = unit;
    }
}