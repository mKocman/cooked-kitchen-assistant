package model;

import java.io.Serializable;

// Safe box to transfer recipe data over the network
public class RecipeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private int prepTime;
    private String instructions;

    // Constructor for packaging recipe data
    public RecipeDTO(Long id, String name, int prepTime, String instructions) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime;
        this.instructions = instructions;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrepTime() { return prepTime; }
    public String getInstructions() { return instructions; }

    // This makes sure the recipes look nice when we put them in a GUI list
    @Override
    public String toString() {
        return name + " (" + prepTime + " mins) - " + instructions;
    }
}