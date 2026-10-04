package model;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    // server port
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // we create our main database handlers ONCE and share them with all clients
        IngredientHandler ingredientHandler = new IngredientHandler();
        RecipeHandler recipeHandler = new RecipeHandler();

        // initializing database tables before starting the server
        ingredientHandler.createTable();
        recipeHandler.createTable();
        recipeHandler.createRecipeIngredientsTable();

        System.out.println("SERVER: starting up on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SERVER: waiting for clients to connect...");

            // accept clients until the server is stopped
            while (true) {
                // code stops here and waits until a client connects
                Socket clientSocket = serverSocket.accept();
                System.out.println("SERVER: a new client connected!");

                // create a handler for this specific client
                ClientHandler handler = new ClientHandler(clientSocket, ingredientHandler, recipeHandler);

                // handle the client in a separate thread
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.out.println("SERVER ERROR: " + e.getMessage());
        }
    }
}