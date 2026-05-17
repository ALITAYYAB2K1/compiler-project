import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    // Inner class to hold the variable's exact type and current value
    public static class SymbolRecord {
        public String type;
        public String value;

        public SymbolRecord(String type) {
            this.type = type;
            this.value = "null"; // Default value before assignment
        }
    }

    // The actual table: Maps VariableName -> Record(Type, Value)
    private Map<String, SymbolRecord> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    // 1. Add a new variable to the table (used when parsing declarations)
    public void addVariable(String name, String type) {
        if (table.containsKey(name)) {
            System.err.println("Semantic Error: Variable '" + name + "' is already declared.");
        } else {
            table.put(name, new SymbolRecord(type));
        }
    }

    // 2. Update the value of an existing variable (used during assignments)
    public void setValue(String name, String value) {
        if (table.containsKey(name)) {
            table.get(name).value = value;
        } else {
            System.err.println("Semantic Error: Cannot assign. Variable '" + name + "' is not declared.");
        }
    }

    // 3. Get the type of a variable (Crucial for our Semantic Analyzer!)
    public String getType(String name) {
        if (table.containsKey(name)) {
            return table.get(name).type;
        }
        return null; // Means the variable doesn't exist
    }

    // 4. Print the table to the console to secure those project marks
    public void printTable() {
        System.out.println("\n================ SYMBOL TABLE ================");
        System.out.printf("%-15s | %-10s | %-10s\n", "LEXEME (NAME)", "TYPE", "VALUE");
        System.out.println("----------------------------------------------");
        for (Map.Entry<String, SymbolRecord> entry : table.entrySet()) {
            System.out.printf("%-15s | %-10s | %-10s\n", entry.getKey(), entry.getValue().type, entry.getValue().value);
        }
        System.out.println("==============================================\n");
    }
}