
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

    public TypeVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Add", this::visitArithmeticExpression);
        addVisit("Sub", this::visitArithmeticExpression);
        addVisit("Mul", this::visitArithmeticExpression);
        addVisit("Div", this::visitArithmeticExpression);
    }

    private Object visitArithmeticExpression(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");
        if(methodNode.isPresent()) {
            String signature = getMethodSignature(methodNode.get());
            verifyType(node, signature, reports);
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

    private Boolean verifyType(JmmNode node, String signature, List<Report> reports) {
        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        Type leftType = getExpressionType(leftChild, signature);
        Type rightType = getExpressionType(rightChild, signature);

        Type intType = new Type("int", false);

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

        return true;
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

    private Type getVariableType(String signature, String varName) {
        // TODO: refactor?
        Symbol symbol = null;

        if (symbolTable.methodSymbolTableMap.containsKey(signature)) {
            symbol = (symbol == null) ?  symbolTable.methodSymbolTableMap.get(signature).getLocalVariable(varName) : null; // Check Method Local Variables
            symbol = (symbol == null) ?  symbolTable.methodSymbolTableMap.get(signature).getParameter(varName) : null; // Check Method Parameters
        }

        symbol = (symbol == null) ? symbolTable.getField(varName) : null; // Check global variables

        if (symbol == null)
            return null;
        return symbol.getType();
    }

    private Boolean isOperator(String nodeKind) {
        return nodeKind.equals("Add") || nodeKind.equals("Sub") || nodeKind.equals("Mul") || nodeKind.equals("Div");
    }

    // TODO:Needs refactoring
    private String getMethodSignature(JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main"))
            signature += "(String[])";
        else {
            Optional<JmmNode> optional = node.getChildren().stream().filter(child -> child.getKind().equals("Params")).findFirst();

            signature += "(";

            if (optional.isPresent()) {
                JmmNode paramsNode = optional.get();
                List<String> types = new ArrayList<>();

                for (JmmNode param : paramsNode.getChildren()) {
                    types.add(param.get("type"));
                }

                signature += String.join(", ", types);
            }
            signature += ")";
        }
        return signature;
    }
}