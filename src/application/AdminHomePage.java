package application;

import databasePart1.DatabaseHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

//adds for background
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;




public class AdminHomePage {

    /**
     * Displays the admin page in the provided primary stage.
     * 
     * @param primaryStage   The primary stage where the scene will be displayed.
     * @param previousScene  The scene to return to when the back button is clicked.
     * @param adminUserName  The username of the currently logged in admin.
     */
    public void show(Stage primaryStage, Scene previousScene, String adminUserName) {
        VBox layout = new VBox(10);
        
        
     // Load background image
        Image backgroundImage = new Image(getClass().getResource("/admin.jpg").toExternalForm());
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true) // Scale image to fit
        );

        // Apply background to VBox
        layout.setBackground(new Background(background));

        
        
        
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        // Welcome label now shows the actual admin username.
        Label adminLabel = new Label("Hello, " + adminUserName + "!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Create TableView to display user data.
        TableView<User> userTable = new TableView<>();
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Define table columns using lambda expressions to avoid reflection.

        // Username column.
        TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUserName()));

        // Role column: displays all roles
        TableColumn<User, String> roleColumn = new TableColumn<>("Roles");
        roleColumn.setCellValueFactory(cellData -> {
            String roles = cellData.getValue().getRole();
            // Format roles for better display (e.g., "Student, Staff" instead of "Student,Staff")
            roles = roles.replace(",", ", ");
            return new SimpleStringProperty(roles);
        });

        // First Name column.
        TableColumn<User, String> firstNameColumn = new TableColumn<>("First Name");
        firstNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getfirstName()));

        // Last Name column.
        TableColumn<User, String> lastNameColumn = new TableColumn<>("Last Name");
        lastNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getlastName()));

        // Email column.
        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getemail()));

        // Add all columns to the table.
        userTable.getColumns().addAll(usernameColumn, roleColumn, firstNameColumn, lastNameColumn, emailColumn);

        // DatabaseHelper instance for database operations.
        DatabaseHelper dbHelper = new DatabaseHelper();

        // Load users into the table.
        Runnable loadUsers = () -> {
            userTable.getItems().clear();
            try {
                dbHelper.connectToDatabase();
                ObservableList<User> users = dbHelper.getAllUsers();
                userTable.setItems(users);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Initially load the users.
        loadUsers.run();

        // Delete button to remove a selected user.
        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(e -> {
            // Get the selected user.
            User selectedUser = userTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a user to delete.");
                alert.showAndWait();
                return;
            }

            // Confirm deletion with the admin.
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, 
                    "Are you sure you want to delete user: " + selectedUser.getUserName() + "?", 
                    ButtonType.YES, ButtonType.NO);
            confirmation.showAndWait();
            if (confirmation.getResult() == ButtonType.YES) {
                boolean success = dbHelper.deleteUser(selectedUser.getUserName());
                if (success) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION, "User deleted successfully.");
                    info.showAndWait();
                    loadUsers.run();  // Refresh the user list.
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to delete the user.");
                    error.showAndWait();
                }
            }
        });
        //go to get the otp
        Button resetPasswordButton = new Button("SET OTP");
        resetPasswordButton.setOnAction(e -> {
            AdminUserReset adminUserReset = new AdminUserReset();
            adminUserReset.show(primaryStage, primaryStage.getScene());
        });
        
   
        

        // Back button to return to the previous scene.
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> primaryStage.setScene(previousScene));
        

        // Add components to the layout.
        layout.getChildren().addAll(adminLabel, userTable, deleteButton, resetPasswordButton, backButton);

        Scene adminScene = new Scene(layout, 800, 400);

        // Set the scene to the primary stage.
        primaryStage.setScene(adminScene);
        primaryStage.setTitle("Admin Page");
    }
}
