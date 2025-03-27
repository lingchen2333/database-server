package edu.uob;

import java.util.ArrayList;
import java.util.List;


public class Tokeniser{
    
    private List<String> tokens;
    private int currentPosition;

    public Tokeniser(String command) {
        this.tokens = tokenise(command);
        this.currentPosition = 0;
    }

    public String getCurrent() {
        if (this.currentPosition >= this.tokens.size()) {
            return null;
        }
        return this.tokens.get(this.currentPosition);
    }

    public String lookAhead() {
        if (!hasNext()) return null;
        return this.tokens.get(this.currentPosition + 1);
    }

    public String nextToken(){
        if (!hasNext()) return null;
        currentPosition++;
        return this.tokens.get(this.currentPosition);
    }

    public List<String> getAllTokens() { return new ArrayList<>(this.tokens);}

    public boolean hasNext() { return currentPosition +1 < this.tokens.size(); }

    private List<String> tokenise(String command) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        boolean inComparisonOperator = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\'') { //start or end of string literal
                inComparisonOperator = false;
                if (inQuotes) { inQuotes = false; }
                else { inQuotes = true; }

                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
                tokens.add(String.valueOf(c)); //add ' to tokens
            } else if (inQuotes) {
                currentToken.append(c);
            } else if (c == ';' | c == ',' || c == '(' || c == ')') {
                inComparisonOperator = false;
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            } else if (Character.isWhitespace(c)) {
                inComparisonOperator = false;
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else if (c == '=' || c == '>' || c == '<' || c == '!'){
                if (inComparisonOperator) { //current token: =, >, < or !
                    inComparisonOperator = false;
                    currentToken.append(c);
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                } else {
                    inComparisonOperator = true;
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    }
                    currentToken.append(c);
                }
            } else if (inComparisonOperator) {
                inComparisonOperator = false;
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
                currentToken.append(c);
            } else {currentToken.append(c);}
        }
        if (currentToken.length() > 0) { tokens.add(currentToken.toString()); }
        return tokens;
    }
}
