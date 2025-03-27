package edu.uob;


import java.io.Serial;
import java.io.Serializable;

public class DBCommandException extends Exception implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public DBCommandException(String message) {
        super(message);
    }
}
