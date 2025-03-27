package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class DBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test data, then queries it.
    // It then checks the response to see that a couple of the entries in the table are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"), "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"), "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n"," ").trim();
        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the entry)
        String lastToken = tokens[tokens.length-1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was " + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Simon"), "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }


    // Test to make sure that the [ERROR] tag is returned in the case of an error (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        assertTrue(response.contains("[ERROR]"), "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"), "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }


    //==========================================
    @Test
    public void testAlterCommand() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();
        String response;

        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");

        sendCommandToServer("CREATE TABLE " + tableName + " (name, age, birthCity);");     
        response = sendCommandToServer("Select * from " + tableName + " ;");
        assertTrue(response.contains("birthCity"), "Failed preserve the case of the column name");

        // Test ALTER TABLE ADD COLUMN
        response = sendCommandToServer("ALTER TABLE " + tableName + " ADD email;");
        assertTrue(response.contains("[OK]"), "Failed to add column");

        response = sendCommandToServer("Select * from " + tableName + " ;");
        assertTrue(response.contains("email"), "Failed to add column");

        response = sendCommandToServer("Alter table " + tableName + " drop NAme;");
        assertTrue(response.contains("[OK]"), "Failed to drop column");
        assertFalse(response.contains("name"), "Failed to drop column");

        // Test server restart after alter table
        server = new DBServer();
        sendCommandToServer("USE " + dbName + ";");
        response = sendCommandToServer("Select * from " + tableName + " ;");
        assertTrue(response.contains("email"), "Failed to add column");
        assertFalse(response.contains("name"), "Failed to drop column");

        // Test ALTER TABLE DROP COLUMN
        response = sendCommandToServer("ALTER TABLE " + tableName + " DROP email;");
        assertTrue(response.contains("[OK]"), "Failed to drop column");
        response = sendCommandToServer("Select * from " + tableName + " ;");
        assertFalse(response.contains("email"), "Failed to drop column");

        // Test DROP TABLE
        response = sendCommandToServer("DROP TABLE " + tableName + ";");
        assertTrue(response.contains("[OK]"), "Failed to drop table");
    }

    @Test
    public void testDropCommand() {
        String dbName1 = generateRandomName();
        String dbName2 = generateRandomName();
        String tableName1 = generateRandomName();
        String tableName2 = generateRandomName();
        
        sendCommandToServer("CREATE DATABASE " + dbName1 + ";");
        sendCommandToServer("CREATE DATABASE " + dbName2 + ";");
        sendCommandToServer("USE " + dbName1 + ";");
        sendCommandToServer("CREATE TABLE " + tableName1 + " (name, city);");
        sendCommandToServer("CREATE TABLE " + tableName2 + " (name, age);");

        String response = sendCommandToServer("DROP TABLE " + tableName1 + ";");
        assertTrue(response.contains("[OK]"), "Failed to drop table");
        
        response = sendCommandToServer("DROP TABLE " + tableName1 + ";");
        assertTrue(response.contains("[ERROR]"), "Should not allow dropping a table that does not exist");

        response = sendCommandToServer("DROP DATABASE " + dbName2 + ";");
        assertTrue(response.contains("[OK]"), "Failed to drop database");

        server = new DBServer();
        sendCommandToServer("USE " + dbName1 + ";");
        response = sendCommandToServer("SELECT * FROM " + tableName1 + ";");
        assertTrue(response.contains("[ERROR]"), "Dropped table should not exist after server restart");

        response = sendCommandToServer("Select * from " + tableName2 + ";");
        assertTrue(response.contains("[OK]"), "table should exist after server restart");

        response = sendCommandToServer("Use " + dbName2 + ";");
        assertTrue(response.contains("[ERROR]"), "Dropped database should not exist after server restart");

        response = sendCommandToServer("drop table " + tableName2 + ";");   
        assertTrue(response.contains("[OK]"), "Failed to drop table after server restart");

        response = sendCommandToServer("drop database " + dbName1 + ";");
        assertTrue(response.contains("[OK]"), "Failed to drop database after server restart");
    }

    @Test
    public void testInsertCommand() {
        String dbName = generateRandomName();
        String tableName1 = generateRandomName();
        String tableName2 = generateRandomName();
        String response;

        // Setup and insert data
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName1 + " (name, age, city);");
        sendCommandToServer("INSERT INTO " + tableName1 + " values ('John', +25.0, 'London');");
        response = sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('Alice', 30, paris);");
        assertTrue(response.contains("[ERROR]"), "paris should not be a valid value");

        sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('Alice', 30, 'paris');");

        response = sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('Bob', 35);");
        assertTrue(response.contains("[ERROR]"), "Should handle invalid number of values");

        response = sendCommandToServer("INSERT INTO " + tableName2 + " VALUES ('Bob', 35, 'New York');");
        assertTrue(response.contains("[ERROR]"), "should not allow inserting into a table that does not exist");
        
        // Create new server instance (simulating restart)
        server = new DBServer();

        // Verify data persistence
        sendCommandToServer("USE " + dbName + ";");
        response = sendCommandToServer("SELECT * FROM " + tableName1 + ";");
        assertTrue(response.contains("John"), "Data not persisted after server restart");
        assertTrue(response.contains("Alice"), "Data not persisted after server restart");
    }

    @Test
    public void testSelectCommand() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();
        String response;

        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName + " (name, Age, City, working);");

        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London', True);");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Alice', 30, 'Paris',false);");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Bob', 35, 'New York',true);");

        response = sendCommandToServer("SELECT age FROM " + tableName + ";");
        assertTrue(response.contains("[OK]"), "Failed to select data");
        assertTrue(response.contains("25"), "Failed to select data");
        assertTrue(response.contains("30"), "Failed to select data");
        assertTrue(response.contains("35"), "Failed to select data");

        response = sendCommandToServer("SELECT age FROM " + tableName + " where Name == 'John';");
        assertTrue(response.contains("25"), "Failed to select data with condition containing case insensitive column name");  

        response = sendCommandToServer("select * from " + tableName + " where name != 'John';");
        assertTrue(response.contains("[OK]"), "Failed to select data with != condition");
        assertFalse(response.contains("John"), "Failed to select data with != condition");

        response = sendCommandToServer("select * from " + tableName + " where age>25;");
        assertTrue(response.contains("[OK]"), "Failed to select data with > condition");
        assertTrue(response.contains("30"), "Failed to select data with > condition");
        assertTrue(response.contains("35"), "Failed to select data with > condition");
        assertFalse(response.contains("25"), "Failed to select data with > condition");

        response = sendCommandToServer("select * from " + tableName + " where working == true;");
        assertTrue(response.contains("[OK]"), "Failed to select data with == condition containing case insensitive boolean value");
        assertTrue(response.contains("John"), "Failed to select data with == condition containing case insensitive boolean value");
        assertTrue(response.contains("Bob"), "Failed to select data with == condition containing case insensitive boolean value");
        assertFalse(response.contains("Alice"), "Failed to select data with == condition containing case insensitive boolean value");
        response = sendCommandToServer("select * from " + tableName + " where age like 3;"); 
        assertTrue(response.contains("[OK]"), "Failed to select data with like condition");
        assertTrue(response.contains("35"), "Failed to select data with like condition");
        assertTrue(response.contains("30"), "Failed to select data with like condition");
        assertFalse(response.contains("25"), "Failed to select data with like condition");

        response = sendCommandToServer("select * from " + tableName + " where name>1;");
        assertTrue(response.contains("[OK]"), "Server should not throw error when comparing string with number");
    }

    @Test
    public void testUpdateCommand() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();
        String response;

        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName + " (name, Age, City);");

        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London');");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Alice', 30, 'Paris');");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Bob', 35, 'New York');");

        response = sendCommandToServer("UPDATE " + tableName + " SET city = 'Paris' WHERE Name == 'John';");
        assertTrue(response.contains("[OK]"), "Failed to update data");

        response = sendCommandToServer("SELECT * FROM " + tableName + " where name == 'John';");
        assertTrue(response.contains("Paris"), "Failed to update data");

        //restart server
        server = new DBServer();
        sendCommandToServer("USE " + dbName + ";");
        response = sendCommandToServer("SELECT * FROM " + tableName + " where name == 'John';");
        assertTrue(response.contains("Paris"), "Failed to update data");


        response = sendCommandToServer("UPDATE " + tableName + " SET city = 'Bristol', age = 26 WHERE Name == 'John' and id == 1;");
        assertTrue(response.contains("[OK]"), "Failed to update data for compound condition");
        response = sendCommandToServer("SELECT * FROM " + tableName + " where name == 'John';");
        assertTrue(response.contains("Bristol"), "Failed to update data for compound and condition");
        assertTrue(response.contains("26"), "Failed to update data for compound and condition");
        
        response = sendCommandToServer("UPDATE " + tableName + " SET city = 'Leicester', age = 20 WHERE Name == 'John' or id == 2 or id ==1 or salary == 50000;");
        assertTrue(response.contains("[OK]"), "Failed to update data for compound or condition with invalid column name");

        response = sendCommandToServer("SELECT * FROM " + tableName + " where name == 'John';");
        assertTrue(response.contains("Leicester"), "Failed to update data for compound or condition");
        assertTrue(response.contains("20"), "Failed to update data for compound or condition");
        response = sendCommandToServer("SELECT * FROM " + tableName + " where id == 2;");
        assertTrue(response.contains("Leicester"), "Failed to update data for compound or condition");
        assertTrue(response.contains("20"), "Failed to update data for compound or condition");      
    }

    @Test
    public void testDeleteCommand() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();
        String tableName2 = generateRandomName();
        String response;

        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName + " (name, Age, City);");

        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London');");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Alice', 30, 'Paris');");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Bob', 35, 'New York');");

        response = sendCommandToServer("DELETE FROM " + tableName + " WHERE name=='John';");
        assertTrue(response.contains("[OK]"), "Failed to delete data");

        response = sendCommandToServer("SELECT * FROM " + tableName + ";");
        assertFalse(response.contains("John"), "Failed to delete data");
        
        response = sendCommandToServer("DELETE FROM " + tableName + " WHERE name == 'John';");
        assertTrue(response.contains("[OK]"), "Do not delete data when there is no match");

        response = sendCommandToServer("DELETE FROM " + tableName2 + " WHERE name=='John';");
        assertTrue(response.contains("[ERROR]"), "Do not delete data when table does not exist");

        //restart server
        server = new DBServer();
        sendCommandToServer("USE " + dbName + ";");
        response = sendCommandToServer("SELECT * FROM " + tableName + ";");
        assertFalse(response.contains("John"), "Failed to delete data after server restart");

        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London');");
        response = sendCommandToServer("DELETE FROM " + tableName + " WHERE (Name=='John') or (age==35);");
        assertTrue(response.contains("[OK]"), "Failed to delete data for compound or condition");
        response = sendCommandToServer("SELECT * FROM " + tableName + ";");
        assertFalse(response.contains("John"), "Failed to delete data for compound or condition");
        assertFalse(response.contains("35"), "Failed to delete data for compound or condition");

    }

    @Test
    public void testJoinCommand() {
        String dbName = generateRandomName();
        String tableName1 = generateRandomName();
        String tableName2 = generateRandomName();
        String response;
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName1 + " (name, age, city);");
        sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('John', 25, 'London');");
        sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('Alice', 30, 'Paris');");
        sendCommandToServer("INSERT INTO " + tableName1 + " VALUES ('David', 35, 'New York');");

        sendCommandToServer("CREATE TABLE " + tableName2 + " (userId, mark, pass);");
        sendCommandToServer("INSERT INTO " + tableName2 + " VALUES (3, 65, true);");
        sendCommandToServer("INSERT INTO " + tableName2 + " VALUES (2, 70, TRUE);");
        sendCommandToServer("INSERT INTO " + tableName2 + " VALUES (1, 50, FAlSE);");
        sendCommandToServer("INSERT INTO " + tableName2 + " VALUES (3, 80, TRUE);");

        response = sendCommandToServer("join " + tableName1 + " and " + tableName2 + " ON " + "id and Userid;");
        assertTrue(response.contains("[OK]"), "Failed to join tables");
        assertTrue(response.contains("Paris"), "Failed to join tables");
        assertTrue(response.contains("London"), "Failed to join tables");
        assertTrue(response.contains("New York"), "Failed to join tables");
        assertTrue(response.contains("80"), "Failed to join tables on multiple matches");
        //restart server
        server = new DBServer();

        sendCommandToServer("USE " + dbName + ";");
        //invalid column name
        response = sendCommandToServer("join " + tableName1 + " and " + tableName2 + " ON " + "NAME and job;");
        assertTrue(response.contains("[ERROR]"), "Table 2 does not have a column called job");

        response = sendCommandToServer("join " + tableName1 + " and " + tableName2 + " ON " + "id and Userid;");
        assertTrue(response.contains("[OK]"), "Failed to join tables");
        assertTrue(response.contains("Paris"), "Failed to join tables");
        assertTrue(response.contains("London"), "Failed to join tables");
        assertTrue(response.contains("80"), "Failed to join tables");
        assertTrue(response.contains("65"), "Failed to join tables");
    }


    @Test
    public void testDataManipulation() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();

        // Setup
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName + " (name, age, city);");

        // Test INSERT
        String response = sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London');");
        assertTrue(response.contains("[OK]"), "Failed to insert data");

        // Test SELECT
        response = sendCommandToServer("SELECT * FROM " + tableName + ";");
        assertTrue(response.contains("[OK]"), "Failed to select data");
        assertTrue(response.contains("John"), "Inserted data not found in select result");

        // Test SELECT with WHERE
        response = sendCommandToServer("SELECT * FROM " + tableName + " WHERE age == 25;");
        assertTrue(response.contains("[OK]"), "Failed to select with condition");
        assertTrue(response.contains("John"), "Conditional select failed");

        // Test UPDATE
        response = sendCommandToServer("UPDATE " + tableName + " SET city = 'Paris' WHERE name == 'John';");
        assertTrue(response.contains("[OK]"), "Failed to update data");

        // Verify UPDATE
        response = sendCommandToServer("SELECT * FROM " + tableName + " WHERE name == 'John';");
        assertTrue(response.contains("Paris"), "Update not reflected in data");

        // Test DELETE
        response = sendCommandToServer("DELETE FROM " + tableName + " WHERE name == 'John';");
        assertTrue(response.contains("[OK]"), "Failed to delete data");

        // Verify DELETE
        response = sendCommandToServer("SELECT * FROM " + tableName + " WHERE name == 'John';");
        assertFalse(response.contains("John"), "Deleted data still present");
    }

    @Test
    public void testComplexQueries() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();

        // Setup
        sendCommandToServer("CREATE database " + dbName + "    ;");
        sendCommandToServer("USE " + dbName + ";");
        sendCommandToServer("CREATE TABLE " + tableName + " (name, age, city, salary);");

        // Insert test data
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John', 25, 'London', 50000);");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Alice', 30, 'Paris', 60000);");
        sendCommandToServer("INSERT INTO " + tableName + " VALUES ('Bob', 35, 'New York', 70000);");

        // Test complex SELECT with multiple conditions
        sendCommandToServer("select name, salary from " + tableName + " where age > 25;");
        sendCommandToServer("select name, salary from " + tableName + " where city != 'London';");

        String response = sendCommandToServer("SELECT name, salary FROM " + tableName + " WHERE age > 25 AND city != 'London';");

        assertTrue(response.contains("[OK]"), "Failed to execute complex select");
        assertTrue(response.contains("Alice"), "Complex select missing expected result");
        assertTrue(response.contains("Bob"), "Complex select missing expected result");

        // Test UPDATE with multiple columns
        response = sendCommandToServer("UPDATE " + tableName + " SET salary = 65000, city = 'Berlin' WHERE name == 'Alice';");
        assertTrue(response.contains("[OK]"), "Failed to update multiple columns");

        // Verify multiple column update
        response = sendCommandToServer("SELECT * FROM " + tableName + " WHERE name == 'Alice';");
        assertTrue(response.contains("65000"), "Salary update not reflected");
        assertTrue(response.contains("Berlin"), "City update not reflected");
    }

    @Test
    public void testErrorHandling() {
        String dbName = generateRandomName();
        String tableName = generateRandomName();

        // Test invalid SQL syntax
        String response = sendCommandToServer("CREATE DATABASE");  // Missing semicolon
        assertTrue(response.contains("[ERROR]"), "Should handle invalid SQL syntax");

        // Test accessing non-existent database
        response = sendCommandToServer("USE nonexistent;");
        assertTrue(response.contains("[ERROR]"), "Should handle non-existent database");

        // Setup for table tests
        sendCommandToServer("CREATE DATABASE " + dbName + ";");
        sendCommandToServer("USE " + dbName + ";");

        // Test accessing non-existent table
        response = sendCommandToServer("SELECT * FROM nonexistent;");
        assertTrue(response.contains("[ERROR]"), "Should handle non-existent table");

        // Test invalid column names
        sendCommandToServer("CREATE TABLE " + tableName + " (name, age);");
        response = sendCommandToServer("INSERT INTO " + tableName + " VALUES ('John');");  // Missing value
        assertTrue(response.contains("[ERROR]"), "Should handle invalid number of values");

        // Test invalid data types in conditions
        response = sendCommandToServer("SELECT * FROM " + tableName + " WHERE age == 'not_a_number';");
        assertTrue(response.contains("[OK]"), "Should handle invalid data type in condition");
    }

   

    @Test
    public void testInvalidCommand(){
        String randomName = generateRandomName();
        String response = sendCommandToServer("CREATE DATABASE " + randomName);
        assertTrue(response.contains("[ERROR]"), "Failed to detect missing semicolon");

        response = sendCommandToServer("CREATE DATABASE []");
        assertTrue(response.contains("[ERROR]"), "Failed to detect invalid plaintext");

        response = sendCommandToServer("CreATE DataBase     " + randomName + ";");
        assertTrue(response.contains("[OK]"), "Failed to allow case insensitive keywords");

        response = sendCommandToServer("USE      " + randomName + "    ;");
        assertTrue(response.contains("[OK]"), "Failed to allow whitespace");       
    }

}