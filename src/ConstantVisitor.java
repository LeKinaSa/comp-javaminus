import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConstantVisitor extends AJmmVisitor<List<Report>, Object> {
    private final Map<Symbol, Object> constantTable = new HashMap<>(); // Store all local variables and keep their constant value
    private final JMMSymbolTable symbolTable;

    private static class ConstantPropagationInformation {
        public final JmmNode parent;
        public final int childIndex;
        public final JmmNode replacement;

        public ConstantPropagationInformation(JmmNode parent, int childIndex, JmmNode replacement) {
            this.parent = parent;
            this.childIndex = childIndex;
            this.replacement = replacement;
        }
    }

    public List<ConstantPropagationInformation> constantPropagations = new ArrayList<>();

    public ConstantVisitor(SymbolTable symbolTable) {
        this.symbolTable = (JMMSymbolTable) symbolTable;

        addVisit("Method", this::visitMethod);
        addVisit("Expression", this::visitExpression);
        addVisit("If", this::visitIf);
        addVisit("While", this::visitWhile);
        addVisit("Return", this::visitReturn);

        setDefaultVisit(this::defaultVisit);
    }

    // ----- Helper Functions -----

    public Symbol getVariableSymbol(String fieldName) {
        for (Symbol symbol: constantTable.keySet()) {
            if (symbol.getName().equals(fieldName)) {
                return symbol;
            }
        }

        return null;
    }

    public Object getVariableValue(Symbol symbol) {
        return symbol == null ? null : constantTable.get(symbol);
    }

    public void setVariableValue(Symbol symbol, Object value) {
        constantTable.replace(symbol, value);
    }

    /**
     * Apply constant propagation to this node and all its children
     * @param node
     * @return
     */
    public void constantPropagation(JmmNode node) {
        String kind = node.getKind();

        if (kind.equals("Var")) {
            String varName = node.get("name");
            Symbol symbol = getVariableSymbol(varName);
            Object value = getVariableValue(symbol);
            prepareForReplacement(node, value);
            return;
        }
        else {
            for (JmmNode child : node.getChildren()) {
                constantPropagation(child);
            }
        }
    }
    
    public void prepareForReplacement(JmmNode node, Object value) {
        String type = getType(node);

        if (value != null && type != null) {
            JmmNode replacement = null;
            
            if (type.equals("int")) {
                replacement = new JmmNodeImpl("Int");
                replacement.put("value", String.valueOf(value));
            }
            else if (type.equals("boolean")) {
                if ((Boolean) value) {
                    replacement = new JmmNodeImpl("True");
                }
                else {
                    replacement = new JmmNodeImpl("False");
                }
            }
            
            constantPropagations.add(new ConstantPropagationInformation(node.getParent(), node.getParent().getChildren().indexOf(node), replacement));
        }
    }

    public void printReplacementInfo() {
        for (ConstantPropagationInformation information : constantPropagations) {
            System.out.println("Node to replace: " + information.parent.getChildren().get(information.childIndex).getKind());
            if (information.replacement.getKind().equals("Int")) {
                System.out.println("Replacement: " + information.replacement.get("value"));
            }
            else {
                System.out.println("Replacement: " + information.replacement.getKind());
            }
        }
    }

    public void replaceNodes() {
        for (ConstantPropagationInformation information : constantPropagations) {
            information.parent.getChildren().set(information.childIndex, information.replacement);
        }
    }

    /**
     * Apply constant propagation or constant folding to this node and all its children
     * If constant folding applies it will be used first
     * When constant folding isn't applicable, constant propagation will be applied when possible
     * @param node
     * @return
     */
    public void constantPropagationAndFolding(JmmNode node) {
        Object value = getValue(node);
        if (value != null) {
            prepareForReplacement(node, value);
            return;
        }

        for (JmmNode child : node.getChildren()) {
            constantPropagationAndFolding(child);
        }
    }

    /**
     * Get the constant value of this node, or null if impossible
     * @param node
     * @return
     */
    public Object getValue(JmmNode node) {
        Object left  = (node.getNumChildren() > 0) ? getValue(node.getChildren().get(0)) : null;
        Object right = (node.getNumChildren() > 1) ? getValue(node.getChildren().get(1)) : null;

        switch (node.getKind()) {
            case "Add":
                return ((left == null) || (right == null)) ? null : (Integer) left + (Integer) right;
            case "Sub":
                return ((left == null) || (right == null)) ? null : (Integer) left - (Integer) right;
            case "Mul":
                return ((left == null) || (right == null)) ? null : (Integer) left * (Integer) right;
            case "Div":
                return ((left == null) || (right == null)) ? null : (Integer) left / (Integer) right;
            case "LessThan":
                return ((left == null) || (right == null)) ? null : (Integer) left < (Integer) right;
            case "And":
                return ((left == null) || (right == null)) ? null : (Boolean) left && (Boolean) right;
            case "Not":
                return (left == null) ? null : !(Boolean) left;
            case "Int":
                return Integer.parseInt(node.get("value"));
            case "False":
                return false;
            case "True":
                return true;
            case "Var":
                return getVariableValue(getVariableSymbol(node.get("name")));
            default:
                return null;
        }
    }

    /**
     * Get the type of this node, or null if impossible
     *
     * @param node
     * @return
     */
    public String getType(JmmNode node) {
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mul":
            case "Div":
                return "int";

            case "LessThan":
            case "And":
            case "Not":
                return "boolean";

            case "Var":
                return getVariableSymbol(node.get("name")).getType().getName();
            default:
                return null;
        }
    }

    // ----- Visits -----

    private Object defaultVisit(JmmNode node, List<Report> reports) {
        // Default visit is a simple pre-order visit
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }

        return null;
    }

    private Object visitMethod(JmmNode node, List<Report> reports) {
        // When entering a new method clear the constant table
        constantTable.clear();

        String signature = Utils.generateMethodSignature(node);

        List<Symbol> parameters = symbolTable.getParameters(signature);
        for (Symbol parameter : parameters) {
            constantTable.put(parameter, null);
        }

        List<Symbol> localVariables = symbolTable.getLocalVariables(signature);
        for (Symbol localVariable : localVariables) {
            constantTable.put(localVariable, null);
        }

        return defaultVisit(node, reports);
    }

    public Object visitExpression(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() == 1) {
            node = node.getChildren().get(0);
            String kind = node.getKind();
            switch (kind) {
                case "Assign":
                    return visitAssignment(node, reports);
                case "Dot":
                    return visitDot(node, reports);
                default:
                    return null;
            }
        }
        return null;
    }

    public Object visitAssignment(JmmNode node, List<Report> reports) {
        // Left Node is the Variable or ArrayAccess
        JmmNode left = node.getChildren().get(0);
        // Constant Propagation and Folding
        JmmNode right = node.getChildren().get(1);
        constantPropagationAndFolding(right);

        if (left.getKind().equals("Var")) {
            // Left Node is Variable
            String varName = left.get("name");
            Symbol symbol = getVariableSymbol(varName);

            // Right Node is the Value
            right = node.getChildren().get(1);
            Object value = getValue(right);

            // Modify the Variable Value
            setVariableValue(symbol, value);
        }

        return null;
    }

    public Object visitDot(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() > 1) {
            node = node.getChildren().get(1); // Func
            if (node.getNumChildren() > 0) {
                node = node.getChildren().get(0); // Args
                // Constant Propagation
                constantPropagation(node);
            }
        }

        return null;
    }

    public Object visitReturn(JmmNode node, List<Report> reports) {
        // Constant Propagation
        constantPropagationAndFolding(node);

        return null;
    }

    public Object visitIf(JmmNode node, List<Report> reports) {
        JmmNode condition = node.getChildren().get(0);
        // Constant Propagation and Folding
        constantPropagationAndFolding(condition);

        // Program Flow
        Object value = getValue(condition);
        if (value == null) {
            // No constant propagation past this point
            for (Symbol symbol : constantTable.keySet()) {
                setVariableValue(symbol, null);
            }
            return null;
        }

        // If we can determine the flow of the program, we can make keep the constants and update them based on it
        JmmNode flow;
        if ((Boolean) value) {
            flow = node.getChildren().get(1); // Then
        }
        else {
            flow = node.getChildren().get(2); // Else
        }
        return visit(flow, reports);
    }

    public Object visitWhile(JmmNode node, List<Report> reports) {
        // No constant propagation past this point
        for (Symbol symbol : constantTable.keySet()) {
            setVariableValue(symbol, null);
        }
        return null;
    }
}
