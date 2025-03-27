package edu.uob;


public class AlterCommand extends DBCommand{ //add or drop columns to a table

    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        String tableName = tableNames.get(0);
        Table table = dbServer.getCurrentDatabase().getTable(tableName);

        String columnName = columnNames.get(0);
        switch (commandType) {
            case "add": table.addColumn(columnName); break; //handle the actual column name
            case "drop": table.dropColumn(columnName); break; //handle the actual column name
            default: throw new DBException("Unknown command type in AlterCommand");
        }
        table.saveToFile();
        return "[OK]";
    }
}
