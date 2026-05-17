import java.util.ArrayList;
import java.util.List;

public class CodeGenerator implements CLParserVisitor {

    static class Quadruple {
        String op, arg1, arg2, result;

        public Quadruple(String op, String arg1, String arg2, String result) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.result = result;
        }

        // Formats it for the Table View
        @Override
        public String toString() {
            return String.format("%-8s | %-8s | %-8s | %-8s", op, arg1, arg2, result);
        }

        // Formats it for the Equation View (Like Image 1)
        public String toEquation() {
            if (op.equals("Label")) return result + ":";
            if (op.equals("goto")) return "goto " + result;
            if (op.equals("ifFalse")) return "ifFalse " + arg1 + " goto " + result.replace("goto ", "");
            if (op.equals("ifTrue")) return "ifTrue " + arg1 + " goto " + result.replace("goto ", "");
            if (op.equals("print")) return "print " + arg1;
            if (op.equals("=")) return result + " = " + arg1;
            if (arg2.isEmpty()) return result + " = " + op + " " + arg1; // Handles the unary "minus"
            return result + " = " + arg1 + " " + op + " " + arg2;
        }
    }

    private List<Quadruple> instructions = new ArrayList<>();
    private int tempCounter = 1;
    private int labelCounter = 1;

    private String newTemp() { return "t" + (tempCounter++); }
    private String newLabel() { return "L" + (labelCounter++); }
    
    private void emit(String op, String arg1, String arg2, String result) {
        instructions.add(new Quadruple(op, arg1, arg2, result));
    }

    private String visitChild(SimpleNode node, int childIndex, Object data) {
        if (node.jjtGetNumChildren() > childIndex) {
            return (String) node.jjtGetChild(childIndex).jjtAccept(this, data);
        }
        return "";
    }

    public Object visit(SimpleNode node, Object data) { return node.childrenAccept(this, data); }
    public Object visit(ASTProgram node, Object data) { 
        node.childrenAccept(this, data); 
        printInstructions(); 
        return null;
    }
    
    public Object visit(ASTVarDecl node, Object data) { 
        String value = (String) node.jjtGetValue();
        String id = value.split(":")[1]; 
        String rhs = visitChild(node, 0, data);
        emit("=", rhs, "", id);
        return null;
    }

    public Object visit(ASTOutString node, Object data) {
        String expr = visitChild(node, 0, data);
        emit("print", expr, "", "");
        return null;
    }

    public Object visit(ASTAssignment node, Object data) {
        String id = (String) node.jjtGetValue();
        String rhs = visitChild(node, 0, data);
        emit("=", rhs, "", id);
        return null;
    }

    private String processMath(SimpleNode node, Object data, String op) {
        String left = visitChild(node, 0, data);
        String right = visitChild(node, 1, data);
        String resultTemp = newTemp();
        emit(op, left, right, resultTemp);
        return resultTemp; 
    }

    public Object visit(ASTAdd node, Object data) { return processMath(node, data, "+"); }
    public Object visit(ASTMultiply node, Object data) { return processMath(node, data, "*"); }
    public Object visit(ASTDivide node, Object data) { return processMath(node, data, "/"); }
    
    public Object visit(ASTSubtract node, Object data) { 
        String left = visitChild(node, 0, data);
        String right = visitChild(node, 1, data);
        String resultTemp = newTemp();
        
        // HACK: If we subtract from 0, generate the Professor's unary "minus" operator!
        if (left.equals("0")) {
            emit("minus", right, "", resultTemp);
        } else {
            emit("-", left, right, resultTemp);
        }
        return resultTemp; 
    }

    public Object visit(ASTLessThanEqual node, Object data) { return processMath(node, data, "<="); }
    public Object visit(ASTGreaterThanEqual node, Object data) { return processMath(node, data, ">="); }
    public Object visit(ASTEqual node, Object data) { return processMath(node, data, "=="); }
    public Object visit(ASTNotEqual node, Object data) { return processMath(node, data, "<>"); }
    public Object visit(ASTGreaterThan node, Object data) { return processMath(node, data, ">"); }
    public Object visit(ASTLessThan node, Object data) { return processMath(node, data, "<"); }

    public Object visit(ASTFactor node, Object data) {
        String val = (String) node.jjtGetValue();
        if (val == null) return visitChild(node, 0, data); 
        return val;
    }
    public Object visit(ASTConstant node, Object data) { return (String) node.jjtGetValue(); }

    // ==========================================
    // NESTED LOOPS BONUS POINTS
    // ==========================================
    public Object visit(ASTLoopIf node, Object data) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        emit("Label", "", "", startLabel);
        String condTemp = visitChild(node, 0, data);
        emit("ifFalse", condTemp, "", "goto " + endLabel);

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).jjtAccept(this, data);
        }

        emit("goto", "", "", startLabel);
        emit("Label", "", "", endLabel);
        return null;
    }

    // ==========================================
    // SWITCH / IF-ELSE BONUS POINTS
    // ==========================================
    public Object visit(ASTSwitchFor node, Object data) {
        String switchVar = (String) node.jjtGetValue();
        String endLabel = newLabel();
        String nextCaseLabel = newLabel();
        
        boolean insideCase = false;
        
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(i);
            
            if (child instanceof ASTConstant) {
                if (insideCase) {
                    emit("goto", "", "", endLabel); 
                    emit("Label", "", "", nextCaseLabel); 
                    nextCaseLabel = newLabel(); 
                } else {
                    insideCase = true;
                }
                
                String caseValue = (String) child.jjtGetValue();
                String condTemp = newTemp();
                emit("==", switchVar, caseValue, condTemp);
                emit("ifFalse", condTemp, "", "goto " + nextCaseLabel);
                
            } else {
                child.jjtAccept(this, data);
            }
        }
        
        emit("Label", "", "", endLabel);
        return null;
    }

    // --- PRINT BOTH FORMATS ---
    public void printInstructions() {
        System.out.println("\n============= 3AC (EQUATION FORMAT) =============");
        for (Quadruple q : instructions) {
            if (!q.op.equals("Label")) {
                System.out.println("  " + q.toEquation());
            } else {
                System.out.println("\n" + q.toEquation()); 
            }
        }

        System.out.println("\n============= 3AC (TABLE FORMAT) ================");
        System.out.printf("%-3s | %-8s | %-8s | %-8s | %-8s\n", "ID", "OP", "ARG1", "ARG2", "RESULT");
        System.out.println("-------------------------------------------------");
        for (int i = 0; i < instructions.size(); i++) {
            System.out.printf("%-3d | %s\n", i, instructions.get(i).toString());
        }
        System.out.println("=================================================\n");
    }
}