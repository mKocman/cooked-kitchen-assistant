package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.IngredientDTO;
import model.RecipeDTO;

public class MainGUI extends Application {

    // core UI elements
    private ListView<IngredientDTO> ingredientList;
    private ListView<RecipeDTO> recipeList;
    private TextArea statusArea;
    private ComboBox<String> modeSelector;

    // pantry mode controls
    private TextField nameInput;
    private TextField quantityInput;
    private ComboBox<String> unitInput;
    private DatePicker expDateInput; // for selecting expiration dates
    private Button addIngBtn;
    private Button deleteIngBtn;
    private Button checkExpiredBtn;

    // recipe mode controls
    private TextField recipeNameInput;
    private TextField prepTimeInput;
    private TextField instructionsInput;
    private Button addRecipeBtn;
    private Button makeRecipeBtn;
    private Button deleteRecipeBtn;

    private Button loadBtn;

    // layout containers for view switching
    private VBox centerPantryBox;
    private VBox centerRecipeBox;
    private VBox rightPantryBox;
    private VBox rightRecipeBox;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // --- top panel: dropdown menu ---
        modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll("Pantry Mode", "Recipe Mode");
        modeSelector.setValue("Pantry Mode");

        VBox topBox = new VBox(5, new Label("Select View Mode:"), modeSelector);
        topBox.setPadding(new Insets(0, 0, 15, 0));
        root.setTop(topBox);

        // --- center panel: smart lists ---
        ingredientList = new ListView<>();
        centerPantryBox = new VBox(5, new Label("Ingredients (Pantry Stock)"), ingredientList);

        recipeList = new ListView<>();
        centerRecipeBox = new VBox(5, new Label("Available Recipes"), recipeList);

        StackPane centerStack = new StackPane(centerRecipeBox, centerPantryBox);
        root.setCenter(centerStack);

        // --- bottom panel: status & alerts ---
        statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefHeight(100);
        root.setBottom(statusArea);

        // --- right panel: dynamic controls ---

        // 1. pantry controls
        nameInput = new TextField();
        nameInput.setPromptText("Ingredient Name");

        quantityInput = new TextField();
        quantityInput.setPromptText("Quantity");

        // date picker setup
        expDateInput = new DatePicker();
        expDateInput.setPromptText("Expiration Date");

        checkExpiredBtn = new Button("Check Expired Items!");
        checkExpiredBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

        // unit dropdown menu setup
        unitInput = new ComboBox<>();
        unitInput.getItems().addAll("g", "ml", "pcs", "kg", "l", "tbsp", "tsp");
        unitInput.setValue("g"); // default value

        addIngBtn = new Button("Add Ingredient");
        deleteIngBtn = new Button("Delete Selected Item");

        rightPantryBox = new VBox(10, new Label("Pantry Tools"), nameInput, quantityInput, unitInput, expDateInput, addIngBtn, deleteIngBtn, new Separator(), checkExpiredBtn);

        // 2. recipe controls
        recipeNameInput = new TextField();
        recipeNameInput.setPromptText("Recipe Name");

        prepTimeInput = new TextField();
        prepTimeInput.setPromptText("Prep Time (min)");

        instructionsInput = new TextField();
        instructionsInput.setPromptText("Instructions");

        addRecipeBtn = new Button("Add New Recipe");

        deleteRecipeBtn = new Button("Delete Selected Recipe");
        deleteRecipeBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");

        makeRecipeBtn = new Button("Make Recipe!");
        makeRecipeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");

        rightRecipeBox = new VBox(10, new Label("Recipe Tools"), recipeNameInput, prepTimeInput, instructionsInput, addRecipeBtn, deleteRecipeBtn, new Separator(), makeRecipeBtn);

        // putting the layouts together in a stack
        StackPane rightStack = new StackPane(rightRecipeBox, rightPantryBox);
        loadBtn = new Button("Load/Refresh Database");

        VBox rightMenu = new VBox(20);
        rightMenu.setPadding(new Insets(0, 0, 0, 15));
        rightMenu.getChildren().addAll(rightStack, new Separator(), loadBtn);
        root.setRight(rightMenu);

        // --- view switching logic ---
        modeSelector.setOnAction(e -> switchMode());
        switchMode();

        Scene scene = new Scene(root, 800, 550);
        primaryStage.setTitle("Smart Recipe Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // initializing the controller
        new controller.MainController(this);
    }

    // handles toggling visibility between pantry and recipe modes
    private void switchMode() {
        boolean isPantry = modeSelector.getValue().equals("Pantry Mode");

        centerPantryBox.setVisible(isPantry);
        rightPantryBox.setVisible(isPantry);

        centerRecipeBox.setVisible(!isPantry);
        rightRecipeBox.setVisible(!isPantry);
    }

    public static void main(String[] args) {
        launch(args);
    }

    // getters for the controller to access UI components
    public ListView<IngredientDTO> getIngredientList() { return ingredientList; }
    public ListView<RecipeDTO> getRecipeList() { return recipeList; }
    public TextArea getStatusArea() { return statusArea; }

    public TextField getNameInput() { return nameInput; }
    public TextField getQuantityInput() { return quantityInput; }
    public ComboBox<String> getUnitInput() { return unitInput; }
    public DatePicker getExpDateInput() { return expDateInput; }
    public Button getAddIngBtn() { return addIngBtn; }
    public Button getDeleteIngBtn() { return deleteIngBtn; }
    public Button getCheckExpiredBtn() { return checkExpiredBtn; }

    public TextField getRecipeNameInput() { return recipeNameInput; }
    public TextField getPrepTimeInput() { return prepTimeInput; }
    public TextField getInstructionsInput() { return instructionsInput; }
    public Button getAddRecipeBtn() { return addRecipeBtn; }
    public Button getDeleteRecipeBtn() { return deleteRecipeBtn; }
    public Button getMakeRecipeBtn() { return makeRecipeBtn; }

    public Button getLoadBtn() { return loadBtn; }
}