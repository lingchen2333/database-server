package edu.uob;
import java.util.List;
import java.util.ArrayList;

public class JoinCommand extends DBCommand {

    public String queryServer(DBServer dbServer) throws DBException {
        String table1Name = tableNames.get(0);
        String table2Name = tableNames.get(1);

        Table table1 = dbServer.getCurrentDatabase().getTable(table1Name);
        Table table2 = dbServer.getCurrentDatabase().getTable(table2Name);

        String columnName1 = columnNames.get(0);
        columnName1 = table1.getActualColumnName(columnName1);
        String columnName2 = columnNames.get(1);
        columnName2 = table2.getActualColumnName(columnName2);

        int columnIndex1 = table1.getColumnNames().indexOf(columnName1);
        int columnIndex2 = table2.getColumnNames().indexOf(columnName2);

        // remove the columns in table1 and table2 that are being joined on
        List<String> resultColumnNames = new ArrayList<>();
        resultColumnNames.add("id");
        List<String> table1ColumnNames =table1.getColumnNames();
        List<String> table2ColumnNames =table2.getColumnNames();

        //remove the join column from table1 and table2
        table1ColumnNames.remove(columnIndex1);
        table2ColumnNames.remove(columnIndex2);

        //remove the id column from table1 and table2
        if (columnIndex1 !=0) {table1ColumnNames.remove(0);}
        if (columnIndex2 !=0) {table2ColumnNames.remove(0);}

        table1ColumnNames.forEach(table1ColumnName -> {resultColumnNames.add(table1Name + "." + table1ColumnName);} );
        table2ColumnNames.forEach(table2ColumnName -> {resultColumnNames.add(table2Name + "." + table2ColumnName);} );

        int id = 1;
        List<List<String>> resultRows = new ArrayList<>();
        for (List<String> table1Row : table1.getRows()) { // for each row in table1
            String value = table1Row.get(columnIndex1); // get the value of the column in table1
            table1Row.remove(columnIndex1); //remove the join column from table1
            if (columnIndex1 !=0) {table1Row.remove(0);} //remove the id column from table1
            List<List<String>> table2MatchingRows = table2.getRowsByColumnValue(columnName2, value); // get the rows in table2 that have the same value in the column

            for (List<String> table2Row : table2MatchingRows) {
                List<String> table2RowCopy = new ArrayList<>(table2Row);
                
                table2RowCopy.remove(columnIndex2); //remove the column in table2 that is being joined on
                if (columnIndex2 !=0) {table2RowCopy.remove(0);} //remove the id column from table2
                
                List<String> joinedRow = new ArrayList<>();
                joinedRow.add(String.valueOf(id));
                joinedRow.addAll(table1Row);
                joinedRow.addAll(table2RowCopy);
                resultRows.add(joinedRow);
                id++;
            }
        }

        StringBuilder resultString = new StringBuilder();
        resultString.append("[OK]");
        resultString.append("\n");
        resultString.append(String.join("\t", resultColumnNames));
        resultString.append("\n");
        for (List<String> row : resultRows) {
            resultString.append(String.join("\t", row));
            resultString.append("\n");
        }
        return resultString.toString();
    }

}
