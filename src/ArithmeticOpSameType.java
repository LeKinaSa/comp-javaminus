
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
        Boolean result = (leftChild.getKind().equals("int") || leftChild.getKind().equals("Expression"))
                && (rightChild.getKind().equals("int") || rightChild.getKind().equals("Expression"));

        if(!result) {
            Symbol leftChildSymbol = null, rightChildSymbol = null;
            if(leftChild.getKind().equals("Var"))
                leftChildSymbol = getVariableType(signature, leftChild.get("name"));
            else if(rightChild.getKind().equals("Var"))
                rightChildSymbol = getVariableType(signature, rightChild.get("name"));

            // Childs (Both variable and with the same type)
            if(leftChildSymbol != null && rightChildSymbol != null) {
                return leftChildSymbol.getType().equals(rightChildSymbol.getType());
            }
            // Childs (this.method - returnType) -> LEFT
            if(leftChild.getKind().equals("Dot")) {
                List<JmmNode> leftChildChildren = leftChild.getChildren();
                JmmNode leftChildThis = leftChildChildren.get(0);

                if(leftChildThis.getKind().equals("This")) {
                    JmmNode leftChildFunction = leftChildChildren.get(1);
                    if(rightChildSymbol != null)
                        return rightChildSymbol.getType().getName().equals(getFunctionType(leftChildFunction.get("name")));
                }
            }

            // Childs (this.method - returnType) -> RIGHT
            if(rightChild.getKind().equals("Dot")) {
                List<JmmNode> rightChildChildren = rightChild.getChildren();
                JmmNode rightChildThis = rightChildChildren.get(0);

                if(rightChildThis.getKind().equals("This")) {
                    JmmNode rightChildFunction = rightChildChildren.get(1);
                    if(leftChildSymbol != null)
                        return leftChildSymbol.getType().getName().equals(getFunctionType(rightChildFunction.get("name")));
                }
            }
            return true;

            /*else {
                if(leftChildSymbol != null) {
                    String message = "Variable " + leftChild.get("name") + " not declared";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
                }
                if(rightChildSymbol != null) {
                    String message = "Variable " + rightChild.get("name") + " not declared";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
                }
                return false;
            }*/
        } else
            return true;
    }

    private Type getFunctionType(String functionName) {
        /*String signature = symbolTable.getFunctionSignature(functionName)
        if(symbolTable.methodSymbolTableMap.containsKey(signature))
            return symbolTable.methodSymbolTableMap.get(signature).returnType;
        else return null;*/
        return null;
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

    /*private Object visitClass(JmmNode node, List<Report> reports) {
        symbolTable.setClassName(node.get("name"));
        symbolTable.setSuperclassName(node.get("extends"));
        return null;
    }

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

    private Object visitMethod(JmmNode node, List<Report> reports) {
        String signature = getMethodSignature(node);
        String name = node.get("name");
        Type returnType;

        if (name.equals("main")) {
            returnType = new Type("void", false);
        } else {
            String returnTypeName = node.get("returnType");
            returnType = new Type(returnTypeName, returnTypeName.endsWith("[]"));
        }

        if (!symbolTable.methods.add(signature)) {
            String message = "Method with signature " + signature + " already exists";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        }

        symbolTable.methodSymbolTableMap.put(signature, new MethodSymbolTable(returnType, new HashSet<>(), new HashSet<>()));
        return null;
    }

    private Object visitVariables(JmmNode node, List<Report> reports) {
        String name = node.get("name");
        Type varType = new Type(node.get("type"), node.get("type").endsWith("[]"));
        Symbol symbol = new Symbol(varType, name);
        if(node.getParent().getKind().equals("Class")) { // Global Variables
            if(!symbolTable.fields.add(symbol)) {
                String message = "Variable " + name + " in class " + node.getParent().get("name") + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
            }
        } else if(node.getParent().getKind().equals("Body")) { // Method Local Variable
            Optional<JmmNode> methodNode = node.getAncestor("Method");
            if(methodNode.isPresent()) {
                String signature = getMethodSignature(methodNode.get());
                if(!symbolTable.methodSymbolTableMap.get(signature).addLocalVariable(symbol)) {
                    String message = "Variable " + name + " in method with signature " + signature + " already exists";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
                }
            }
        }
        return null;
    }

    private Object visitParameters(JmmNode node, List<Report> reports) {
        String name = node.get("name");
        Type varType = new Type(node.get("type"), node.get("type").endsWith("[]"));
        Symbol symbol = new Symbol(varType, name);
        Optional<JmmNode> methodNode = node.getAncestor("Method");
        if(methodNode.isPresent()) {
            String signature = getMethodSignature(methodNode.get());
            if(!symbolTable.methodSymbolTableMap.get(signature).addParameter(symbol)) {
                String message = "Parameter " + name + " in method with signature " + signature + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
            }
        }
        return null;
    }*/
}