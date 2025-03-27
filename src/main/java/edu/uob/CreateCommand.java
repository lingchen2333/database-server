package edu.uob;

import java.util.Map;

public class CreateCommand extends DBCommand{

    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        
        if (commandType.equals("create database")) {
            Map<String, Database> databaseMap = dbServer.loadDatabases();
            if (databaseMap.containsKey(databaseName)) {throw new DBException("Database " + databaseName + " already exists");}
            Database newDatabase = new Database(databaseName, dbServer.getStorageFolderPath());
            newDatabase.createDatabaseFolder();
            return "[OK]";
        } else { //create table
            String tableName = tableNames.get(0);
            Database database = dbServer.getCurrentDatabase();

            if (database.getTables().containsKey(tableName)) {
                throw new DBException("Table " + tableName + " already exists in current database " + databaseName);
            }
            Table newTable = new Table(tableName, database.getStoragePath());
            if (columnNames.size() > 0) {
                for (String columnName : columnNames) {
                    newTable.addColumn(columnName);
                }
            }
            database.addTable(newTable);
            return "[OK]";
        }
    }
}
