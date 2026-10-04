package model;

import java.io.Serializable;

public class IngredientDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // we need the id so the gui knows exactly which item to delete or update
    private Long id;
    private String name;
    private double quantity;
    private String unit;
    private String expirationDate;

    // constructor for adding new items (database will generate the id later, so it is null for now)
    public IngredientDTO(String name, double quantity, String unit, String expirationDate) {
        this.id = null;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expirationDate = expirationDate;
    }

    // constructor for fetching existing items from the database
    public IngredientDTO(Long id, String name, double quantity, String unit, String expirationDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expirationDate = expirationDate;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getExpirationDate() { return expirationDate; }

    // this makes sure the gui listview displays the object nicely with the expiration date
    @Override
    public String toString() {
        return name + " - " + quantity + " " + unit + " (Exp: " + expirationDate + ")";
    }
}