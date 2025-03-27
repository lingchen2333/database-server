package edu.uob;

import java.util.List;

public class Condition {

    private List<Condition> subConditions;
    String boolOperator;

    String attributeName;
    String comparator;
    String value;

    Table table;

    public Condition(String attributeName, String comparator, String value) {
        this.attributeName = attributeName;
        this.comparator = comparator;
        this.value = value;
    }

    public Condition(List<Condition> subConditions, String boolOperator){
        this.subConditions = subConditions;
        this.boolOperator = boolOperator;
    }

    public void setTable(Table table) {
        this.table = table;
        if (isCompound()) {
            for (Condition subCondition : subConditions) {
                subCondition.setTable(table);
            }
        }
    }

    public String getAttributeName() {return attributeName;}
    public String getComparator() {return comparator;}
    public String getValue() {return value;}
    public String getBoolOperator() {return boolOperator;}

    public boolean isCompound(){ return subConditions != null; }

    public boolean evaluate(List<String> allColumnNames, List<String> row) {
        if (isCompound()){
            if (boolOperator.equals("and")){
                return subConditions.stream().allMatch(condition -> condition.evaluate(allColumnNames, row));
            } else {
                return subConditions.stream().anyMatch(condition -> condition.evaluate(allColumnNames, row));
            }
        }
        int columnIndex;
        try {
            columnIndex = allColumnNames.indexOf(table.getActualColumnName(attributeName));
        } catch (DBException e) {
           return false;
        }
        
        if(columnIndex == -1) return false;
        String rowValue = row.get(columnIndex);

        return switch(comparator){
            case "==" -> rowValue.equals(value);
            case "!=" -> !rowValue.equals(value);
            case ">=" -> compareNumeric(rowValue,value) >= 0;
            case "<=" -> compareNumeric(rowValue,value) <= 0;
            case "<" -> compareNumeric(rowValue,value) < 0;
            case ">" -> compareNumeric(rowValue,value) > 0;
            default -> rowValue.contains(value);
        };
    }

    private int compareNumeric(String rowValue, String value) {
        try {
            double rowNum = Double.parseDouble(rowValue);
            double valueNum = Double.parseDouble(value);
            return Double.compare(rowNum, valueNum);
        } catch (NumberFormatException e) {
            return rowValue.compareTo(value);
        }
    }
}
