package application;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.DatabaseHelper;

public class SelectRole {
    
    public void show(Stage primaryStage, User user, String roleString) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        // Load background image
        Image backgroundImage = new Image(getClass().getResource("/role.jpg").toExternalForm());
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true) // Scale image to fit
        );

        // Apply background to VBox
        layout.setBackground(new Background(background));
        
        // Create label
        Label selectLabel = new Label("Select your role:");
        selectLabel.setStyle("-fx-font-size: 14px;");
        
        // Create ComboBox for role selection
        ComboBox<String> roleComboBox = new ComboBox<>();
        
        // Split the roleString and add each role to the ComboBox
        String[] roles = roleString.split(",");
        for (String role : roles) {
            roleComboBox.getItems().add(role.trim());
        }
        
        // Select first role by default
        if (roles.length > 0) {
            roleComboBox.setValue(roles[0].trim());
        }
        
        // Continue button
        Button continueButton = new Button("Continue");
        continueButton.setOnAction(e -> {
            String selectedRole = roleComboBox.getValue();
            if (selectedRole != null) {
                // Create a temporary user with the selected role
                User tempUser = new User(
                    user.getUserName(),
                    user.getPassword(),
                    selectedRole,
                    user.getfirstName(),
                    user.getlastName(),
                    user.getemail()
                );
                
                switch(selectedRole.trim()) {
                    case "Student":
                        new StudentHomePage().show(primaryStage, tempUser);
                        break;
                    case "Staff":
                        new StaffHomePage().show(primaryStage, tempUser);
                        break;
                    case "Reviewer":
                        new ReviewerHomePage().show(primaryStage, tempUser);
                        break;
                    case "Instructor":
                        new InstructorHomePage().show(primaryStage, tempUser);
                        break;
                    default:
                        System.out.println("Unknown role: " + selectedRole);
                        break;
                }
            }
        });
        
        // Back button to return to login page
        Button backButton = new Button("Back");
        DatabaseHelper dbHelper = new DatabaseHelper();
        backButton.setOnAction(e -> new WelcomeLoginPage(dbHelper).show(primaryStage, user));
        
        layout.getChildren().addAll(selectLabel, roleComboBox, continueButton, backButton);
        
        Scene roleScene = new Scene(layout, 400, 300);
        primaryStage.setScene(roleScene);
        primaryStage.setTitle("Select Your Role");
    }
}
