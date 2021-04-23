
import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

class TypeVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    private final Set<String> initializedVariables = new HashSet<>();

    public TypeVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Add", this::visitArithmeticExpression);
        addVisit("Sub", this::visitArithmeticExpression);
        addVisit("Mul", this::visitArithmeticExpression);
        addVisit("Div", this::visitArithmeticExpression);
        addVisit("LessThan", this::visitArithmeticExpression);

        addVisit("And", this::visitBooleanExpression);
        addVisit("Not", this::visitBooleanExpression);
        addVisit("Assign", this::visitAssignment);

        addVisit("Var", this::visitVariable);
    }

    private Object visitArithmeticExpression(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        final Type intType = new Type("int", false);

        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        Type leftType = getExpressionType(leftChild, signature);
        Type rightType = getExpressionType(rightChild, signature);

        System.out.println("Left type: " + leftType);
        System.out.println("Right type: " + rightType);

        // Childs (Both variables with the same type (int))
        if (!intType.equals(leftType)) {
            String message = "Invalid type for operation " + node.getKind();
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(leftChild.get("line")), Integer.parseInt(leftChild.get("col")), message));
        }

        if (!intType.equals(rightType)) {
            String message = "Invalid type for operation " + node.getKind();
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
        }

        return null;
    }

    private Object visitBooleanExpression(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        final Type booleanType = new Type("boolean", false);

        JmmNode firstChild = node.getChildren().get(0);
        Type firstType = getExpressionType(firstChild, signature);

        if (!booleanType.equals(firstType)) {
            String message = "Invalid type for operation " + node.getKind();
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), message));
        }
        
        if (node.getNumChildren() == 2) { // And
            JmmNode secondChild = node.getChildren().get(1);
            Type secondType = getExpressionType(secondChild, signature);

            if (!booleanType.equals(secondType)) {
                String message = "Invalid type for operation " + node.getKind();
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(secondChild.get("line")), Integer.parseInt(secondChild.get("col")), message));
            }
        }

        return null;
    }

    public Object visitAssignment(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        Type leftType = getExpressionType(leftChild, signature);
        Type rightType = getExpressionType(rightChild, signature);

        initializedVariables.add(leftChild.get("name"));

        if (leftType == null || !leftType.equals(rightType)) {
            String message = "Type mismatch in assignment";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    public Object visitVariable(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        String name = node.get("name");
        Symbol symbol = symbolTable.getSymbol(signature, name);

        if (symbol == null) {
            String message = "Error: symbol " + name + " is undefined.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            return null;
        }

        if (!initializedVariables.contains(name)) {
            String message = "Error: variable " + name + " was used without being initialized.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    public Type getExpressionType(JmmNode node, String methodSignature) {
        // TODO: New
        JmmNode parentNode = node.getParent();

        switch (node.getKind()) {
        case "And":
        case "LessThan":
        case "Not":
        case "True":
        case "False":
            return new Type("boolean", false);
        case "Expression":
            if (node.getNumChildren() == 1) {
                return getExpressionType(node.getChildren().get(0), methodSignature);
            }
            break;
        case "Add":
        case "Sub":
        case "Mul":
        case "Div":
        case "Int":
            return new Type("int", false);
        case "This":
            if (parentNode.getKind().equals("Dot")) { // this.method
                return getReturnType(methodSignature, parentNode.getChildren().get(1));
            }
            break;
        case "Var":
            return getVariableType(methodSignature, node.get("name")); // Variable
        case "Dot": {
            JmmNode rightChild = node.getChildren().get(1);

            if (rightChild.getKind().equals("Length")) {
                Type type = getVariableType(methodSignature, node.getChildren().get(0).get("name"));

                if (type != null && type.isArray()) {
                    return new Type(type.getName(), false);
                }

                return type;
            }
            else if (rightChild.getKind().equals("Func")) {
                return getExpressionType(rightChild, methodSignature);
            }
            
            break;
        }
        case "Func":
            return getReturnType(methodSignature, node);
        default:
            break;
        }

        return null;
    }

    private String getNodeFunctionSignature(String methodSignature, JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main"))
            signature += "(String[])";
        else {
            JmmNode argsNode = node.getChildren().get(0);
            if (argsNode != null) {
                List<JmmNode> expressionNodes = argsNode.getChildren();
                List<String> types = new ArrayList<>();

                signature += "(";

                for (JmmNode expressionNode: expressionNodes) {
                    String varName = expressionNode.getChildren().get(0).get("name");
                    Type varType = getVariableType(methodSignature, varName);

                    types.add((varType.isArray()) ? varType.getName().concat("[]") : varType.getName());
                }

                signature += String.join(", ", types) + ")";
            }
        }
        return signature;
    }

    private Type getReturnType(String methodSignature, JmmNode funcNode) {
        String newSignature = getNodeFunctionSignature(methodSignature, funcNode);

        if (symbolTable.methodSymbolTableMap.containsKey(newSignature)) {
            return symbolTable.methodSymbolTableMap.get(newSignature).returnType;
        }

        return null;
    }

    private Type getVariableType(String signature, String name) {
        Symbol symbol = symbolTable.getSymbol(signature, name);
        if (symbol == null) return null;
        return symbol.getType();
    }
}