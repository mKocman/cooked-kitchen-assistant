package model;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// Runnable task: each connected client is handled in its own thread
public class ClientHandler implements Runnable {
    private Socket socket;
    private IngredientHandler ingredientHandler;
    private RecipeHandler recipeHandler;

    public ClientHandler(Socket socket, IngredientHandler ingredientHandler, RecipeHandler recipeHandler) {
        this.socket = socket;
        this.ingredientHandler = ingredientHandler;
        this.recipeHandler = recipeHandler;
    }

    @Override
    public void run() {
        // setting up the input and output pipes for OBJECTS
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("HANDLER: ready to receive commands.");

            // keep listening to this client until they say BYE
            while (true) {
                // 1. read the incoming request package
                Request request = (Request) in.readObject();
                Command command = request.getCommand();

                System.out.println("HANDLER: received command -> " + command);

                // 2. if client wants to leave, break the loop and close connection
                if (command == Command.BYE) {
                    out.writeObject(new Response(Status.OK, null, "Goodbye!"));
                    out.flush();
                    System.out.println("HANDLER: client disconnected cleanly.");
                    break;
                }

                // 3. routing the command to the right action
                switch (command) {
                    case ADD_INGREDIENT:
                        // checking if the payload is actually an ingredientdto before casting
                        if (!(request.getPayload() instanceof IngredientDTO)) {
                            out.writeObject(new Response(Status.ERROR, null, "invalid data format sent to server"));
                            out.flush();
                            break;
                        }

                        IngredientDTO dto = (IngredientDTO) request.getPayload();

                        // validating the data so we dont save garbage to the database
                        if (dto.getName() == null || dto.getName().trim().isEmpty() || dto.getQuantity() <= 0) {
                            out.writeObject(new Response(Status.ERROR, null, "name cannot be empty and quantity must be greater than 0"));
                            out.flush();
                            break;
                        }

                        // if everything is fine, we try to save it
                        try {
                            java.time.LocalDate parsedDate = java.time.LocalDate.parse(dto.getExpirationDate());
                            Ingredient newIng = new Ingredient(dto.getName(), dto.getQuantity(), dto.getUnit(), parsedDate);

                            synchronized (ingredientHandler) {
                                ingredientHandler.addIngredient(newIng);
                            }

                            out.writeObject(new Response(Status.OK, null, "ingredient added safely!"));
                        } catch (Exception ex) {
                            // catching the sqlexception from handler and sending it back to the client
                            out.writeObject(new Response(Status.ERROR, null, "database error: " + ex.getMessage()));
                        }
                        out.flush();
                        break;

                    case GET_INGREDIENTS:
                        try {
                            List<Ingredient> inventory;
                            synchronized (ingredientHandler) {
                                inventory = ingredientHandler.getAllIngredients();
                            }

                            List<IngredientDTO> dtoList = new ArrayList<>();
                            for (Ingredient ing : inventory) {
                                // now passing the real id from database into our safe dto box
                                dtoList.add(new IngredientDTO(
                                        ing.getIngredientId(),
                                        ing.getName(),
                                        ing.getQuantity(),
                                        ing.getUnit(),
                                        ing.getExpirationDate().toString()
                                ));
                            }

                            out.writeObject(new Response(Status.OK, dtoList, "inventory loaded."));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "failed to load inventory: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case GET_RECIPES:
                        try {
                            List<Recipe> allRecipes;

                            // Locking the recipe handler while reading from database
                            synchronized (recipeHandler) {
                                allRecipes = recipeHandler.getAllRecipes();
                            }

                            // Converting heavy database objects into light DTO boxes for network transfer
                            List<RecipeDTO> recipeDTOList = new ArrayList<>();
                            for (Recipe r : allRecipes) {
                                recipeDTOList.add(new RecipeDTO(
                                        r.getRecipeId(),
                                        r.getName(),
                                        r.getPrepTime(),
                                        r.getInstructions()
                                ));
                            }

                            out.writeObject(new Response(Status.OK, recipeDTOList, "Recipes loaded successfully."));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Failed to load recipes: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case GET_RECIPE_REQUIREMENTS:
                        Long recipeId = (Long) request.getPayload();
                        try {
                            // Fetching details from recipe_ingredients table
                            List<String> requirements = recipeHandler.getRecipeRequirements(recipeId);
                            out.writeObject(new Response(Status.OK, requirements, "Requirements loaded"));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Error loading requirements"));
                        }
                        out.flush();
                        break;
                    case MAKE_RECIPE:
                        // Checking if the payload is a valid ID
                        if (!(request.getPayload() instanceof Long)) {
                            out.writeObject(new Response(Status.ERROR, null, "invalid recipe id sent"));
                            out.flush();
                            break;
                        }

                        Long recipeIdToMake = (Long) request.getPayload();

                        // a recipe without requirements cannot be cooked (otherwise it would "succeed" without using anything)
                        if (recipeHandler.getRecipeRequirements(recipeIdToMake).isEmpty()) {
                            out.writeObject(new Response(Status.ERROR, null, "This recipe has no ingredients yet. Double-click it to add some."));
                            out.flush();
                            break;
                        }

                        try {
                            boolean isSuccess;

                            // Locking both handlers so the stock cannot change during the check-and-consume step
                            // This is crucial for thread safety.
                            synchronized (recipeHandler) {
                                synchronized (ingredientHandler) {
                                    // run the canBeMade() check
                                    isSuccess = recipeHandler.canBeMade(recipeIdToMake, ingredientHandler);
                                }
                            }

                            if (isSuccess) {
                                out.writeObject(new Response(Status.OK, null, "Recipe cooked successfully! Stock updated."));
                            } else {
                                out.writeObject(new Response(Status.ERROR, null, "Not enough ingredients or some are expired!"));
                            }
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Database error while cooking: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case DELETE_INGREDIENT:
                        // checking if the payload is a valid ID number (Long)
                        if (!(request.getPayload() instanceof Long)) {
                            out.writeObject(new Response(Status.ERROR, null, "invalid id format sent for deletion"));
                            out.flush();
                            break;
                        }

                        // changed the variable name so it doesn't clash with delete_recipe
                        Long ingIdToDelete = (Long) request.getPayload();

                        try {
                            // locking the database handler while deleting
                            synchronized (ingredientHandler) {
                                ingredientHandler.deleteIngredient(ingIdToDelete);
                            }

                            out.writeObject(new Response(Status.OK, null, "ingredient deleted successfully!"));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "database error while deleting: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case DELETE_RECIPE:
                        // payload is just the recipe ID (Long)
                        Long idToDelete = (Long) request.getPayload();

                        synchronized (recipeHandler) {
                            recipeHandler.deleteRecipe(idToDelete);
                        }

                        out.writeObject(new Response(Status.OK, null, "Recipe deleted."));
                        out.flush();
                        break;

                    default:
                        out.writeObject(new Response(Status.ERROR, null, "Unknown command!"));
                        out.flush();
                        break;
                    case ADD_RECIPE:
                        // Checking if the payload is a RecipeDTO
                        if (!(request.getPayload() instanceof RecipeDTO)) {
                            out.writeObject(new Response(Status.ERROR, null, "Invalid recipe data sent to server."));
                            out.flush();
                            break;
                        }


                        RecipeDTO recipeDto = (RecipeDTO) request.getPayload();

                        // Basic validation
                        if (recipeDto.getName() == null || recipeDto.getName().trim().isEmpty() || recipeDto.getPrepTime() <= 0) {
                            out.writeObject(new Response(Status.ERROR, null, "Recipe name cannot be empty and prep time must be > 0."));
                            out.flush();
                            break;
                        }

                        try {
                            // Converting DTO back to a heavy Recipe object
                            Recipe newRecipe = new Recipe(
                                    recipeDto.getName(),
                                    recipeDto.getPrepTime(),
                                    new ArrayList<>(), // Empty ingredients list for now
                                    recipeDto.getInstructions()
                            );

                            // Locking the database handler to safely write
                            synchronized (recipeHandler) {
                                recipeHandler.addRecipe(newRecipe);
                            }

                            out.writeObject(new Response(Status.OK, null, "Recipe added successfully!"));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Database error: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case ADD_REQUIREMENT:
                        // Check if the incoming box is our new DTO
                        if (!(request.getPayload() instanceof IngredientRequirementDTO)) {
                            out.writeObject(new Response(Status.ERROR, null, "Invalid requirement data."));
                            out.flush();
                            break;
                        }

                        IngredientRequirementDTO reqDto = (IngredientRequirementDTO) request.getPayload();

                        try {
                            // Lock database and write the new requirement to the junction table
                            synchronized (recipeHandler) {
                                recipeHandler.addIngredientRequirement(reqDto.recipeId, reqDto.name, reqDto.qty, reqDto.unit);
                            }
                            out.writeObject(new Response(Status.OK, null, reqDto.name + " added to the recipe!"));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Database error: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                    case GET_EXPIRED:
                        try {
                            // Using inventory class to find expired items
                            Inventory inventoryObj = new Inventory(ingredientHandler);
                            List<Ingredient> expiredList;

                            synchronized (ingredientHandler) {
                                expiredList = inventoryObj.findExpired();
                            }

                            // changing into DTO and sending it
                            List<IngredientDTO> expiredDTOs = new ArrayList<>();
                            for (Ingredient ing : expiredList) {
                                expiredDTOs.add(new IngredientDTO(ing.getIngredientId(), ing.getName(), ing.getQuantity(), ing.getUnit(), ing.getExpirationDate().toString()));
                            }

                            out.writeObject(new Response(Status.OK, expiredDTOs, "Expired items loaded."));
                        } catch (Exception ex) {
                            out.writeObject(new Response(Status.ERROR, null, "Error finding expired items: " + ex.getMessage()));
                        }
                        out.flush();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("HANDLER ERROR: connection lost -> " + e.getMessage());
        }
    }

}