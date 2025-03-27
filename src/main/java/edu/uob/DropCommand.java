package edu.uob;

import java.util.Map;

public class DropCommand extends DBCommand{

    @Override
    public String queryServer(DBServer dbServer) throws DBException {  

        if (commandType.equals("drop database")) {
            Map<String,Database> databaseMap = dbServer.loadDatabases();
            Database database = databaseMap.remove(databaseName);
            if (database == null) {throw new DBException("Database" +databaseName + "does not exist");}
            
            database.deleteDatabaseFolder();
            
            if (dbServer.getCurrentDatabase().getName().equals(databaseName)) {
                dbServer.setCurrentDatabase(null);
            }
            return "[OK]";
        } else { //drop table
            String tableName = tableNames.get(0);
            dbServer.getCurrentDatabase().dropTable(tableName);
            return "[OK]";
        }
    }
}
