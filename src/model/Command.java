package model;

// List of all possible commands the client can send to the server
public enum Command {
    // Ingredient commands
    ADD_INGREDIENT,
    GET_INGREDIENTS,
    DELETE_INGREDIENT,
    UPDATE_INGREDIENT,
    GET_RECIPE_REQUIREMENTS,
    GET_EXPIRED,
    // Recipe commands
    GET_RECIPES,
    ADD_REQUIREMENT,
    ADD_RECIPE,
    DELETE_RECIPE,
    MAKE_RECIPE, // This will trigger the algorithm to check and consume ingredients

    BYE
}