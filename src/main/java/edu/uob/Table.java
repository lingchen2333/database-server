package edu.uob;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Table {

    private String name;
    private List<String> columnNames;
    private List<List<String>> rows;
    private int nextId;
    private String filePath;

    public Table(String name, String databaseStoragePath) {
        this.name = name.toLowerCase();
        this.columnNames= new ArrayList<>();
        columnNames.add("id"); //initiate the id column
        this.rows = new ArrayList<>();
        nextId = 1;
        filePath = databaseStoragePath + File.separator + this.name + ".tab";
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {return this.filePath;}

    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }

    public void addColumn(String columnName) throws DBException {
        if (columnNames.contains(columnName)) {
            throw new DBException("Unable to insert column: " + columnName + " because it already exists");
        }
        columnNames.add(columnName);
        for (List<String> row : rows) {
            row.add("NULL");
        }
    }

    public void dropColumn(String columnName) throws DBException {
        columnName = getActualColumnName(columnName);
        int columnIndex = columnNames.indexOf(columnName);
        if (columnIndex == 0) {
            throw new DBException("Cannot drop id column");
        }
        columnNames.remove(columnIndex);
        for (List<String> row : rows) {
            row.remove(columnIndex);
        }
    }

    public List<List<String>> getRows() {
        return new ArrayList<>(this.rows);
    }

    public void insertRow(List<String> values) throws DBException {
        if ((values.size() +1) != columnNames.size()) {
            throw new DBException("Number of values does not match number of columns in table " + name);
        }
        List<String> newRow = new ArrayList<>();
        newRow.add(String.valueOf(nextId++)); //autogenerate and add id
        newRow.addAll(values);
        this.rows.add(newRow);
    }

    public void deleteRow(String id) {
        this.rows.removeIf(row -> row.get(0).equals(id));
    }

    public void updateRow(String id, List<NameValuePair> nameValueList) throws DBException {
        List<String> row = this.rows.stream()
                .filter(r -> r.get(0).equals(id))
                .findFirst()
                .orElse(null);

        if (row == null) {return;}
        for (NameValuePair nameValuePair : nameValueList) {
            String columnName = getActualColumnName(nameValuePair.getName());
            int columnIndex = columnNames.indexOf(columnName);
            if (columnIndex == 0) {throw new DBException("Cannot update id column");}
            row.set(columnIndex, nameValuePair.getValue());
        }
    }

    public List<List<String>> getRowsByColumnValue(String columnName, String value) throws DBException {
        columnName = getActualColumnName(columnName);
        int columnIndex = columnNames.indexOf(columnName);
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            if (row.get(columnIndex).equals(value)) {result.add(new ArrayList<>(row));}
        }
        return result;
    }
    
    public String getActualColumnName(String columnName) throws DBException {
        return columnNames.stream()
                .filter(c -> c.equalsIgnoreCase(columnName))
                .findFirst()
                .orElseThrow(() -> new DBException("column: " + columnName + " does not exist in the table: " + this.name));
    }

    //===========================
    //  data storage to file
    //===========================
    public void loadFromFile() throws DBException {
        File tableFile = new File(filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(tableFile))) {
            //reader header
            String headerLine = br.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {return;}
            this.columnNames = new ArrayList<>(Arrays.asList(headerLine.split("\t")));

            //read next id
            String nextIdLine = br.readLine();
            if (nextIdLine == null || nextIdLine.trim().isEmpty()) {
                throw new DBException("Unable to load the next available id to use for table: " + this.name);
            }
            this.nextId = Integer.parseInt(nextIdLine);

            //read data rows
            String line;
            while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
                List<String> values = new ArrayList<>(Arrays.asList(line.split("\t")));
                this.rows.add(values);
            }
        } catch (IOException e) {
            throw new DBException("Failed to load table: " + this.name);
        }
    }

    public void saveToFile() throws DBException {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new DBException("Unable to create table file: " + filePath);
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            //write header
            bw.write(String.join("\t", this.columnNames));
            bw.newLine();

            //write the next id line
            bw.write(String.valueOf(nextId));
            bw.newLine();

            //write data rows
            for (List<String> row : this.rows) {
                bw.write(String.join("\t", row));
                bw.newLine();
            }
        } catch (IOException e){
            throw new DBException("Unable to save table file: " + filePath);
        }
    }

}
