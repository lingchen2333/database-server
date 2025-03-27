package edu.uob;

import java.util.ArrayList;
import java.util.List;

public abstract class DBCommand implements ICommand {

    protected String commandType;
    protected String databaseName;
    protected List<String> tableNames;
    protected List<String> columnNames;
    protected List<String> values;
    protected Condition condition;

    public DBCommand() {
        tableNames = new ArrayList<>();
        columnNames = new ArrayList<>();
        values = new ArrayList<>();
    }

    @Override
    public abstract String queryServer(DBServer dbServer) throws DBException;

    @Override
    public void setCommandType(String commandType) {this.commandType = commandType;}
    @Override
    public String getCommandType() {return commandType;}

    @Override
    public void setDatabaseName(String databaseName) {this.databaseName = databaseName;}
    @Override
    public String getDatabaseName() {return databaseName;}

    @Override
    public void addTableName(String tableName) {this.tableNames.add(tableName);}
    @Override
    public List<String> getTableNames() {return new ArrayList<String>(tableNames);}

    @Override
    public void setColumnNames(List<String> columnNames) {this.columnNames = columnNames;}
    @Override
    public List<String> getColumnNames() {return new ArrayList<>(columnNames);}
    @Override
    public void addColumnName(String columnName) {this.columnNames.add(columnName);}

    @Override
    public void setCondition(Condition condition) {this.condition = condition;}
    @Override
    public Condition getCondition() {return condition;}

    @Override
    public void setValues(List<String> values) {this.values = values;}
    @Override
    public List<String> getValues() {return new ArrayList<>(values);}

}
