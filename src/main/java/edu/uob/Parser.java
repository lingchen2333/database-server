package edu.uob;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Parser  {

    private final Tokeniser tokeniser;
    private static final Set<String> KEYWORDS = Set.of(
            "use", "create", "drop", "alter", "insert", "select", "update", "delete",
            "join", "where", "and", "or", "table", "database", "values", "from",
            "into", "set", "on", "add", "true", "false", "like", "null"
    );
    private static final Set<Character> SYMBOLS = Set.of(
            '!', '#', '$', '%', '&', '(', ')', '*',
            '+', ',', '-', '.', '/', ':', ';', '>', '=', '<', '?',
            '@', '[', '\\', ']', '^', '_', '`', '{', '}', '~'
    );
    private static final Set<String> BOOLEAN = Set.of("true", "false");
    private static final Set<String> COMPARATOR = Set.of("==", "!=", ">=", "<=", ">", "<", "like");


    public Parser(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public ICommand parseCommand() throws DBCommandException {
        String keyword = parseKeyword();
        return switch (keyword) {
            case "use" -> parseUseCommand();
            case "create" -> parseCreateCommand();
            case "drop" -> parseDropCommand();
            case "alter" -> parseAlterCommand();
            case "insert" -> parseInsertCommand();
            case "select" -> parseSelectCommand();
            case "update" -> parseUpdateCommand();
            case "delete" -> parseDeleteCommand();
            case "join" -> parseJoinCommand();
            default -> throw new DBCommandException("Invalid command.");
        };

    }


    // =========================
    // different command parsers
    // ==========================

    private UseCommand parseUseCommand() throws DBCommandException {
        UseCommand useCommand = new UseCommand();

        String databaseName = parseDatabaseName();
        useCommand.setDatabaseName(databaseName);

        parseSemicolon();
        return useCommand;
    }

    private CreateCommand parseCreateCommand() throws DBCommandException {
        CreateCommand createCommand = new CreateCommand();
        String keyword = parseKeyword();
        if (keyword.equals("database")) {
            createCommand.setCommandType("create database");

            String databaseName = parseDatabaseName();
            createCommand.setDatabaseName(databaseName);

            parseSemicolon();
            return createCommand;
        } else if (keyword.equals("table")){
            createCommand.setCommandType("create table");

            String tableName = parseTableName();
            createCommand.addTableName(tableName);

            List<String> attributeList = new ArrayList<>();
            if (tokeniser.getCurrent().equals("(")){
                tokeniser.nextToken(); //consume (
                attributeList = parseAttributeList();
                if (!tokeniser.getCurrent().equals(")")){
                    throw new DBCommandException("Expect ')' after attribute list.");
                }
                tokeniser.nextToken(); //consume )
            }
            parseSemicolon();

            createCommand.setColumnNames(attributeList);
            return createCommand;
        } else { throw new DBCommandException("Expect 'DATABASE' or 'TABLE' after CREATE."); }
    }

    private DropCommand parseDropCommand() throws DBCommandException {
        DropCommand dropCommand = new DropCommand();

       String keyword = parseKeyword();
       if (keyword.equals("database")) {
           dropCommand.setCommandType("drop database");

           String databaseName = parseDatabaseName();
           dropCommand.setDatabaseName(databaseName);

           parseSemicolon();
           return dropCommand;
       } else if (keyword.equals("table")) {
           dropCommand.setCommandType("drop table");

           String tableName = parseTableName();
           dropCommand.addTableName(tableName);
           parseSemicolon();
           return dropCommand;
       }
       throw new DBCommandException("Expect 'DATABASE' or 'TABLE' after DROP.");
    }

    private AlterCommand parseAlterCommand() throws DBCommandException {
        AlterCommand alterCommand = new AlterCommand();
        String keyword = parseKeyword();
        if (!keyword.equals("table")) {throw new DBCommandException("Expect 'TABLE' after ALTER.");}

        String tableName = parseTableName();
        alterCommand.addTableName(tableName);

        String alterationType = parseKeyword();
        if (alterationType.equals("add")) {alterCommand.setCommandType("add");}
        else if (alterationType.equals("drop")) {alterCommand.setCommandType("drop");}
        else {throw new DBCommandException("Expect 'ADD' or 'DROP' after table names in Alter command.");}

        String attributeName = parseAttributeName();
        alterCommand.addColumnName(attributeName);

        parseSemicolon();
        return alterCommand;
    }

    private InsertCommand parseInsertCommand() throws DBCommandException {
        InsertCommand insertCommand = new InsertCommand();
        if (!parseKeyword().equals("into")) {throw new DBCommandException("Expect 'INTO' after 'INSERT' in Insert command.");}

        String tableName = parseTableName();
        insertCommand.addTableName(tableName);

        if (!parseKeyword().equals("values")) { throw new DBCommandException("Expect 'VALUES' after table name in Insert command.");}

        if (!tokeniser.getCurrent().equals("(")) {throw new DBCommandException("Expect '(' after 'VALUES' in Insert command.");}
        tokeniser.nextToken(); //consume (

        List<String> valueList = parseValueList();
        insertCommand.setValues(valueList);

        if (!tokeniser.getCurrent().equals(")")){ throw new DBCommandException("Expect ')' after values in Insert command.");}
        tokeniser.nextToken(); //consume )
        parseSemicolon();
        return insertCommand;
    }

    private SelectCommand parseSelectCommand() throws DBCommandException {
        SelectCommand selectCommand = new SelectCommand();

        if (tokeniser.getCurrent().equals("*")) {
            selectCommand.setSelectAll(true);
            tokeniser.nextToken(); //consume *
        } else {
            selectCommand.setColumnNames(parseAttributeList());
        }

        if (!parseKeyword().equals("from")) {throw new DBCommandException("Expect 'FROM' after column lists in Select command.");}

        selectCommand.addTableName(parseTableName());

        if (tokeniser.getCurrent().equals(";")) {
            parseSemicolon();
            return selectCommand;
        } else if (parseKeyword().equals("where")) {
            Condition condition = parseCondition();
            selectCommand.setCondition(condition);

            parseSemicolon();
            return selectCommand;
        } else {
            throw new DBCommandException("Expected ';' or 'WHERE' after table name in SELECT command");
        }
    }

    private UpdateCommand parseUpdateCommand() throws DBCommandException {
        UpdateCommand updateCommand = new UpdateCommand();

        updateCommand.addTableName(parseTableName());

        if (!parseKeyword().equals("set")) {
            throw new DBCommandException("Expected 'SET' after table name in Update command");
        }

        List<NameValuePair> nameValueList = parseNameValueList();
        updateCommand.setNameValueList(nameValueList);

        if (!parseKeyword().equals("where")) {
            throw new DBCommandException("Expected 'WHERE' in Update command");
        }

        Condition condition = parseCondition();
        updateCommand.setCondition(condition);

        parseSemicolon();
        return updateCommand;
    }

    private DeleteCommand parseDeleteCommand() throws DBCommandException {
        DeleteCommand deleteCommand = new DeleteCommand();

        if (!parseKeyword().equals("from")) {
            throw new DBCommandException("Expected 'FROM' after DELETE");
        }

        deleteCommand.addTableName(parseTableName());

        if (!parseKeyword().equals("where")) {
            throw new DBCommandException("Expected 'WHERE' in Delete command");
        }

        Condition condition = parseCondition();
        deleteCommand.setCondition(condition);

        parseSemicolon();
        return deleteCommand;
    }

    private JoinCommand parseJoinCommand() throws DBCommandException {
        JoinCommand joinCommand = new JoinCommand();

        joinCommand.addTableName(parseTableName());
        if (!parseKeyword().equals("and")) {throw new DBCommandException("Expected 'AND' after first table name in Join command");}
        joinCommand.addTableName(parseTableName());

        if (!parseKeyword().equals("on")) {throw new DBCommandException("Expected 'ON' after second table name in Join command");}

        joinCommand.addColumnName(parseAttributeName());
        if (!parseKeyword().equals("and")) {throw new DBCommandException("Expected 'AND' after first column name in Join command");}
        joinCommand.addColumnName(parseAttributeName());
        
        parseSemicolon();
        return joinCommand;
    }




    //====================================
    //helper function to parse BNF element
    //====================================
    private String parseKeyword() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (!isKeyWord(token)) {
            throw new DBCommandException(token + " is not a valid keyword");
        }
        tokeniser.nextToken(); //consume keyword
        return token.toLowerCase();
    }

    private void parseSemicolon() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (!token.equals(";")) {
            throw new DBCommandException("Missing semicolon.");
        }

        if (tokeniser.hasNext()) {throw new DBCommandException("Unexpected text after semicolon.");}
    }

    private String parsePlainText() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (token == null || isKeyWord(token)) { throw new DBCommandException(token + " is not plain text."); }

        for (char c : token.toCharArray()) {
            if (!Character.isLetter(c) && !Character.isDigit(c)) { throw new DBCommandException(token + " is not plain text."); }
        }
        tokeniser.nextToken(); //consume plain text
        return token;
    }

    private String parseDatabaseName() throws DBCommandException {
        try {
            return parsePlainText().toLowerCase();
        } catch (DBCommandException e){
            throw new DBCommandException("Invalid database name: " + e.getMessage());
        }
    }

    private String parseTableName() throws DBCommandException {
        try {
            return parsePlainText().toLowerCase();
        } catch (DBCommandException e){
            throw new DBCommandException("Invalid table name: " + e.getMessage());
        }
    }

    private String parseAttributeName() throws DBCommandException {
        try {
            return parsePlainText();
        } catch (DBCommandException e){
            throw new DBCommandException("Invalid attribute name: " + e.getMessage());
        }
    }

    private List<String> parseAttributeList() throws DBCommandException {
        List<String> attributeList = new ArrayList<>();
        attributeList.add(parseAttributeName());

        while (tokeniser.getCurrent().equals(",")) {
            tokeniser.nextToken(); //consume ,
            attributeList.add(parseAttributeName());
        }
        return attributeList;
    }

    private List<String> parseValueList() throws DBCommandException {
        List<String> valueList = new ArrayList<>();
        valueList.add(parseValue());

        while (tokeniser.getCurrent().equals(",")) {
            tokeniser.nextToken(); //consume ,
            valueList.add(parseValue());
        }
        return valueList;
    }

    private String parseValue() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (token == null) { throw new DBCommandException("Invalid value."); }
        if (token.equals("'")) { return parseStringLiteral(); } //string literal
        if (token.equalsIgnoreCase("null")){ //null
            tokeniser.nextToken();
            return token.toLowerCase();
        }
        if (isBooleanLiteral(token)) {// boolean literal
            tokeniser.nextToken();
            return token.toLowerCase();
        }
        if (token.matches("[+-]?\\d+(\\.\\d+)?")){ //integer literal and float literal
            tokeniser.nextToken();
            return token;
        }

        throw new DBCommandException("Invalid value: " + token);
    }

    private String parseStringLiteral() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (!token.equals("'")) {throw new DBCommandException("Expect String literal to be enclosed in single quote.");}
        tokeniser.nextToken(); //consume opening '

        String stringLiteral = tokeniser.getCurrent();
        for (char c : stringLiteral.toCharArray()) {
            if (!isCharLiteral(c)) {throw new DBCommandException("Invalid character " + c + "in a string literal.");}
        }
        tokeniser.nextToken(); //consume stringLiteral

        token = tokeniser.getCurrent();
        if (!token.equals("'")) {throw new DBCommandException("Expect String literal to be enclosed in single quote.");}
        tokeniser.nextToken(); //consume '
        return stringLiteral;
    }

    //====================================
    //          parse condition
    //====================================

    private Condition parseOrCondition() throws DBCommandException {
        Condition leftCondition = parseAndCondition(); //parse and condition first
    
        while (tokeniser.getCurrent().equalsIgnoreCase("or")) {
            tokeniser.nextToken(); // Consume "OR"
            Condition rightCondition = parseAndCondition(); //parse and condition recursively
            leftCondition = new Condition(new ArrayList<>(Arrays.asList(leftCondition, rightCondition)), "or");
        }
        return leftCondition;
    }

    private Condition parseAndCondition() throws DBCommandException { // and has higher precedence than or
        Condition leftCondition = parseSimpleCondition(); 
    
        while (tokeniser.getCurrent().equalsIgnoreCase("and")) {
            tokeniser.nextToken(); // Consume "AND"
            Condition rightCondition = parseSimpleCondition();
            leftCondition = new Condition(new ArrayList<>(Arrays.asList(leftCondition, rightCondition)), "and");
        }
        return leftCondition;
    }

    private Condition parseCondition() throws DBCommandException { //parse complex condition
        return parseOrCondition();
    }

    private Condition parseSimpleCondition() throws DBCommandException {
        String token = tokeniser.getCurrent();

        // Handle parenthesized condition
        if (token.equals("(")) {
            tokeniser.nextToken(); // consume (
            Condition condition = parseCondition();
            token = tokeniser.getCurrent();
            if (!token.equals(")")) {
                throw new DBCommandException("Expected ')' to close parenthesized condition");
            }
            tokeniser.nextToken(); // consume )
            return condition;
        }

        // Handle simple condition (attribute comparator value)
        String attributeName = parseAttributeName();

        String comparator = parseComparator();

        String value = parseValue();

        return new Condition(attributeName, comparator, value);
    }

    private String parseComparator() throws DBCommandException {
        String token = tokeniser.getCurrent();
        if (isComparator(token)) {
            tokeniser.nextToken(); // consume comparator
            return token.toLowerCase();
        }
        throw new DBCommandException("Invalid comparator");
    }

    private NameValuePair parseNameValuePair() throws DBCommandException {
        String name = parseAttributeName();
        // if (name.equals("id")) {throw new DBCommandException("Cannot update ID column");
        // }

        if (!tokeniser.getCurrent().equals("=")) {
            throw new DBCommandException("Expected '=' after attribute name in Update command");
        }
        tokeniser.nextToken(); // consume =
        String value = parseValue();
        return new NameValuePair(name, value);
    }

    private List<NameValuePair> parseNameValueList() throws DBCommandException {
        List<NameValuePair> nameValueList = new ArrayList<>();

        nameValueList.add(parseNameValuePair());

        while (tokeniser.getCurrent().equals(",")) {
            tokeniser.nextToken(); // consume ,
            nameValueList.add(parseNameValuePair());
        }

        return nameValueList;
    }

    private boolean isKeyWord(String token) throws DBCommandException {
        if (token == null || token.isEmpty()){ return false; }
        return KEYWORDS.contains(token.toLowerCase());
    }

    private boolean isSymbol(Character c) {
        return SYMBOLS.contains(c);
    }

    private boolean isCharLiteral(Character c) {
        if (Character.isLetter(c) || isSymbol(c) || Character.isDigit(c) || Character.isSpaceChar(c)) { return true; }
        return false;
    }

    private boolean isBooleanLiteral(String token) {return  BOOLEAN.contains(token.toLowerCase());}

    private boolean isComparator(String token) {
        return COMPARATOR.contains(token.toLowerCase());
    }

}

