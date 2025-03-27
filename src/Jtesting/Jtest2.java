package Jtesting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import application.Question;
import application.Answer;
import application.Questions;
import application.Answers;
import databasePart1.DatabaseHelper;

/**
 * JUnit test class for testing database-related operations 
 * such as adding, modifying, and deleting questions and answers.
 */
public class Jtest2 {
    private DatabaseHelper dbHelper;
    private Questions questions;
    private Answers answers;

    /**
     * Default constructor for Jtest2.
     * Initializes the test class for JUnit execution.
     */
    public Jtest2() {
        // Default constructor
    }

    /**
     * Sets up the database connection and initializes objects before each test.
     * 
     * @throws SQLException if a database connection error occurs.
     */
    @BeforeEach
    public void setUp() throws SQLException {
        dbHelper = new DatabaseHelper();
        dbHelper.connectToDatabase();
        questions = new Questions();
        answers = new Answers();
    }

    /**
     * Closes the database connection after each test.
     */
    @AfterEach
    public void tearDown() {
        dbHelper.closeConnection();
    }

    /**
     * Test to delete questions
     * 
     * @throws SQLException if a database access error occurs.
     */
    @Test
    @DisplayName("Delete question from database")
    public void testDeleteQuestion() throws SQLException {
        int newId = getNewQuestionId();
        Question question = new Question(newId, "Why are the cars slow?", "Emma", new Date());
        dbHelper.insertQuestion(question);
        dbHelper.deleteQuestion(question.getQuestionID());
        List<Question> questions = dbHelper.getAllQuestions();
        assertFalse(questions.stream().anyMatch(q -> q.getQuestionID() == question.getQuestionID()));
    }

    /**
     * Updates the question ids so that there aren't random blank slots in the database
     * 
     * @return the next available question ID.
     * @throws SQLException if a database access error occurs.
     */
    private int getNewQuestionId() throws SQLException {
        List<Question> allQuestions = dbHelper.getAllQuestions();
        return allQuestions.stream()
            .mapToInt(Question::getQuestionID)
            .max()
            .orElse(0) + 1;
    }

    /**
     * Test modify existing answers
     */
    @Test
    @DisplayName("Modify answer")
    public void testModifyAnswer() {
        Answer answer = new Answer(1, 1, "A healthy diet should include a balance of proteins, fats, and carbohydrates.", "Alan", new Date());
        answers.insertAnswer(answer);
        answer.setBodyText("Updated: A well-balanced diet should include lean proteins, healthy fats, and complex carbohydrates.");
        answers.modifyAnswer(answer);
        assertEquals("Updated: A well-balanced diet should include lean proteins, healthy fats, and complex carbohydrates.", answers.findAnswerByID(1).getBodyText());
    }

    /**
     * Test to see if the question field was left blank
     */
    @Test
    @DisplayName("Empty question should be invalid")
    public void testQuestionValidationEmptyContent() {
        Question question = new Question(1, "", "John", new Date());
        assertFalse(question.checkValidity());
    }

    /**
     * Test to see you user can add and retrieve a question
     */
    @Test
    @DisplayName("Add and retrieve question")
    public void testAddQuestion() {
        String uniqueContent = "Is the sky blue?" + System.currentTimeMillis();
        Question question = new Question(0, uniqueContent, "", new Date());
        questions.insertQuestion(question);

        List<Question> allQuestions = questions.listAllQuestions();
        boolean found = allQuestions.stream().anyMatch(q -> q.getBodyText().equals(uniqueContent));
        assertTrue(found);
    }

    /**
     * Test to see if the user can delete answers
     */
    @Test
    @DisplayName("Delete answer")
    public void testDeleteAnswer() {
        Answer answer = new Answer(1, 1, "The sky is red.", "Emily", new Date());
        answers.insertAnswer(answer);
        answers.deleteAnswer(answer.getAnsID());
        assertFalse(answers.listAllAnswers().contains(answer));
    }
}
