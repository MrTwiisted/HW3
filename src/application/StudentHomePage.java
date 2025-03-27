package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import databasePart1.DatabaseHelper;

/**
 * This page handles the student interface for viewing and managing questions and answers.
 * Students can ask questions, provide answers, and track their interactions.
 */
public class StudentHomePage {
    private DatabaseHelper dbHelper;

    /**
     * Initializes the StudentHomePage with a database connection.
     */
    public StudentHomePage() {
        this.dbHelper = new DatabaseHelper();
        try {
            dbHelper.connectToDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays the student page in the provided primary stage.
     * 
     * @param primaryStage The primary stage where the scene will be displayed.
     * @param user The user object containing student information.
     */
    public void show(Stage primaryStage, User user) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        
        // Load background image
        Image backgroundImage = new Image(getClass().getResource("/student.jpg").toExternalForm());
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true) // Scale image to fit
        );

        // Apply background to VBox
        layout.setBackground(new Background(background));
        
        
        
        
        // Welcome label
        Label userLabel = new Label("Hello, " + user.getfirstName() + "! (Student)");
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Create TableView for questions
        TableView<Question> questionTable = new TableView<>();
        questionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create TableView for answers
        TableView<Answer> answerTable = new TableView<>();
        answerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Define table columns
        TableColumn<Question, String> idColumn = new TableColumn<>("Question ID");
        idColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getQuestionID())));

        TableColumn<Question, String> bodyColumn = new TableColumn<>("Question");
        bodyColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBodyText()));

        TableColumn<Question, String> postedByColumn = new TableColumn<>("Posted By");
        postedByColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPostedBy()));

        TableColumn<Question, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDateCreated().toString()));

        TableColumn<Question, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isResolved() ? "Resolved" : "Unresolved"));

        TableColumn<Question, String> unreadColumn = new TableColumn<>("Unread Answers");
        unreadColumn.setCellValueFactory(cellData -> {
            try {
                // Get total number of answers for this question
                List<Answer> answers = dbHelper.getAnswersForQuestion(cellData.getValue().getQuestionID());
                int unreadCount = cellData.getValue().getNewMessagesCount();
                return new SimpleStringProperty(unreadCount > 0 ? String.valueOf(unreadCount) : "");
            } catch (SQLException e) {
                e.printStackTrace();
                return new SimpleStringProperty("");
            }
        });

        questionTable.getColumns().addAll(idColumn, bodyColumn, postedByColumn, dateColumn, 
                                        statusColumn, unreadColumn);

        // Initial load of questions
        refreshQuestionTable(questionTable);

        // Buttons for question management
        Button askQuestionButton = new Button("Ask Question");
        Button viewAnswersButton = new Button("Answer");
        Button searchQuestion = new Button("Search Questions");
        Button updateQuestionButton = new Button("Update Question");
        Button deleteButton = new Button("Delete");
        Button feedbackButton = new Button("Feedback");
        Button inboxButton = new Button("Inbox");
        
        Button replyChainButton = new Button ("Reply");

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center;");
        buttonBox.getChildren().addAll(askQuestionButton, viewAnswersButton, searchQuestion, updateQuestionButton, deleteButton, feedbackButton, inboxButton, replyChainButton);

        // Ask Question button action
        askQuestionButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Ask Question");
            dialog.setHeaderText("Enter your question:");
            dialog.setContentText("Question:");

            dialog.showAndWait().ifPresent(questionText -> {
                try {
                    // Get the current max question ID from the database
                    List<Question> allQuestions = dbHelper.getAllQuestions();
                    int newId = 1;
                    if (!allQuestions.isEmpty()) {
                        newId = allQuestions.stream()
                                          .mapToInt(Question::getQuestionID)
                                          .max()
                                          .getAsInt() + 1;
                    }

                    Question newQuestion = new Question(
                        newId,
                        questionText,
                        user.getUserName(),
                        new Date()
                    );

                    dbHelper.insertQuestion(newQuestion);
                    refreshQuestionTable(questionTable);
                } catch (SQLException ex) {
                    showAlert("Error saving question: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });
        });

        // View Answers button action
        viewAnswersButton.setOnAction(e -> {
            Question selectedQuestion = questionTable.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                try {
                    // Reset unread count when viewing answers
                    if (selectedQuestion.getNewMessagesCount() > 0) {
                        selectedQuestion.setNewMessagesCount(0);
                        dbHelper.updateQuestion(selectedQuestion);
                        refreshQuestionTable(questionTable);
                    }
                    showAnswersDialog(selectedQuestion, user, questionTable);
                } catch (SQLException ex) {
                    showAlert("Error updating unread count: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Please select a question first.", Alert.AlertType.WARNING);
            }
        });

        // Search question button action
        searchQuestion.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Search Questions");
            dialog.setHeaderText("Enter search keyword:");
            dialog.setContentText("Keyword:");

            dialog.showAndWait().ifPresent(keyword -> {
                try {
                    List<Question> allQuestions = dbHelper.getAllQuestions();
                    List<Question> filteredQuestions = allQuestions.stream()
                        .filter(q -> q.getBodyText().toLowerCase().contains(keyword.toLowerCase()))
                        .toList();
                    questionTable.setItems(FXCollections.observableArrayList(filteredQuestions));
                } catch (SQLException ex) {
                    showAlert("Error searching questions: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });
        });

        // Update Question button action
        updateQuestionButton.setOnAction(e -> {
            Question selectedQuestion = questionTable.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                if (selectedQuestion.getPostedBy().equals(user.getUserName())) {
                    TextInputDialog updateDialog = new TextInputDialog(selectedQuestion.getBodyText());
                    updateDialog.setTitle("Update Question");
                    updateDialog.setHeaderText("Update your question:");
                    updateDialog.setContentText("Question:");

                    updateDialog.showAndWait().ifPresent(updatedText -> {
                        try {
                            selectedQuestion.setBodyText(updatedText);
                            dbHelper.updateQuestion(selectedQuestion);
                            refreshQuestionTable(questionTable);
                        } catch (SQLException ex) {
                            showAlert("Error updating question: " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                } else {
                    showAlert("You can only update your own questions.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Please select a question to update.", Alert.AlertType.WARNING);
            }
        });

        // Delete button action
        deleteButton.setOnAction(e -> {
            Question selectedQuestion = questionTable.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                if (selectedQuestion.getPostedBy().equals(user.getUserName())) {
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete this question?",
                            ButtonType.YES, ButtonType.NO);
                    confirmation.showAndWait();
                    
                    if (confirmation.getResult() == ButtonType.YES) {
                        try {
                            dbHelper.deleteQuestion(selectedQuestion.getQuestionID());
                            refreshQuestionTable(questionTable);
                        } catch (SQLException ex) {
                            showAlert("Error deleting question: " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                } else {
                    showAlert("You can only delete your own questions.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Please select a question to delete.", Alert.AlertType.WARNING);
            }
        });

        feedbackButton.setOnAction(e -> {
            Question selectedQuestion = questionTable.getSelectionModel().getSelectedItem();
            
            if (selectedQuestion != null) {
                // Get the username of the person who posted the selected question
                String questionOwner = selectedQuestion.getPostedBy();

                // Prevent the question owner from giving feedback to themselves
                if (user.getUserName().equals(questionOwner)) {
                    showAlert("You cannot give feedback on your own question.", Alert.AlertType.WARNING);
                    return;
                }

                // Open a text input dialog for feedback
                TextInputDialog feedbackDialog = new TextInputDialog();
                feedbackDialog.setTitle("Give Feedback");
                feedbackDialog.setHeaderText("Provide feedback for the question:\n" + selectedQuestion.getBodyText());
                feedbackDialog.setContentText("Enter your feedback:");

                feedbackDialog.showAndWait().ifPresent(feedbackText -> {
                    if (!feedbackText.trim().isEmpty()) {
                        try {
                            // Insert feedback into the database with question ID
                            dbHelper.insertFeedback(selectedQuestion.getQuestionID(), questionOwner, user.getUserName(), feedbackText);
                            showAlert("Feedback sent successfully!", Alert.AlertType.INFORMATION);
                        } catch (SQLException ex) {
                            showAlert("Error saving feedback: " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    } else {
                        showAlert("Feedback cannot be empty!", Alert.AlertType.WARNING);
                    }
                });

            } else {
                showAlert("Please select a question first to give feedback.", Alert.AlertType.WARNING);
            }
        });

        // Button to open the Inbox
        inboxButton.setOnAction(e -> {
            try {
                List<String[]> feedbackList = dbHelper.getFeedbackForUser(user.getUserName());

                if (feedbackList.isEmpty()) {
                    showAlert("Your inbox is empty.", Alert.AlertType.INFORMATION);
                    return;
                }

                // Create a window for inbox
                Stage inboxStage = new Stage();
                inboxStage.setTitle("Inbox - Received Feedback");

                // Table
                TableView<String[]> inboxTable = new TableView<>();

                // Type
                TableColumn<String[], String> typeColumn = new TableColumn<>("Type");
                typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[0]));
                typeColumn.setPrefWidth(100);

                // Question ID
                TableColumn<String[], String> questionIDColumn = new TableColumn<>("Question ID");
                questionIDColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[1]));
                questionIDColumn.setPrefWidth(100);

                // Question
                TableColumn<String[], String> questionColumn = new TableColumn<>("Question");
                questionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[3]));  
                questionColumn.setPrefWidth(300);

                // Feedback
                TableColumn<String[], String> feedbackColumn = new TableColumn<>("Feedback/Reply");
                feedbackColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[4]));
                feedbackColumn.setPrefWidth(400);

                // From
                TableColumn<String[], String> fromColumn = new TableColumn<>("From");
                fromColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[5]));
                fromColumn.setPrefWidth(200);

                // Date-Time
                TableColumn<String[], String> dateTimeColumn = new TableColumn<>("Date-Time");
                dateTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[6]));
                dateTimeColumn.setPrefWidth(200);

                // Reply Button
                TableColumn<String[], Void> replyColumn = new TableColumn<>("Reply");
                replyColumn.setCellFactory(param -> new TableCell<>() {
                    private final Button replyButton = new Button("Reply");

                    {
                        replyButton.setOnAction(event -> {
                            String[] selectedFeedback = getTableView().getItems().get(getIndex());

                            // Only allow replies for Feedback (not Replies)
                            if (selectedFeedback[0].equals("Feedback")) {
                                // Open dialog to enter reply
                                TextInputDialog replyDialog = new TextInputDialog();
                                replyDialog.setTitle("Reply to Feedback");
                                replyDialog.setHeaderText("Replying to feedback from " + selectedFeedback[5]);
                                replyDialog.setContentText("Enter your reply:");

                                replyDialog.showAndWait().ifPresent(replyText -> {
                                    if (!replyText.trim().isEmpty()) {
                                        try {
                                            int parentID = Integer.parseInt(selectedFeedback[2]);  // Get feedback ID
                                            String sentTo = selectedFeedback[5];  // The original sender of the feedback

                                            // insertReply function to store reply
                                            dbHelper.insertReply(parentID, sentTo, user.getUserName(), replyText);
                                            showAlert("Reply sent successfully!", Alert.AlertType.INFORMATION);
                                        } catch (SQLException ex) {
                                            showAlert("Error saving reply: " + ex.getMessage(), Alert.AlertType.ERROR);
                                        }
                                    } else {
                                        showAlert("Reply cannot be empty!", Alert.AlertType.WARNING);
                                    }
                                });
                            } else {
                                showAlert("You can only reply to Feedback, not a Reply.", Alert.AlertType.WARNING);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getTableView().getItems().get(getIndex())[0].equals("Reply")) {
                            setGraphic(null);
                        } else {
                            setGraphic(replyButton);
                        }
                    }
                });

                inboxTable.getColumns().setAll(typeColumn, questionIDColumn, questionColumn, feedbackColumn, fromColumn, dateTimeColumn, replyColumn);

                inboxTable.getItems().addAll(feedbackList);

                VBox newLayout = new VBox(10, inboxTable);
                newLayout.setStyle("-fx-padding: 20;");
                Scene scene = new Scene(newLayout, 1400, 500);

                inboxStage.setScene(scene);
                inboxStage.show();

            } catch (SQLException ex) {
                showAlert("Error fetching feedback: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });



        // Back button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new SelectRole().show(primaryStage, user, user.getRole()));
        
        replyChainButton.setOnAction(e -> {
            Question selectedQuestion = questionTable.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                openChatWindow(selectedQuestion); // Pass selectedQuestion to open the chat window
            } else {
                showAlert("Please select a question to open chat.", Alert.AlertType.WARNING);
            }
        });
        

        // Add all components to layout
        layout.getChildren().addAll(userLabel, questionTable, buttonBox, backButton);

        Scene userScene = new Scene(layout, 800, 400);
        primaryStage.setScene(userScene);
        primaryStage.setTitle("Student Page");
    }

    /**
     * Displays a dialog showing answers for a specific question.
     * 
     * @param question The question whose answers are being displayed.
     * @param user The current user viewing the answers.
     * @param questionTable The main question table to refresh after updates.
     */
    private void showAnswersDialog(Question question, User user, TableView<Question> questionTable) {
        Stage dialogStage = new Stage();
        VBox dialogLayout = new VBox(10);
        dialogLayout.setStyle("-fx-padding: 20;");

        // Question status label
        Label statusLabel = new Label("Status: " + (question.isResolved() ? "Resolved" : "Unresolved"));
        statusLabel.setStyle("-fx-font-weight: bold;");

        HBox statusBox = new HBox(10);
        statusBox.setStyle("-fx-alignment: center;");
        statusBox.getChildren().addAll(statusLabel);

        // Answer table setup
        TableView<Answer> answerTable = new TableView<>();
        
        // First column: Answer
        TableColumn<Answer, String> answerColumn = new TableColumn<>("Answer");
        answerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBodyText()));
        answerColumn.setPrefWidth(300);

        // Second column: Answered By
        TableColumn<Answer, String> answeredByColumn = new TableColumn<>("Answered By");
        answeredByColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAnsweredBy()));
        answeredByColumn.setPrefWidth(150);

        // Third column: Date
        TableColumn<Answer, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDateCreated().toString()));
        dateColumn.setPrefWidth(150);

        // Fourth column: Accepted Answer
        TableColumn<Answer, String> acceptedColumn = new TableColumn<>("Status");
        acceptedColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getAnsID() == question.getAcceptedAnsID() ? 
                "Accepted" : ""));
        acceptedColumn.setPrefWidth(100);

        answerTable.getColumns().addAll(answerColumn, answeredByColumn, dateColumn, acceptedColumn);
        
        try {
            List<Answer> allAnswers = dbHelper.getAnswersForQuestion(question.getQuestionID());
            answerTable.setItems(FXCollections.observableArrayList(allAnswers));
        } catch (SQLException ex) {
            showAlert("Error loading answers: " + ex.getMessage(), Alert.AlertType.ERROR);
        }

        // Button container for answer management
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center;");

        Button addAnswerButton = new Button("Add Answer");
        Button updateAnswerButton = new Button("Update Answer");
        Button deleteAnswerButton = new Button("Delete Answer");
        Button acceptAnswerButton = new Button("Accept Answer");
        Button searchAnswerButton = new Button("Search Answer");

        // Only show accept button if user is question owner and question is not resolved
        acceptAnswerButton.setVisible(question.getPostedBy().equals(user.getUserName()) && !question.isResolved());

        buttonBox.getChildren().addAll(addAnswerButton, updateAnswerButton, deleteAnswerButton, acceptAnswerButton, searchAnswerButton);

        // Accept Answer button action
        acceptAnswerButton.setOnAction(e -> {
            Answer selectedAnswer = answerTable.getSelectionModel().getSelectedItem();
            if (selectedAnswer != null) {
                try {
                    // Update question with accepted answer ID and resolved status
                    question.setAcceptedAnsID(selectedAnswer.getAnsID());
                    question.setResolved(true);
                    dbHelper.updateQuestion(question);

                    // Update UI
                    statusLabel.setText("Status: Resolved");
                    refreshQuestionTable(questionTable);

                    // Refresh answer table to show accepted status
                    answerTable.setItems(FXCollections.observableArrayList(
                        dbHelper.getAnswersForQuestion(question.getQuestionID())
                    ));

                    showAlert("Answer accepted and question marked as resolved.", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    showAlert("Error accepting answer: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Please select an answer to accept.", Alert.AlertType.WARNING);
            }
        });

        // Modify Add Answer button action
        addAnswerButton.setOnAction(e -> {
            TextInputDialog answerDialog = new TextInputDialog();
            answerDialog.setTitle("Add Answer");
            answerDialog.setHeaderText("Enter your answer:");
            answerDialog.setContentText("Answer:");

            answerDialog.showAndWait().ifPresent(answerText -> {
                try {
                    // Get all answers from the database to find the max ID
                    List<Answer> allAnswers = dbHelper.getAllAnswers();  // You'll need to add this method to DatabaseHelper
                    int newId = 1;
                    if (!allAnswers.isEmpty()) {
                        newId = allAnswers.stream()
                                        .mapToInt(Answer::getAnsID)
                                        .max()
                                        .getAsInt() + 1;
                    }

                    Answer newAnswer = new Answer(
                        newId,
                        question.getQuestionID(),
                        answerText,
                        user.getUserName(),
                        new Date()
                    );

                    dbHelper.insertAnswer(newAnswer);

                    // Update unread count for the question owner if it's not their own answer
                    if (!user.getUserName().equals(question.getPostedBy())) {
                        question.setNewMessagesCount(question.getNewMessagesCount() + 1);
                        dbHelper.updateQuestion(question);
                    }

                    // Refresh both the answer table and the main question table
                    answerTable.setItems(FXCollections.observableArrayList(
                        dbHelper.getAnswersForQuestion(question.getQuestionID())
                    ));
                    refreshQuestionTable(questionTable);

                } catch (SQLException ex) {
                    showAlert("Error saving answer: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });
        });

        // Update Answer button action
        updateAnswerButton.setOnAction(e -> {
            Answer selectedAnswer = answerTable.getSelectionModel().getSelectedItem();
            if (selectedAnswer != null) {
                if (selectedAnswer.getAnsweredBy().equals(user.getUserName())) {
                    TextInputDialog updateDialog = new TextInputDialog(selectedAnswer.getBodyText());
                    updateDialog.setTitle("Update Answer");
                    updateDialog.setHeaderText("Update your answer:");
                    updateDialog.setContentText("Answer:");

                    updateDialog.showAndWait().ifPresent(updatedText -> {
                        try {
                            Answer updatedAnswer = new Answer(
                                selectedAnswer.getAnsID(),
                                selectedAnswer.getQuestionID(),
                                updatedText,
                                selectedAnswer.getAnsweredBy(),
                                new Date()  // Update the timestamp
                            );
                            dbHelper.updateAnswer(updatedAnswer);
                            answerTable.setItems(FXCollections.observableArrayList(
                                dbHelper.getAnswersForQuestion(question.getQuestionID())
                            ));
                        } catch (SQLException ex) {
                            showAlert("Error updating answer: " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                } else {
                    showAlert("You can only update your own answers.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Please select an answer to update.", Alert.AlertType.WARNING);
            }
        });

        // Delete Answer button action
        deleteAnswerButton.setOnAction(e -> {
            Answer selectedAnswer = answerTable.getSelectionModel().getSelectedItem();
            if (selectedAnswer != null) {
                if (selectedAnswer.getAnsweredBy().equals(user.getUserName())) {
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete this answer?",
                            ButtonType.YES, ButtonType.NO);
                    confirmation.showAndWait();

                    if (confirmation.getResult() == ButtonType.YES) {
                        try {
                            dbHelper.deleteAnswer(selectedAnswer.getAnsID());
                            answerTable.setItems(FXCollections.observableArrayList(
                                dbHelper.getAnswersForQuestion(question.getQuestionID())
                            ));
                        } catch (SQLException ex) {
                            showAlert("Error deleting answer: " + ex.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                } else {
                    showAlert("You can only delete your own answers.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Please select an answer to delete.", Alert.AlertType.WARNING);
            }
        });

        // Search Answer button action
        searchAnswerButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Search Answer");
            dialog.setHeaderText("Enter search keyword:");
            dialog.setContentText("Keyword:");

            dialog.showAndWait().ifPresent(keyword -> {
                try {
                    List<Answer> allAnswers = dbHelper.getAnswersForQuestion(question.getQuestionID());
                    List<Answer> filteredAnswers = allAnswers.stream()
                        .filter(answer -> answer.getBodyText().toLowerCase().contains(keyword.toLowerCase()))
                        .toList();
                    answerTable.setItems(FXCollections.observableArrayList(filteredAnswers));
                } catch (SQLException ex) {
                    showAlert("Error searching answers: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });
        });

        dialogLayout.getChildren().addAll(statusBox, answerTable, buttonBox);
        dialogStage.setScene(new Scene(dialogLayout, 600, 400));
        dialogStage.setTitle("Answers for Question: " + question.getBodyText());
        dialogStage.show();
    }

    
    private String chatHistory = ""; // Store chat messages as one string
    public void openChatWindow(Question selectedQuestion) {
        // Create a new Stage (window)
        Stage chatWindow = new Stage();
        chatWindow.setTitle("Chat");
        // Create a VBox to hold the components
        VBox chatBox = new VBox(10);
        chatBox.setPadding(new Insets(10));
        // Text area for displaying chat messages
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false); // The chat area should only display messages, not be editable
        chatArea.setPrefHeight(300);
        chatArea.setText(chatHistory); // Load previous messages
        // Text field for typing new messages
        TextField messageField = new TextField();
        messageField.setPromptText("Type your message...");
        // Button to send the message
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                String formattedMessage = "You: " + message + "\n";
               
                // Append message to both the chat area and history
                chatArea.appendText(formattedMessage);
                chatHistory += formattedMessage; // Save message for future openings
                // Clear the message field
                messageField.clear();
            }
        });
        // Add the components to the chat box
        chatBox.getChildren().addAll(chatArea, messageField, sendButton);
        // Create a scene and set it on the chat window
        Scene chatScene = new Scene(chatBox, 400, 400);
        chatWindow.setScene(chatScene);
        // Show the chat window
        chatWindow.show();
    }

    /**
     * Refreshes the question table with the latest data from the database.
     * 
     * @param table The TableView to be refreshed with updated question data.
     */
    private void refreshQuestionTable(TableView<Question> table) {
        try {
            List<Question> questions = dbHelper.getAllQuestions();
            table.setItems(FXCollections.observableArrayList(questions));
        } catch (SQLException ex) {
            showAlert("Error refreshing questions: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Displays an alert dialog with the specified message and type.
     * 
     * @param message The message to display in the alert.
     * @param alertType The type of alert to show.
     */
    private void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType, message);
        alert.showAndWait();
    }
}