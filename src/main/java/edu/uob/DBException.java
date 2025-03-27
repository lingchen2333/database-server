package edu.uob;

import java.io.Serial;
import java.io.Serializable;

public class DBException extends Exception implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public DBException(String message) {
        super(message);
    }
}
