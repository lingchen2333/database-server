# Java Database Server

A lightweight database server implemented in Java, designed to process `.tab` files and execute basic queries.

## Features

- Reads and parses `.tab` files to populate in-memory data structures
- Supports **persistent storage**, ensuring data is saved across sessions
- Implements a client-server model with request handling
- Supports query execution for data retrieval and manipulation

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/lingchen2333/db-server.git
   cd db-server
   ```

2. Run the server:
   ```bash
   ./mvnw exec:java@server
   ```

3. In a separate terminal, run the client:
   ```bash
   ./mvnw exec:java@client
   ```

## Query Language

The database server supports a SQL-like query language. For detailed syntax, see [Query Language Specification](./BNF.txt).

### Supported Operations

```sql
-- Database Operations
CREATE DATABASE dbname;
USE dbname;
DROP DATABASE dbname;

-- Table Operations
CREATE TABLE tablename (column1, column2, column3);
ALTER TABLE tablename ADD columnname;
DROP TABLE tablename;

-- Data Operations
INSERT INTO tablename VALUES ('value1', value2, TRUE);
SELECT * FROM tablename WHERE condition1 AND condition2;
UPDATE tablename SET column = value WHERE condition;
DELETE FROM tablename WHERE condition;

-- Join Operations
JOIN table1 AND table2 ON column1 AND column2;
```

### Example Usage

```sql
-- Create and use a database
CREATE DATABASE markbook;
USE markbook;

-- Create a table for student marks
CREATE TABLE marks (name, mark, pass);

-- Insert student data
INSERT INTO marks VALUES ('Simon', 65, TRUE);

-- Query passing students with marks above 60
SELECT * FROM marks WHERE pass = TRUE AND mark > 60;

-- Update a student's mark
UPDATE marks SET mark = 38 WHERE name == 'Chris';
```






