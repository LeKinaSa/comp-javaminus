
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

class ArithmeticOpSameType extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    public ArithmeticOpSameType(JMMSymbolTable symbolTable) {
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

    private Boolean verifyType(JmmNode node, String signature, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftChild = children.get(0);
        JmmNode rightChild = children.get(1);

        // Childs (Int or Expression)
        Boolean result = (leftChild.getKind().equals("int") || leftChild.getKind().equals("Expression") || isOperator(leftChild.getKind()))
                && (rightChild.getKind().equals("int") || rightChild.getKind().equals("Expression") || isOperator(rightChild.getKind()));

        if(!result) {
            String leftChildType = null, rightChildType = null;

            // Childs (this.method or array.length) -> LEFT
            if(leftChild.getKind().equals("Dot")) {
                List<JmmNode> leftChildChildren = leftChild.getChildren();
                JmmNode leftChildNode = leftChildChildren.get(0);

                if(leftChildNode.getKind().equals("This")) { // this.method
                    JmmNode leftChildFunction = leftChildChildren.get(1);
                    leftChildType = getFunctionType(signature, leftChildFunction);
                } else if(leftChildNode.getKind().equals("Var") && leftChildChildren.get(1).getKind().equals("Length")) { // array.method
                    if(getVariableType(signature, leftChildChildren.get(0).get("name")).equals("int[]"))
                        leftChildType = "int";
                }
            } else if(leftChild.getKind().equals("Var")) // Variable
                leftChildType = getVariableType(signature, leftChild.get("name")).getType().getName();

            // Childs (this.method or array.length) -> RIGHT
            if(rightChild.getKind().equals("Dot")) {
                List<JmmNode> rightChildChildren = rightChild.getChildren();
                JmmNode rightChildNode = rightChildChildren.get(0);

                if(rightChildNode.getKind().equals("This")) { // this.method
                    JmmNode rightChildFunction = rightChildChildren.get(1);
                    rightChildType = getFunctionType(signature, rightChildFunction);
                } else if(rightChildNode.getKind().equals("Var") && rightChildChildren.get(1).getKind().equals("Length")) { // array.method
                    if(getVariableType(signature, rightChildChildren.get(0).get("name")).equals("int[]"))
                        rightChildType = "int";
                }
            } else if(rightChild.getKind().equals("Var")) // Variable
                rightChildType = getVariableType(signature, rightChild.get("name")).getType().getName();

            // Childs (Both variable and with the same type (int))
            if(leftChildType != null && rightChildType != null)
                return leftChildType.equals(rightChildType) && leftChildType.equals("int");
            else {
                checkReports(reports, leftChild, leftChildType);
                checkReports(reports, rightChild, rightChildType);
                return false;
            }
        }

        return true;
    }

    private void checkReports(List<Report> reports, JmmNode child, String childType) {
        if(childType == null) {
            String message = "Variable " + child.get("name") + " not declared";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        } else {
            String message = "Variable " + child.get("name") + " with incorrect type";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        }
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
                    Symbol varSymbol = getVariableType(methodSignature, varName);
                    types.add(varSymbol.getType().getName());
                }

                signature += String.join(", ", types) + ")";
            }
        }
        return signature;
    }

    private String getFunctionType(String methodSignature, JmmNode funcNode) {
        String newSignature = getNodeFunctionSignature(methodSignature, funcNode);
        if(symbolTable.methodSymbolTableMap.containsKey(newSignature))
            return symbolTable.methodSymbolTableMap.get(newSignature).returnType.getName();
        else return null;
    }

    private Symbol getVariableType(String signature, String varName) {
        Symbol symbol = null;
        if(symbolTable.methodSymbolTableMap.containsKey(signature)) {
            symbol = symbolTable.methodSymbolTableMap.get(signature).getLocalVariable(varName);
            if (symbol == null) // Check Method Local Variables
                symbol = symbolTable.methodSymbolTableMap.get(signature).getParameter(varName); // Check Method Parameters
        }
        if (symbol == null)
            symbol = symbolTable.getField(varName); // Check global variables
        return symbol;
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