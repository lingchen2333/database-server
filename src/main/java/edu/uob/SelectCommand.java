package edu.uob;

import java.util.ArrayList;
import java.util.List;
public class SelectCommand extends DBCommand{ //select values from the current table
    private boolean selectAll;

    SelectCommand() {
        super();
        this.selectAll = false;
    }

    public void setSelectAll(boolean selectAll) {this.selectAll = selectAll;}

    @Override
    public String queryServer(DBServer dbServer) throws DBException {
        String tableName = tableNames.get(0);
        Table table = dbServer.getCurrentDatabase().getTable(tableName);

        if (selectAll) { columnNames = table.getColumnNames();}

        List<String> allColumnNames = table.getColumnNames();
        List<Integer> selectedColumnIndices = new ArrayList<>();
        List<String> actualColumnNames = new ArrayList<>();
        
        for (String columnName : columnNames) {
            String actualColumnName = table.getActualColumnName(columnName);
            selectedColumnIndices.add(allColumnNames.indexOf(actualColumnName));
            actualColumnNames.add(actualColumnName);
        }

        List<List<String>> rows = table.getRows();
        List<List<String>> filteredRows = new ArrayList<>();

        // handling condition
        if (condition == null) {
            filteredRows = rows;
        } else {
            condition.setTable(table);
            for (List<String> row : rows) {
                if (condition.evaluate(allColumnNames, row)) {filteredRows.add(row);}
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("[OK]");
        result.append("\n");
        result.append(String.join("\t", actualColumnNames));
        result.append("\n");
        for (List<String> row : filteredRows) {
            List<String> selectedRow = new ArrayList<>();
            for (Integer index : selectedColumnIndices) {selectedRow.add(row.get(index));}
            result.append(String.join("\t", selectedRow));
            result.append("\n");
        }
        return result.toString();
    }

}
