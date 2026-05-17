public class SemanticAnalyzer implements CLParserVisitor {
    
    private SymbolTable symTable;

    public SemanticAnalyzer(SymbolTable symTable) {
        this.symTable = symTable;
    }

    // Helper method to safely visit child nodes and get their data type back
    private String visitChild(SimpleNode node, int childIndex, Object data) {
        if (node.jjtGetNumChildren() > childIndex) {
            return (String) node.jjtGetChild(childIndex).jjtAccept(this, data);
        }
        return null;
    }

    // --- STANDARD TRAVERSALS ---
    public Object visit(SimpleNode node, Object data) { return node.childrenAccept(this, data); }
    public Object visit(ASTProgram node, Object data) { return node.childrenAccept(this, data); }
    public Object visit(ASTLoopIf node, Object data) { return node.childrenAccept(this, data); }
    public Object visit(ASTSwitchFor node, Object data) { return node.childrenAccept(this, data); }
    public Object visit(ASTOutString node, Object data) { return node.childrenAccept(this, data); }

    // --- VARIABLE DECLARATIONS ---
    public Object visit(ASTVarDecl node, Object data) {
        // We saved this in the .jjt file as "type:name" (e.g., "int:abc")
        String value = (String) node.jjtGetValue();
        String[] parts = value.split(":");
        String type = parts[0];
        String id = parts[1];
        
        symTable.addVariable(id, type); // Save to our memory

        // Check the right side of the equals sign
        String exprType = visitChild(node, 0, data);
        if (exprType != null && !type.equals(exprType)) {
            System.err.println("Type Error: Cannot assign " + exprType + " to " + type + " variable '" + id + "'.");
        }
        return null;
    }

    // --- ASSIGNMENTS ---
    public Object visit(ASTAssignment node, Object data) {
        String id = (String) node.jjtGetValue();
        String varType = symTable.getType(id);

        if (varType == null) {
            System.err.println("Semantic Error: Variable '" + id + "' is not declared.");
            return null;
        }

        // Check if the right side matches the variable's type
        String exprType = visitChild(node, 0, data);
        if (exprType != null && !varType.equals(exprType)) {
            System.err.println("Type Error: Cannot assign " + exprType + " to " + varType + " variable '" + id + "'.");
        }
        return null;
    }

    // --- MATH OPERATIONS (The core of the assignment rule!) ---
    private String checkMath(SimpleNode node, Object data, String operator) {
        String leftType = visitChild(node, 0, data);
        String rightType = visitChild(node, 1, data);

        if (leftType != null && rightType != null) {
            if (!leftType.equals(rightType)) {
                System.err.println("Type Error: Cannot apply '" + operator + "' to different types (" + leftType + " and " + rightType + ").");
                return "error";
            }
            return leftType; // If int + int, return "int" so the parent node knows!
        }
        return null;
    }

    public Object visit(ASTAdd node, Object data) { return checkMath(node, data, "+"); }
    public Object visit(ASTSubtract node, Object data) { return checkMath(node, data, "-"); }
    public Object visit(ASTMultiply node, Object data) { return checkMath(node, data, "*"); }
    public Object visit(ASTDivide node, Object data) { return checkMath(node, data, "/"); }

    // --- LOGIC OPERATIONS ---
    public Object visit(ASTLessThanEqual node, Object data) { checkMath(node, data, "<="); return "boolean"; }
    public Object visit(ASTGreaterThanEqual node, Object data) { checkMath(node, data, ">="); return "boolean"; }
    public Object visit(ASTEqual node, Object data) { checkMath(node, data, "=="); return "boolean"; }
    public Object visit(ASTNotEqual node, Object data) { checkMath(node, data, "<>"); return "boolean"; }
    public Object visit(ASTGreaterThan node, Object data) { checkMath(node, data, ">"); return "boolean"; }
    public Object visit(ASTLessThan node, Object data) { checkMath(node, data, "<"); return "boolean"; }

    // --- FACTORS (Numbers, Strings, Identifiers) ---
    public Object visit(ASTFactor node, Object data) {
        String val = (String) node.jjtGetValue();
        if (val == null) {
            return visitChild(node, 0, data); // It's a parenthesis (Expression)
        }

        // Regex checks to determine what data type this raw text is
        if (val.matches("[0-9]+")) return "int";
        if (val.matches("[0-9]+\\.[0-9]+")) return "float";
        if (val.startsWith("\"")) return "string";
        if (val.startsWith("'")) return "char";

        // If it's none of the above, it MUST be a variable name. Look it up!
        String type = symTable.getType(val);
        if (type == null) {
            System.err.println("Semantic Error: Variable '" + val + "' used but not declared.");
            return "error";
        }
        return type;
    }

    public Object visit(ASTConstant node, Object data) {
        String val = (String) node.jjtGetValue();
        if (val.matches("[0-9]+")) return "int";
        if (val.startsWith("\"")) return "string";
        return "unknown";
    }
}