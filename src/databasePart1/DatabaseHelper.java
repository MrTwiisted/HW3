package databasePart1;

import java.sql.*;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import application.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import application.Question;
import application.Answer;

/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

    // JDBC driver name and database URL 
    static final String JDBC_DRIVER = "org.h2.Driver";   
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

    // Database credentials 
    static final String USER = "sa"; 
    static final String PASS = ""; 

    private Connection connection = null;
    private Statement statement = null; 
 
    /**
     * Connects to the database and creates the necessary tables.
     */
    public void connectToDatabase() throws SQLException {
        try {
            // Load the JDBC driver
            Class.forName(JDBC_DRIVER); 
            System.out.println("Connecting to database...");
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement(); 
            // If you want to reset database just uncomment the line below
            //statement.execute("DROP ALL OBJECTS");

            createTables();  // Create the necessary tables if they don't exist
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
        }
    }

    /**
     * This method ensures that the database connection is open.
     * If the connection is null or closed, it will attempt to reconnect.
     */
    public void ensureConnected() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Reconnecting to database...");
            connectToDatabase();
        }
    }

    /**
     * Creates the necessary tables if they do not exist.
     */
    private void createTables() throws SQLException {
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "role VARCHAR(255), "
                + "firstName VARCHAR(255), "
                + "lastName VARCHAR(255), "
                + "email VARCHAR(255))";
        statement.execute(userTable);
        
        // Create the invitation codes table
        String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
                + "code VARCHAR(10) PRIMARY KEY, "
                + "role VARCHAR(255), "
                + "isUsed BOOLEAN DEFAULT FALSE)";
        statement.execute(invitationCodesTable);

        // Create Questions table
        String questionsTable = "CREATE TABLE IF NOT EXISTS Questions ("
                + "questionID INT PRIMARY KEY, "
                + "bodyText TEXT, "
                + "postedBy VARCHAR(255), "
                + "dateCreated TIMESTAMP, "
                + "resolvedStatus BOOLEAN DEFAULT FALSE, "
                + "acceptedAnsID INT DEFAULT -1, "
                + "newMessagesCount INT DEFAULT 0)";
        statement.execute(questionsTable);

        // Create Answers table with consistent column naming
        String answersTable = "CREATE TABLE IF NOT EXISTS Answers ("
                + "answerID INT PRIMARY KEY, "
                + "questionID INT, "
                + "bodyText TEXT, "
                + "answeredBy VARCHAR(255), "
                + "dateCreated TIMESTAMP, "
                + "FOREIGN KEY (questionID) REFERENCES Questions(questionID))";
        statement.execute(answersTable);
        
        //Create a table to maintain feedback
        String feedbackTable = "CREATE TABLE IF NOT EXISTS Feedback ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "questionID INT, "
                + "sentTo VARCHAR(255), "
                + "sentBy VARCHAR(255), "
                + "feedbackText TEXT, "
                + "parentID INT DEFAULT NULL, "
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (questionID) REFERENCES Questions(questionID) ON DELETE CASCADE, "
                + "FOREIGN KEY (parentID) REFERENCES Feedback(id) ON DELETE CASCADE"
                + ")";
        statement.execute(feedbackTable);

        
    }


    /**
     * Checks if the database is empty.
     */
    public boolean isDatabaseEmpty() throws SQLException {
        // Ensure connection is open before querying
        ensureConnected();
        String query = "SELECT COUNT(*) AS count FROM cse360users";
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getInt("count") == 0;
        }
        return true;
    }

    /**
     * Registers a new user in the database.
     */
    public void register(User user) throws SQLException {
        // Ensure connection is open before executing any operation
        ensureConnected();
        String insertUser = "INSERT INTO cse360users (userName, password, role, firstName, lastName, email) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getfirstName()); // or getFirstName() if renamed
            pstmt.setString(5, user.getlastName());  // or getLastName() if renamed
            pstmt.setString(6, user.getemail());     // or getEmail() if renamed
            pstmt.executeUpdate();
        }
    }


    /**
     * Validates a user's login credentials.
     */
    public boolean login(User user) throws SQLException {
        // Ensure connection is open before executing any operation
        ensureConnected();
        
        // Modified query to only check username and password
        String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Get the stored role from the database
                    String storedRole = rs.getString("role");
                    // If the user has the role they're trying to use (either exact match or as part of multiple roles)
                    return storedRole.equals(user.getRole()) || 
                           storedRole.contains(user.getRole());
                }
                return false;
            }
        }
    }
    
    /**
     * Checks if a user already exists in the database based on their userName.
     */
    public boolean doesUserExist(String userName) {
        try {
            // Ensure connection is open before executing any operation
            ensureConnected();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        
        String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // If the count is greater than 0, the user exists
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // If an error occurs, assume user doesn't exist
    }
    
    /**
     * Retrieves the role of a user from the database using their userName.
     */
    public String getUserRole(String userName) {
        try {
            // Ensure connection is open before executing any operation
            ensureConnected();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return null;
        }
        
        String query = "SELECT role FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role"); // Return the role if user exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // If no user exists or an error occurs
    }
    
    /**
     * Generates a new invitation code with associated role and inserts it into the database.
     */
    public String generateInvitationCodeWithRole(String role) {
        try {
            ensureConnected();
            
            String code = UUID.randomUUID().toString().substring(0, 4);
            String query = "INSERT INTO InvitationCodes (code, role) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, code);
                pstmt.setString(2, role);
                pstmt.executeUpdate();
                return code;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Validates an invitation code and returns the associated role if valid.
     */
    public String validateInvitationCodeAndGetRole(String code) {
        try {
            ensureConnected();
            
            String query = "SELECT role FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("role");
                    markInvitationCodeAsUsed(code);
                    return role;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Marks the invitation code as used in the database.
     */
    private void markInvitationCodeAsUsed(String code) {
        try {
            // Ensure connection is open before executing any operation
            ensureConnected();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return;
        }
        
        String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieves all users from the database.
     * @return An ObservableList of User objects.
     * @throws SQLException if a database access error occurs.
     */
    public ObservableList<User> getAllUsers() throws SQLException {
        // Ensure we are connected to the database.
        ensureConnected();
        ObservableList<User> userList = FXCollections.observableArrayList();
        // Retrieve userName, role, firstName, lastName, and email.
        String query = "SELECT userName, role, firstName, lastName, email FROM cse360users";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String userName = rs.getString("userName");
                String role = rs.getString("role");
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                String email = rs.getString("email");
                // Create a User object. We use an empty string for the password.
                User user = new User(userName, "", role, firstName, lastName, email);
                userList.add(user);
            }
        }
        return userList;
    }

    
    /**
     * Deletes a user from the database based on their userName.
     * <p>
     * Note: This method will not delete an admin user.
     * </p>
     * @param userName The username of the user to be deleted.
     * @return true if a user was deleted, false otherwise.
     */
    public boolean deleteUser(String userName) {
        try {
            ensureConnected();
            // Prevent deletion if the user is an admin
            String role = getUserRole(userName);
            if (role != null && role.equalsIgnoreCase("admin")) {
                System.out.println("Cannot delete an admin user.");
                return false;
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        
        String query = "DELETE FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Updates the users password.
     */
    public void updatePassword(String username, String newPassword) throws SQLException {
        ensureConnected();
        String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }
    /**
     * Updates the users password.
     */
    public boolean isOTPValid(String username, String otp) throws SQLException {
        ensureConnected();
        String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, otp);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;  // Returns true if OTP matches the stored password
            }
        }
        return false; // OTP is invalid
    }
    
    /**
     * Closes the database connection and statement.
     */
    public void closeConnection() {
        try { 
            if (statement != null) {
                statement.close(); 
            }
        } catch (SQLException se2) { 
            se2.printStackTrace();
        } 
        try { 
            if (connection != null) {
                connection.close(); 
            }
        } catch (SQLException se) { 
            se.printStackTrace(); 
        } 
    }

    public String getUserFirstName(String userName) throws SQLException {
        ensureConnected();
        String query = "SELECT firstName FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("firstName");
            }
        }
        return "";
    }

    public String getUserLastName(String userName) throws SQLException {
        ensureConnected();
        String query = "SELECT lastName FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("lastName");
            }
        }
        return "";
    }

    public String getUserEmail(String userName) throws SQLException {
        ensureConnected();
        String query = "SELECT email FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        }
        return "";
    }

    //================================================================================
    // Question and Answer Related Methods
    //================================================================================
    
    /**
     * Inserts a new question into the database.
     */
    public void insertQuestion(Question question) throws SQLException {
        ensureConnected();
        String query = "INSERT INTO Questions (questionID, bodyText, postedBy, dateCreated, "
                    + "resolvedStatus, acceptedAnsID, newMessagesCount) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, question.getQuestionID());
            pstmt.setString(2, question.getBodyText());
            pstmt.setString(3, question.getPostedBy());
            pstmt.setTimestamp(4, new Timestamp(question.getDateCreated().getTime()));
            pstmt.setBoolean(5, question.isResolved());
            pstmt.setInt(6, question.getAcceptedAnsID());
            pstmt.setInt(7, question.getNewMessagesCount());
            pstmt.executeUpdate();
        }
    }

    /**
     * Updates an existing question in the database.
     */
    public void updateQuestion(Question question) throws SQLException {
        ensureConnected();
        String query = "UPDATE Questions SET bodyText = ?, postedBy = ?, dateCreated = ?, "
                    + "resolvedStatus = ?, acceptedAnsID = ?, newMessagesCount = ? "
                    + "WHERE questionID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, question.getBodyText());
            pstmt.setString(2, question.getPostedBy());
            pstmt.setTimestamp(3, new Timestamp(question.getDateCreated().getTime()));
            pstmt.setBoolean(4, question.isResolved());
            pstmt.setInt(5, question.getAcceptedAnsID());
            pstmt.setInt(6, question.getNewMessagesCount());
            pstmt.setInt(7, question.getQuestionID());
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a question from the database.
     */
    public void deleteQuestion(int questionID) throws SQLException {
        ensureConnected();
        // First delete all associated answers
        String deleteAnswers = "DELETE FROM Answers WHERE questionID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteAnswers)) {
            pstmt.setInt(1, questionID);
            pstmt.executeUpdate();
        }
        
        // Then delete the question
        String deleteQuestion = "DELETE FROM Questions WHERE questionID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteQuestion)) {
            pstmt.setInt(1, questionID);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all questions from the database.
     */
    public List<Question> getAllQuestions() throws SQLException {
        ensureConnected();
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM Questions";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Question q = new Question(
                    rs.getInt("questionID"),
                    rs.getString("bodyText"),
                    rs.getString("postedBy"),
                    rs.getTimestamp("dateCreated")
                );
                q.setResolved(rs.getBoolean("resolvedStatus"));
                q.setAcceptedAnsID(rs.getInt("acceptedAnsID"));
                q.setNewMessagesCount(rs.getInt("newMessagesCount"));
                questions.add(q);
            }
        }
        return questions;
    }

    /**
     * Inserts a new answer into the database.
     */
    public void insertAnswer(Answer answer) throws SQLException {
        ensureConnected();
        String query = "INSERT INTO Answers (answerID, questionID, bodyText, answeredBy, dateCreated) "
                    + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answer.getAnsID());
            pstmt.setInt(2, answer.getQuestionID());
            pstmt.setString(3, answer.getBodyText());
            pstmt.setString(4, answer.getAnsweredBy());
            pstmt.setTimestamp(5, new Timestamp(answer.getDateCreated().getTime()));
            pstmt.executeUpdate();
        }
    }

    /**
     * Updates an existing answer in the database.
     */
    public void updateAnswer(Answer answer) throws SQLException {
        ensureConnected();
        String query = "UPDATE Answers SET bodyText = ?, answeredBy = ?, dateCreated = ? "
                    + "WHERE answerID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, answer.getBodyText());
            pstmt.setString(2, answer.getAnsweredBy());
            pstmt.setTimestamp(3, new Timestamp(answer.getDateCreated().getTime()));
            pstmt.setInt(4, answer.getAnsID());
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes an answer from the database.
     */
    public void deleteAnswer(int answerID) throws SQLException {
        ensureConnected();
        String query = "DELETE FROM Answers WHERE answerID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerID);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all answers for a specific question.
     */
    public List<Answer> getAnswersForQuestion(int questionID) throws SQLException {
        ensureConnected();
        List<Answer> answers = new ArrayList<>();
        String query = "SELECT * FROM Answers WHERE questionID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Answer a = new Answer(
                        rs.getInt("answerID"),
                        rs.getInt("questionID"),
                        rs.getString("bodyText"),
                        rs.getString("answeredBy"),
                        rs.getTimestamp("dateCreated")
                    );
                    answers.add(a);
                }
            }
        }
        return answers;
    }

    /**
     * Retrieves all answers from the database.
     */
    public List<Answer> getAllAnswers() throws SQLException {
        ensureConnected();
        List<Answer> answers = new ArrayList<>();
        String query = "SELECT * FROM Answers";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Answer a = new Answer(
                    rs.getInt("answerID"),
                    rs.getInt("questionID"),
                    rs.getString("bodyText"),
                    rs.getString("answeredBy"),
                    rs.getTimestamp("dateCreated")
                );
                answers.add(a);
            }
        }
        return answers;
    }
    /**
     * Inserts feedback entry into the database for a specific question.
     */
    public void insertFeedback(int questionID, String sentTo, String sentBy, String feedbackText) throws SQLException {
        ensureConnected();
        String query = "INSERT INTO Feedback (questionID, sentTo, sentBy, feedbackText) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionID);
            pstmt.setString(2, sentTo);
            pstmt.setString(3, sentBy);
            pstmt.setString(4, feedbackText);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all feedback and replies for a specific user.
     */
    public List<String[]> getFeedbackForUser(String username) throws SQLException {
        ensureConnected();
        List<String[]> feedbackList = new ArrayList<>();

        String query = "SELECT f.id, f.feedbackText, f.sentBy, q.bodyText, f.questionID, f.parentID, f.timestamp, "
                     + "CASE WHEN f.parentID IS NULL THEN 'Feedback' ELSE 'Reply' END AS type "
                     + "FROM Feedback f "
                     + "JOIN Questions q ON f.questionID = q.questionID "
                     + "WHERE f.sentTo = ? "
                     + "ORDER BY f.id DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String feedbackID = String.valueOf(rs.getInt("id"));
                String questionID = String.valueOf(rs.getInt("questionID"));
                String questionText = rs.getString("bodyText");  
                String feedbackText = rs.getString("feedbackText");
                String sentBy = rs.getString("sentBy");
                String dateTime = rs.getTimestamp("timestamp").toString();
                String type = rs.getString("type");

                feedbackList.add(new String[]{type, questionID, feedbackID, questionText, feedbackText, sentBy, dateTime});
            }
        }
        return feedbackList;
    }
    
    /**
     * Inserts a reply to an existing feedback entry.
     */
    public void insertReply(int parentID, String sentTo, String sentBy, String replyText) throws SQLException {
        ensureConnected();
        
        String getQuestionQuery = "SELECT questionID FROM Feedback WHERE id = ?";
        int questionID = -1;

        try (PreparedStatement pstmt = connection.prepareStatement(getQuestionQuery)) {
            pstmt.setInt(1, parentID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                questionID = rs.getInt("questionID");
            }
        }

        if (questionID == -1) {
            throw new SQLException("Error: Unable to retrieve questionID for reply.");
        }

        String insertReplyQuery = "INSERT INTO Feedback (parentID, questionID, sentTo, sentBy, feedbackText) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertReplyQuery)) {
            pstmt.setInt(1, parentID);
            pstmt.setInt(2, questionID);
            pstmt.setString(3, sentTo);
            pstmt.setString(4, sentBy);
            pstmt.setString(5, replyText);
            pstmt.executeUpdate();
        }
    }
}