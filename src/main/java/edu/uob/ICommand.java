package edu.uob;

import java.util.List;

public interface ICommand {
    String queryServer(DBServer dbServer) throws DBException;
    void setCommandType(String commandType);
    String getCommandType();
    void setDatabaseName(String databaseName);
    String getDatabaseName();
    void addTableName(String tableName);
    List<String> getTableNames();
    void setColumnNames(List<String> columnNames);
    List<String> getColumnNames();
    void addColumnName(String columnName);
    void setCondition(Condition condition);
    Condition getCondition();
    void setValues(List<String> values);
    List<String> getValues();
} 