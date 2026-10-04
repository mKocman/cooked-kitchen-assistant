package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.*;
import view.MainGUI;

import java.util.List;

public class MainController {

    private MainGUI view;

    public MainController(MainGUI view) {
        this.view = view;
        attachEvents();
    }

    private void attachEvents() {
        view.getLoadBtn().setOnAction(e -> {
            view.getStatusArea().appendText(">> Refreshing database...\n");
            loadIngredientsFromServer();
            loadRecipesFromServer();
        });

        view.getAddIngBtn().setOnAction(e -> addIngredientToServer());
        view.getDeleteIngBtn().setOnAction(e -> deleteIngredientFromServer());
        view.getMakeRecipeBtn().setOnAction(e -> makeSelectedRecipe());
        view.getCheckExpiredBtn().setOnAction(e -> checkExpiredIngredients());
        view.getDeleteRecipeBtn().setOnAction(e -> deleteRecipeFromServer());

        // binding the add recipe button to the method
        view.getAddRecipeBtn().setOnAction(e -> addRecipeToServer());

        view.getRecipeList().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                view.getStatusArea().appendText(">> Selected: " + newVal.getName() + " - Click 'Make Recipe!' to cook, double-click to view or add its ingredients.\n");
            }
        });

        // double-click listener to open the requirement manager for recipes
        view.getRecipeList().setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RecipeDTO selected = view.getRecipeList().getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openRecipeDetailWindow(selected);
                }
            }
        });
    }

    private void loadRecipesFromServer() {
        Task<List<RecipeDTO>> fetchRecipesTask = new Task<>() {
            @Override
            protected List<RecipeDTO> call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                Request getReq = new Request(Command.GET_RECIPES, null);
                Response serverResponse = localClient.sendRequest(getReq);

                @SuppressWarnings("unchecked")
                List<RecipeDTO> recipeList = (List<RecipeDTO>) serverResponse.getPayload();

                localClient.disconnect();
                return recipeList;
            }
        };

        fetchRecipesTask.setOnSucceeded(e -> {
            view.getRecipeList().getItems().clear();
            view.getRecipeList().getItems().addAll(fetchRecipesTask.getValue());
        });

        new Thread(fetchRecipesTask).start();
    }

    private void loadIngredientsFromServer() {
        view.getLoadBtn().setDisable(true);

        Task<List<IngredientDTO>> fetchTask = new Task<>() {
            @Override
            protected List<IngredientDTO> call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                Request getReq = new Request(Command.GET_INGREDIENTS, null);
                Response serverResponse = localClient.sendRequest(getReq);

                @SuppressWarnings("unchecked")
                List<IngredientDTO> inventory = (List<IngredientDTO>) serverResponse.getPayload();

                localClient.disconnect();
                return inventory;
            }
        };

        fetchTask.setOnSucceeded(e -> {
            view.getIngredientList().getItems().clear();
            view.getIngredientList().getItems().addAll(fetchTask.getValue());
            view.getLoadBtn().setDisable(false);
        });

        fetchTask.setOnFailed(e -> {
            view.getStatusArea().appendText("Error: Could not refresh inventory!\n");
            view.getLoadBtn().setDisable(false);
        });

        new Thread(fetchTask).start();
    }

    private void addIngredientToServer() {
        // added .trim() to clean invisible spaces from user input
        String name = view.getNameInput().getText().trim();
        String qtyText = view.getQuantityInput().getText().trim();

        // reading from the dropdown menu
        String unit = view.getUnitInput().getValue();

        if (name.isEmpty() || qtyText.isEmpty() || unit == null) {
            view.getStatusArea().appendText("Error: Please fill in all fields!\n");
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(qtyText);
        } catch (NumberFormatException e) {
            view.getStatusArea().appendText("Error: Quantity must be a number!\n");
            return;
        }

        view.getStatusArea().appendText("Adding ingredient...\n");
        view.getAddIngBtn().setDisable(true);

        Task<Boolean> addTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                String expDate = java.time.LocalDate.now().plusMonths(1).toString();
                IngredientDTO newIng = new IngredientDTO(name, quantity, unit, expDate);

                Request addReq = new Request(Command.ADD_INGREDIENT, newIng);
                Response response = localClient.sendRequest(addReq);

                localClient.disconnect();
                return response.getStatus() == model.Status.OK;
            }
        };

        addTask.setOnSucceeded(e -> {
            if (addTask.getValue()) {
                view.getStatusArea().appendText("Success: Ingredient added!\n");
                view.getNameInput().clear();
                view.getQuantityInput().clear();
                loadIngredientsFromServer();
            } else {
                view.getStatusArea().appendText("Error: Server refused ingredient.\n");
            }
            view.getAddIngBtn().setDisable(false);
        });

        addTask.setOnFailed(e -> {
            view.getStatusArea().appendText("Error: Could not add to server!\n");
            view.getAddIngBtn().setDisable(false);
        });

        new Thread(addTask).start();
    }

    private void deleteIngredientFromServer() {
        IngredientDTO selectedItem = view.getIngredientList().getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            view.getStatusArea().appendText("Error: Select an ingredient to delete!\n");
            return;
        }

        view.getStatusArea().appendText("Deleting...\n");
        view.getDeleteIngBtn().setDisable(true);

        Task<Boolean> deleteTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                Request delReq = new Request(Command.DELETE_INGREDIENT, selectedItem.getId());
                Response response = localClient.sendRequest(delReq);

                localClient.disconnect();
                return response.getStatus() == model.Status.OK;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            if (deleteTask.getValue()) {
                view.getStatusArea().appendText("Success: Item deleted!\n");
                loadIngredientsFromServer();
            } else {
                view.getStatusArea().appendText("Error: Server refused deletion.\n");
            }
            view.getDeleteIngBtn().setDisable(false);
        });

        deleteTask.setOnFailed(e -> {
            view.getStatusArea().appendText("Error: Server connection failed.\n");
            view.getDeleteIngBtn().setDisable(false);
        });

        new Thread(deleteTask).start();
    }

    private void makeSelectedRecipe() {
        RecipeDTO selectedRecipe = view.getRecipeList().getSelectionModel().getSelectedItem();

        if (selectedRecipe == null) {
            view.getStatusArea().appendText("Error: Please select a recipe!\n");
            return;
        }

        view.getStatusArea().appendText("Cooking " + selectedRecipe.getName() + "...\n");
        view.getMakeRecipeBtn().setDisable(true);

        Task<Response> makeTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                Request makeReq = new Request(Command.MAKE_RECIPE, selectedRecipe.getId());
                Response response = localClient.sendRequest(makeReq);

                localClient.disconnect();
                return response;
            }
        };

        makeTask.setOnSucceeded(e -> {
            Response serverResponse = makeTask.getValue();

            if (serverResponse.getStatus() == model.Status.OK) {
                view.getStatusArea().appendText(">> SUCCESS: " + serverResponse.getMessage() + "\n");
                loadIngredientsFromServer();
            } else {
                view.getStatusArea().appendText(">> ERROR: " + serverResponse.getMessage() + "\n");
            }
            view.getMakeRecipeBtn().setDisable(false);
        });

        makeTask.setOnFailed(e -> {
            view.getStatusArea().appendText(">> ERROR: Could not connect to cook.\n");
            view.getMakeRecipeBtn().setDisable(false);
        });

        new Thread(makeTask).start();
    }

    // sends a request to create a new recipe in the database
    private void addRecipeToServer() {
        String name = view.getRecipeNameInput().getText().trim();
        String prepText = view.getPrepTimeInput().getText().trim();
        String instructions = view.getInstructionsInput().getText().trim();

        if (name.isEmpty() || prepText.isEmpty() || instructions.isEmpty()) {
            view.getStatusArea().appendText("Error: Please fill in all recipe fields!\n");
            return;
        }

        int prepTime;
        try {
            prepTime = Integer.parseInt(prepText);
        } catch (NumberFormatException e) {
            view.getStatusArea().appendText("Error: Prep Time must be a whole number!\n");
            return;
        }

        view.getStatusArea().appendText("Adding new recipe to database...\n");
        view.getAddRecipeBtn().setDisable(true);

        Task<Boolean> addRecipeTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                // packaging the recipe info into a DTO box
                RecipeDTO newRecipeDTO = new RecipeDTO(null, name, prepTime, instructions);

                Request addReq = new Request(Command.ADD_RECIPE, newRecipeDTO);
                Response response = localClient.sendRequest(addReq);

                localClient.disconnect();
                return response.getStatus() == model.Status.OK;
            }
        };

        addRecipeTask.setOnSucceeded(e -> {
            if (addRecipeTask.getValue()) {
                view.getStatusArea().appendText("Success: New recipe added!\n");
                // clear the input fields
                view.getRecipeNameInput().clear();
                view.getPrepTimeInput().clear();
                view.getInstructionsInput().clear();
                // refresh the recipe list
                loadRecipesFromServer();
            } else {
                view.getStatusArea().appendText("Error: Server refused the recipe.\n");
            }
            view.getAddRecipeBtn().setDisable(false);
        });

        addRecipeTask.setOnFailed(e -> {
            view.getStatusArea().appendText("Error: Could not connect to server!\n");
            view.getAddRecipeBtn().setDisable(false);
        });

        new Thread(addRecipeTask).start();
    }

    // opens a new window when a recipe is double-clicked to view and add ingredients
    private void openRecipeDetailWindow(RecipeDTO recipe) {
        Stage detailStage = new Stage();
        detailStage.setTitle("Manage Recipe: " + recipe.getName());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label titleLabel = new Label("Requirements for " + recipe.getName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // list to show the ingredients we already added
        ListView<String> reqList = new ListView<>();
        reqList.setPrefHeight(100);

        // fetch the existing ingredients right away
        refreshRequirementsList(recipe.getId(), reqList);

        Label addLabel = new Label("Add New Ingredient:");
        addLabel.setStyle("-fx-font-weight: bold;");

        TextField ingNameInput = new TextField();
        ingNameInput.setPromptText("Ingredient Name (e.g. Eggs)");

        TextField ingQtyInput = new TextField();
        ingQtyInput.setPromptText("Required Quantity");

        ComboBox<String> unitInput = new ComboBox<>();
        unitInput.getItems().addAll("g", "ml", "pcs", "kg", "l", "tbsp", "tsp");
        unitInput.setValue("pcs");

        Button addBtn = new Button("Add to Recipe");

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(60);

        addBtn.setOnAction(e -> {
            String name = ingNameInput.getText().trim();
            String qtyText = ingQtyInput.getText().trim();
            String unit = unitInput.getValue();

            if (name.isEmpty() || qtyText.isEmpty() || unit == null) {
                resultArea.setText("Please fill all fields!");
                return;
            }

            try {
                double qty = Double.parseDouble(qtyText);
                addBtn.setDisable(true);

                // sending to server
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        Client client = new Client();
                        client.connect("localhost", 8080);
                        Request req = new Request(Command.ADD_REQUIREMENT, new IngredientRequirementDTO(recipe.getId(), name, qty, unit));
                        Response res = client.sendRequest(req);
                        client.disconnect();

                        // update UI on success safely from the background thread
                        Platform.runLater(() -> {
                            if(res.getStatus() == model.Status.OK) {
                                resultArea.setText("Success: " + res.getMessage());
                                ingNameInput.clear();
                                ingQtyInput.clear();
                                // refresh the list so the user sees the new item!
                                refreshRequirementsList(recipe.getId(), reqList);
                            } else {
                                resultArea.setText("Error: " + res.getMessage());
                            }
                            addBtn.setDisable(false);
                        });
                        return null;
                    }
                };
                new Thread(task).start();

            } catch (NumberFormatException ex) {
                resultArea.setText("Quantity must be a number!");
            }
        });

        layout.getChildren().addAll(titleLabel, reqList, addLabel, ingNameInput, ingQtyInput, unitInput, addBtn, resultArea);
        Scene scene = new Scene(layout, 350, 450);
        detailStage.setScene(scene);
        detailStage.show();
    }

    // helper method to load the ingredients for a specific recipe
    private void refreshRequirementsList(Long recipeId, ListView<String> reqList) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                Client client = new Client();
                client.connect("localhost", 8080);
                Request req = new Request(Command.GET_RECIPE_REQUIREMENTS, recipeId);
                Response res = client.sendRequest(req);
                client.disconnect();

                @SuppressWarnings("unchecked")
                List<String> payload = (List<String>) res.getPayload();
                return payload;
            }
        };

        task.setOnSucceeded(e -> {
            reqList.getItems().clear();
            List<String> reqs = task.getValue();
            if (reqs == null || reqs.isEmpty()) {
                reqList.getItems().add("No ingredients required yet.");
            } else {
                reqList.getItems().addAll(reqs);
            }
        });

        new Thread(task).start();
    }
    private void checkExpiredIngredients() {
        Task<List<IngredientDTO>> task = new Task<>() {
            @Override
            protected List<IngredientDTO> call() throws Exception {
                Client client = new Client();
                client.connect("localhost", 8080);
                Request req = new Request(Command.GET_EXPIRED, null);
                Response res = client.sendRequest(req);
                client.disconnect();
                return (List<IngredientDTO>) res.getPayload();
            }
        };

        task.setOnSucceeded(e -> {
            List<IngredientDTO> expired = task.getValue();
            if (expired.isEmpty()) {
                view.getStatusArea().appendText(">> SUCCESS: No expired items found in pantry!\n");
            } else {
                view.getStatusArea().appendText(">> WARNING: Found " + expired.size() + " expired items!\n");
                // only show what is expired
                view.getIngredientList().getItems().clear();
                view.getIngredientList().getItems().addAll(expired);
            }
        });
        new Thread(task).start();
    }
    // Deletes the selected recipe from the database
    private void deleteRecipeFromServer() {
        RecipeDTO selectedRecipe = view.getRecipeList().getSelectionModel().getSelectedItem();

        if (selectedRecipe == null) {
            view.getStatusArea().appendText("Error: Select a recipe to delete!\n");
            return;
        }

        view.getStatusArea().appendText("Deleting recipe...\n");
        view.getDeleteRecipeBtn().setDisable(true);

        Task<Boolean> deleteTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Client localClient = new Client();
                localClient.connect("localhost", 8080);

                // the dto sent to the server will just be recipe id
                Request delReq = new Request(Command.DELETE_RECIPE, selectedRecipe.getId());
                Response response = localClient.sendRequest(delReq);

                localClient.disconnect();
                return response.getStatus() == model.Status.OK;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            if (deleteTask.getValue()) {
                view.getStatusArea().appendText("Success: Recipe deleted!\n");
                loadRecipesFromServer(); // refresh the list
            } else {
                view.getStatusArea().appendText("Error: Server refused deletion.\n");
            }
            view.getDeleteRecipeBtn().setDisable(false);
        });

        deleteTask.setOnFailed(e -> {
            view.getStatusArea().appendText("Error: Server connection failed.\n");
            view.getDeleteRecipeBtn().setDisable(false);
        });

        new Thread(deleteTask).start();
    }
}
