
import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

class ArithmeticOpTypeVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    public ArithmeticOpTypeVisitor(JMMSymbolTable symbolTable) {
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
            if(parentNode.getKind().equals("Dot")) { // this.method
                return getFunctionType(methodSignature, parentNode.getChildren().get(1));
            }
        case "Var":
            if(parentNode.getKind().equals("Dot")) { // array.length
                Type varType = getVariableType(methodSignature, parentNode.getChildren().get(0).get("name"));
                if(varType != null && varType.getName().equals("int[]"))
                    return new Type("int", false);
                return varType;
            }
            return getVariableType(methodSignature, node.get("name")); // Variable
        case "Dot":
            return getExpressionType(node.getChildren().get(0), methodSignature);
        default:
            break;
        }
        return null;
    }

    private Boolean verifyType(JmmNode node, String signature, List<Report> reports) {
        // List<JmmNode> children = node.getChildren();
        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        Type leftType = getExpressionType(leftChild, signature);
        Type rightType = getExpressionType(rightChild, signature);

        System.out.println("Left type: " + leftType);
        System.out.println("Right type: " + rightType);
        // Childs (Both variables with the same type (int))
        if(leftType == null || rightType == null || !leftType.equals(rightType)) {
            System.out.println("SEMANTIC ERROR!");
        }
        return true;

        // Childs (Int or Expression or Add, Sub, Mul, Div)
        /*Boolean result = (leftChild.getKind().equals("Int") || leftChild.getKind().equals("Expression") || isOperator(leftChild.getKind()))
                && (rightChild.getKind().equals("Int") || rightChild.getKind().equals("Expression") || isOperator(rightChild.getKind()));

        if(!result) {
            String leftChildType = (leftChild.getKind().equals("Int")) ? "int" : null;
            String rightChildType = (rightChild.getKind().equals("Int")) ? "int" : null;

            // Childs (this.method or array.length) -> LEFT
            if(leftChild.getKind().equals("Dot")) {
                List<JmmNode> leftChildChildren = leftChild.getChildren();
                JmmNode leftChildNode = leftChildChildren.get(0);

                if(leftChildNode.getKind().equals("This")) { // this.method
                    JmmNode leftChildFunction = leftChildChildren.get(1);
                    leftChildType = getFunctionType(signature, leftChildFunction).getName();
                } else if(leftChildNode.getKind().equals("Var") && leftChildChildren.get(1).getKind().equals("Length")) { // array.method
                    String varType = getVariableType(signature, leftChildChildren.get(0).get("name")).getName();
                    if(varType != null) {
                        if(varType.equals("int[]"))
                            leftChildType = "int";
                        else
                            leftChildType = varType;
                    }
                }
            } else if(leftChild.getKind().equals("Var")) // Variable
                leftChildType = getVariableType(signature, leftChild.get("name")).getName();

            // Childs (this.method or array.length) -> RIGHT
            if(rightChild.getKind().equals("Dot")) {
                List<JmmNode> rightChildChildren = rightChild.getChildren();
                JmmNode rightChildNode = rightChildChildren.get(0);

                if(rightChildNode.getKind().equals("This")) { // this.method
                    JmmNode rightChildFunction = rightChildChildren.get(1);
                    rightChildType = getFunctionType(signature, rightChildFunction).getName();
                } else if(rightChildNode.getKind().equals("Var") && rightChildChildren.get(1).getKind().equals("Length")) { // array.method
                    String varType = getVariableType(signature, rightChildChildren.get(0).get("name")).getName();
                    if(varType != null) {
                        if(varType.equals("int[]"))
                            rightChildType = "int";
                        else
                            rightChildType = varType;
                    }
                }
            } else if(rightChild.getKind().equals("Var")) // Variable
                rightChildType = getVariableType(signature, rightChild.get("name")).getName();

            System.out.println("Left type: " + leftChildType);
            System.out.println("Right type: " + rightChildType);
            // Childs (Both variables with the same type (int))
            if(leftChildType != null && rightChildType != null && leftChildType.equals(rightChildType) && leftChildType.equals("int"))
                return true;
            else {
                checkReports(reports, leftChild, leftChildType);
                checkReports(reports, rightChild, rightChildType);
                return false;
            }
        }

        return true;*/
    }

    private void checkReports(List<Report> reports, JmmNode child, String childType) {
        String message = null;
        if(child.getKind().equals("Dot") && child.getChildren().get(0).getKind().equals("Var"))
            message = "Variable " + child.getChildren().get(0).get("name") + " w/incorrect type!";
        else if(child.getKind().equals("Dot") && child.getChildren().get(0).getKind().equals("This"))
            message = "Function " + child.getChildren().get(1).get("name") + " w/incorrect type!";
        else if(childType == null)
            message = "Variable " + child.get("name") + " not declared!";
        else if(child.getKind().equals("Var"))
            message = "Variable " + child.get("name") + " w/incorrect type!";
        else  // Defensive programming
            message = "Unknown error occured!";

        System.out.println(message);
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
    }

    private String getNodeFunctionSignature(String methodSignature, JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main"))
            signature += "(String[])";
        else {
            JmmNode argsNode = node.getChildren().get(0);
            if(argsNode != null) {
                List<JmmNode> expressionNodes = argsNode.getChildren();
                List<String> types = new ArrayList<>();

                signature += "(";

                for(JmmNode expressionNode: expressionNodes) {
                    String varName = expressionNode.getChildren().get(0).get("name");
                    String varType = getVariableType(methodSignature, varName).getName();
                    types.add(varType);
                }

                signature += String.join(", ", types) + ")";
            }
        }
        return signature;
    }

    private Type getFunctionType(String methodSignature, JmmNode funcNode) {
        String newSignature = getNodeFunctionSignature(methodSignature, funcNode);
        if(symbolTable.methodSymbolTableMap.containsKey(newSignature))
            return symbolTable.methodSymbolTableMap.get(newSignature).returnType;
        else return null;
    }

    private Type getVariableType(String signature, String varName) {
        Symbol symbol = null;
        if(symbolTable.methodSymbolTableMap.containsKey(signature)) {
            symbol = (symbol == null) ?  symbolTable.methodSymbolTableMap.get(signature).getLocalVariable(varName) : null; // Check Method Local Variables
            symbol = (symbol == null) ?  symbolTable.methodSymbolTableMap.get(signature).getParameter(varName) : null; // Check Method Parameters
        }
        symbol = (symbol == null) ? symbolTable.getField(varName) : null; // Check global variables
        if(symbol == null)
            return null;
        return symbol.getType();
    }

    private Boolean isOperator(String nodeKind) {
        return nodeKind.equals("Add") || nodeKind.equals("Sub") || nodeKind.equals("Mul") || nodeKind.equals("Div");
    }

    // Needs refactoring
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