package edu.uob;

import java.util.Map;

public class UseCommand extends DBCommand {
    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        Map<String, Database> databaseMap = dbServer.loadDatabases();
        if (!databaseMap.containsKey(databaseName)) {
            throw new DBException("Database " + databaseName + " not found");
        }
        dbServer.setCurrentDatabase(databaseMap.get(databaseName));
        return "[OK]";
    }
}
