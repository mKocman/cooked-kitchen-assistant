package model;
import java.time.LocalDate;

public class Ingredient {
    //I am making an id because this way I can prevent the problems such as same name but different product,
    //each id will be a unique one
    private Long ingredientId;
    private String name;
    private double quantity;
    private String unit;
    private LocalDate expirationDate;

    public Ingredient(String name, double quantity, String unit, LocalDate expirationDate) {
        this.name = name;
        this.ingredientId=null;
        this.unit = unit;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }

    public Ingredient(Long ingredientId, String name, double quantity, String unit, LocalDate expirationDate) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void usedAmount(double consumedAmount) {
        if (consumedAmount<0){
            throw new IllegalArgumentException("Error : Used amount cannot be less than 0!");
        }
        if(consumedAmount>this.quantity){
            throw new IllegalArgumentException("Not enought material in the invertory!");
        }
        this.quantity -= consumedAmount;
    }
        @Override
        public int hashCode () {
            return ingredientId != null ? ingredientId.hashCode(): super.hashCode();
        }
        @Override
        public String toString () {
            return name + "(" + quantity + "" + unit + ")" + "(MHD: " + expirationDate + ")";
        }
        @Override
        public boolean equals (Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ingredient that = (Ingredient) o;
            if (this.ingredientId == null || that.ingredientId == null) {
                return false;
            }
            return this.ingredientId.equals(that.ingredientId);
        }

}