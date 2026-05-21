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

        // Formats it for the Equation View
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

        // --- NEW: BINARY FORMAT HELPERS ---
        
        // Maps operators to 8-bit opcodes
        private String getOpcode(String operator) {
            switch(operator) {
                case "+": return "00000001";
                case "-": return "00000010";
                case "*": return "00000011";
                case "/": return "00000100";
                case "=": return "00000101";
                case "minus": return "00000110";
                case "ifFalse": return "00000111";
                case "goto": return "00001000";
                case "==": return "00001001";
                case "<": return "00001010";
                case ">": return "00001011";
                case "<=": return "00001100";
                case ">=": return "00001101";
                case "<>": return "00001110";
                case "print": return "00001111";
                case "Label": return "11111111";
                default: return "00000000";
            }
        }

        // Converts strings (like "t1" or "abc") to binary string representation
        private String toBin(String s) {
            if (s == null || s.isEmpty()) return "00000000";
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) {
                b.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
            }
            return b.toString();
        }

        // Formats it for the Binary View (Line by Line)
        public String toBinary() {
            String opBin = getOpcode(op);
            String arg1Bin = toBin(arg1);
            String arg2Bin = toBin(arg2.replace("goto ", ""));
            String resBin = toBin(result.replace("goto ", ""));
            
            if (op.equals("Label")) return resBin + ":";
            return opBin + " " + arg1Bin + " " + arg2Bin + " " + resBin;
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

    // --- PRINT ALL THREE FORMATS ---
    public void printInstructions() {
        System.out.println("\n============= 3AC (EQUATION FORMAT) =============");
        for (Quadruple q : instructions) {
            if (!q.op.equals("Label")) {
                System.out.println("  " + q.toEquation());
            } else {
                System.out.println("\n" + q.toEquation()); 
            }
        }

        // --- NEW: THE BINARY OUTPUT LOOP ---
        System.out.println("\n============= 3AC (BINARY FORMAT) ===============");
        for (Quadruple q : instructions) {
            if (!q.op.equals("Label")) {
                System.out.println("  " + q.toBinary());
            } else {
                System.out.println("\n" + q.toBinary()); 
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