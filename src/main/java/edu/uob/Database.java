package edu.uob;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private String storagePath;
    private Map<String,Table> tablesMap;
    private String name;

    public Database(String name, String storageFolderPath) {
        this.name = name.toLowerCase();
        this.storagePath = storageFolderPath + File.separator + this.name;
        this.tablesMap = new HashMap<String,Table>();
    }

    public String getName() {return this.name;}
    public String getStoragePath() {return this.storagePath;}
    public Map<String,Table> getTables() {return new HashMap<>(this.tablesMap);}

    public Table getTable(String tableName) throws DBException {
        Table table = this.tablesMap.get(tableName.toLowerCase());
        if (table == null) {
            throw new DBException("Table: " + tableName + " not found in database: " + this.name);
        }
        return table;
    }



    //========================
    // Database file Storage
    //========================

    public void createDatabaseFolder() throws DBException {
        File databaseFolder = new File(storagePath);
        if (databaseFolder.exists()) {
            throw new DBException("Database" + this.name + " already exists");
        }
        try {
            databaseFolder.mkdir();
        } catch (SecurityException e) {
            throw new DBException("Database" + this.name + " could not be created");
        }
    }

    public void deleteDatabaseFolder() throws DBException {
        File databaseFolder = new File(storagePath);
        if (!databaseFolder.exists()) {
            throw new DBException("Database" + this.name + " does not exist");
        }
        File[] tableFiles = databaseFolder.listFiles();
        for (File tableFile : tableFiles) {
            tableFile.delete();
        }
        databaseFolder.delete();
    }

    public void loadTables() throws DBException {
        File databaseFolder = new File(storagePath);
        File[] tableFiles = databaseFolder.listFiles();
        if (tableFiles == null) {return;}
        for (File tableFile : tableFiles) {
            String tableName = tableFile.getName().replace(".tab","");
            Table table = new Table(tableName, this.getStoragePath());
            table.loadFromFile();
            this.tablesMap.put(tableName, table);
        }
    }

    public void dropTable(String tableName) throws DBException { //delete table folder + remove table from tablesMap
        tableName = tableName.toLowerCase();
        Table table = this.tablesMap.remove(tableName);
        if (table == null) {
            throw new DBException("Table " + tableName + " does not exist");
        }
        File tableFile = new File(table.getFilePath());
        tableFile.delete();
    }

    public void addTable(Table table) throws DBException { //add table to tablesMap + save to file
        table.saveToFile();
        tablesMap.put(table.getName(), table);
    }
}
