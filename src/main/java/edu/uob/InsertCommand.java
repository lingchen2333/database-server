package edu.uob;

public class InsertCommand extends DBCommand{ //insert values to a table

    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        String tableName = tableNames.get(0);
        Table table = dbServer.getCurrentDatabase().getTable(tableName);
        table.insertRow(values);
        table.saveToFile();
        return "[OK]";
    }
}
