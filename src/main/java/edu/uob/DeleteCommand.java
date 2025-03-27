package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class DeleteCommand extends DBCommand { //delete rows from a table
    public String queryServer(DBServer dbServer) throws DBException {

        Table table = dbServer.getCurrentDatabase().getTable(tableNames.get(0));
        List<List<String>> rows = table.getRows();
        List<String> filteredRowsIds = new ArrayList<>();

        condition.setTable(table);
        for (List<String> row : rows) {
            if (condition.evaluate(table.getColumnNames(), row)) {
                filteredRowsIds.add(row.get(0));
            }
        }

        for (String id : filteredRowsIds) {table.deleteRow(id);}
        table.saveToFile();
        return "[OK]";
    }
}