package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This page displays a simple welcome message for the user.
 */
public class InstructorHomePage {

    /**
     * Displays the user page in the provided primary stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     * @param user The user object containing user information.
     */
    public void show(Stage primaryStage, User user) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        // Load background image
        Image backgroundImage = new Image(getClass().getResource("/instructor.jpg").toExternalForm());
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true) // Scale image to fit
        );

        // Apply background to VBox
        layout.setBackground(new Background(background));
        
        
        
        
        // Label to display Hello user with their name
        Label userLabel = new Label("Hello, " + user.getfirstName() + "! (Instructor)");
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Back button to return to the role selection
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new SelectRole().show(primaryStage, user, user.getRole()));

        layout.getChildren().addAll(userLabel, backButton);
        Scene userScene = new Scene(layout, 800, 400);

        // Set the scene to primary stage
        primaryStage.setScene(userScene);
        primaryStage.setTitle("Instructor Page");
    }
}
