import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConstantVisitor extends AJmmVisitor<List<Report>, Object> {
    private final Map<Symbol, Object> constantTable = new HashMap<>(); // Store all local variables and keep their constant value

    public ConstantVisitor() {
        // TODO: should i add the parameters to the constant table?
        //  i could steal them from the symbol table
        //  i wouldnt need the visit to the vardecl
        addVisit("Method", this::visitMethod);
        addVisit("VarDecl", this::visitVariableDeclaration);
        addVisit("Expression", this::visitExpression);
        addVisit("If", this::visitIf);
        addVisit("While", this::visitWhile);
        addVisit("Return", this::visitReturn);

        setDefaultVisit(this::defaultVisit);
    }

    public Symbol getVariableSymbol(String fieldName) {
        for (Symbol symbol: this.constantTable.keySet()) {
            if (symbol.getName().equals(fieldName)) {
                return symbol;
            }
        }

        return null;
    }

    public Object getVariableValue(Symbol symbol) {
        return symbol == null ? null : this.constantTable.get(symbol);
    }

    public void setVariableValue(Symbol symbol, Object value) {
        this.constantTable.replace(symbol, value);
    }

    public Object constantPropagation(JmmNode node) {
        String kind = node.getKind();
        switch (kind) {
            case "Var":
                String varName = node.get("name");
                Symbol symbol = this.getVariableSymbol(varName);
                Object value = this.getVariableValue(symbol);
                if (value != null) {
                    // TODO: replace the var node for a constant node with this value
                }
                return null;
            default:
                for (JmmNode child : node.getChildren()) {
                    this.constantPropagation(child);
                }
        }
        return null;
    }

    public Object getValue(JmmNode node) {
        Object left = (node.getNumChildren() > 0) ? this.getValue(node.getChildren().get(0)) : null;
        Object right = (node.getNumChildren() > 1) ? this.getValue(node.getChildren().get(1)) : null;

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
                return node.get("value");
            case "False":
                return false;
            case "True":
                return true;
            case "Var":
                return this.getVariableValue(this.getVariableSymbol(node.get("name")));
            default:
                break;
        }
        return null;
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
        // When entering a new method clear from localVariables
        this.constantTable.clear();
        return this.defaultVisit(node, reports);
    }

    public Object visitVariableDeclaration(JmmNode node, List<Report> reports) {
        // When the variable declaration is inside the body of a method
        if (node.getParent().getKind().equals("Body")) {
            String name = node.get("name"), typeName = node.get("type");

            Type type = Utils.getTypeFromString(typeName);
            Symbol symbol = new Symbol(type, name);

            Optional<JmmNode> methodNode = node.getAncestor("Method");
            if (methodNode.isPresent()) {
                // Add it to the constant table
                this.constantTable.put(symbol, null);
            }
        }
        return null;
    } // TODO

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
        // Constant Propagation
        this.constantPropagation(node);

        // Constant Folding
        // TODO

        // Left Node is the Variable
        JmmNode left = node.getChildren().get(0);
        String varName = left.get("name");
        Symbol symbol = this.getVariableSymbol(varName);

        // Right Node is the Value
        JmmNode right = node.getChildren().get(1);
        Object value = this.getValue(right);

        // Modify the Variable Value
        this.setVariableValue(symbol, value);

        return null;
    }

    public Object visitDot(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() > 1) {
            node = node.getChildren().get(1); // Func
            if (node.getNumChildren() > 0) {
                node = node.getChildren().get(0); // Args
                // Constant Propagation
                this.constantPropagation(node);
            }
        }
        return null;
    }

    public Object visitReturn(JmmNode node, List<Report> reports) {
        // Constant Propagation
        this.constantPropagation(node);

        // Constant Folding
        // TODO

        return null;
    }

    public Object visitIf(JmmNode node, List<Report> reports) {
        JmmNode condition = node.getChildren().get(0);
        // Constant Propagation
        this.constantPropagation(condition);

        // Constant Folding
        // TODO

        // Program Flow
        Object value = this.getValue(condition);
        if (value == null) {
            // No constant propagation past this point
            for (Symbol symbol : this.constantTable.keySet()) {
                this.setVariableValue(symbol, null);
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
        for (Symbol symbol : this.constantTable.keySet()) {
            this.setVariableValue(symbol, null);
        }
        return null;
    }
}
