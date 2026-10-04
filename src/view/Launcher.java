package view;

public class Launcher {
    static void main(String[] args) {
        // separate launcher class to work around JavaFX module restrictions when starting the GUI
        MainGUI.main(args);
    }
}