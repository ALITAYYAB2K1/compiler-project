# Compiler Construction - Milestone 2 (Semantic Analysis & 3AC Generation)

This project implements the front-end (Scanner/Parser), Semantic Analyzer (Type Checking), and Back-end (Three-Address Code Quadruple Generation) for the CL Language.

It successfully handles nested `loopif` statements and nested `switchFor` conditional branches by dynamically generating jump labels.

## 📂 Folder Structure

Ensure your files are organized exactly like this before running any commands:

```text
COMPILER ASSIGNMENT 2/
│
├── frontend/
│   ├── CLParser.jjt        (The core grammar and AST logic)
│   └── javacc.jar          (The JavaCC executable tool)
│
├── src/
│   ├── SymbolTable.java       (Memory management)
│   ├── SemanticAnalyzer.java  (Type checking logic)
│   ├── CodeGenerator.java     (3AC & Quadruple logic)
│   └── Main.java              (The compiler manager)
│
├── input.txt               (Your test code goes here)
└── README.md               (This file)
```

---

## 🛠️ Compilation Instructions

If you change the grammar in `CLParser.jjt` or the logic in any of the `.java` files, you must recompile the project. Open your terminal (PowerShell) and follow these steps in order:

### Phase 1: Generate the Front-End (Parser & AST)

Navigate to the `frontend` folder and run the JavaCC tools:

```powershell
cd .\frontend\
java -cp javacc.jar jjtree CLParser.jjt
java -cp javacc.jar javacc CLParser.jj
```

### Phase 2: Compile the Back-End (Java Source Files)

Navigate to the `src` folder. You must compile these files using the `-cp` (classpath) flag so Java knows to link them to the AST files generated in Phase 1.

```powershell
cd ..\src\
javac SymbolTable.java
javac -cp ".;../frontend" SemanticAnalyzer.java
javac -cp ".;../frontend" CodeGenerator.java
javac -cp ".;../frontend" Main.java
```

_(Note: You can also compile them all at once by running: `javac -cp ".;../frontend" _.java`)\*

---

## 🚀 Execution Instructions

To run the compiler and generate the Three-Address Code, ensure your CL code is written inside `input.txt` in the root directory.

From inside the `src` folder, run the following command to pipe the text file into the compiled program:

```powershell
Get-Content ../input.txt | java -cp ".;../frontend" Main
```

or put any filename

```powershell
Get-Content ../anyname.txt | java -cp ".;../frontend" Main
```

### Expected Output:

1. Syntax and AST validation confirmation.
2. The Symbol Table mapping variable lexemes to their data types (`int`, `float`, `string`, `char`).
3. Semantic Analysis confirmation (will halt and warn if type errors are found, e.g., adding `int` to `float`).
4. The generated Three-Address Code (3AC) printed in both Equation format and Quadruple Table format.
