module FoundationCode {
	requires javafx.controls;
	requires java.sql;
	requires javafx.graphics;
	requires org.junit.jupiter.api;
	
	opens application to javafx.graphics, javafx.fxml;
}
