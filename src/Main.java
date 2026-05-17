public class Main {
    public static void main(String[] args) {
        try {
            // 1. Fire up the Parser reading from the terminal pipe
            CLParser parser = new CLParser(System.in);
            
            System.out.println("\n>>> STARTING COMPILATION PROCESS <<<\n");
            
            // PHASE 1: Parse the grammar and build the AST
            ASTProgram root = parser.Program();
            System.out.println("[✓] Phase 1: Syntax Parsed & AST Built.");

            // PHASE 2 & 3: Set up memory and check semantics
            SymbolTable symTable = new SymbolTable();
            SemanticAnalyzer analyzer = new SemanticAnalyzer(symTable);
            root.jjtAccept(analyzer, null); 
            System.out.println("[✓] Phase 2: Semantic Analysis (Type Checking) Complete.");
            
            // Print the Symbol Table to show the professor we did Milestone 2 properly
            symTable.printTable();

            // PHASE 4: Generate the Three-Address Code
            System.out.println("[✓] Phase 3: Generating Three-Address Code...");
            CodeGenerator generator = new CodeGenerator();
            root.jjtAccept(generator, null); 
            // Note: The generator's visit(ASTProgram) method will automatically print the 3AC table at the end!
            
        } catch (Exception e) {
            System.err.println("\n[X] COMPILATION FAILED: " + e.getMessage());
        }
    }
}