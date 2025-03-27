package edu.uob;

import java.util.List;
import java.util.ArrayList;

public class UpdateCommand extends DBCommand { //update table
    private List<NameValuePair> nameValueList;

    public void setNameValueList(List<NameValuePair> nameValueList) {
        this.nameValueList = nameValueList;
    }

    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        String tableName = tableNames.get(0);
        Table table = dbServer.getCurrentDatabase().getTable(tableName);
        List<String> allColumnNames = table.getColumnNames();

        List<List<String>> rows = table.getRows();
        List<String> filteredRowsIds = new ArrayList<>();

        // handling condition
        condition.setTable(table);
        for (List<String> row : rows) {
            if (condition.evaluate(allColumnNames, row)) {filteredRowsIds.add(row.get(0));}
        }
    
        for (String id : filteredRowsIds) {table.updateRow(id, nameValueList);}
        table.saveToFile();
        return "[OK]";
    }
}
