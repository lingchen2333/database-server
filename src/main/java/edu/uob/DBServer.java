package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

//This class implements the DB server. 
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private Database currentDatabase;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    // This method handles all incoming DB commands and carries out the required actions.
    public String handleCommand(String command) {
        try {
            Tokeniser tokeniser = new Tokeniser(command);
            Parser parser = new Parser(tokeniser);
            ICommand cmd = parser.parseCommand();
            return cmd.queryServer(this);
        } catch (DBCommandException e) {
            return "[ERROR]: Invalid command. " + e.getMessage();
        } catch (DBException e) {
            return "[ERROR]: Invalid database operation: " + e.getMessage();
        } catch (Exception e) {
            return "[ERROR]: An unexpected error occurred: " + e.getMessage();
        }
    }

    public String getStorageFolderPath() {return this.storageFolderPath;}

    public void setCurrentDatabase(Database database) {this.currentDatabase = database;}

    public Database getCurrentDatabase() throws DBException {
        if (this.currentDatabase == null) {throw new DBException("No database selected"); }
        return this.currentDatabase;
    }

    //load databases from storage folder
    public Map<String, Database> loadDatabases() throws DBException {
        Map<String, Database> databases = new HashMap<>();
        File storageFolder = new File(storageFolderPath);
        File[] databaseFolders = storageFolder.listFiles();
        if (databaseFolders == null) {return databases;}
        for (File databaseFolder : databaseFolders) {
            String databaseName = databaseFolder.getName();
            Database database = new Database(databaseName,storageFolderPath);
            database.loadTables();
            databases.put(databaseName, database);
        }
        return databases;
    }

    //  Methods below handle networking aspects of the project 
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
