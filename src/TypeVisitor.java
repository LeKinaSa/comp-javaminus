
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
    private final Set<String> importedClasses = new HashSet<>();

    private static final Type intType = new Type("int", false),
            booleanType = new Type("boolean", false),
            intArrayType = new Type("int", true);

    public TypeVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Add", this::visitArithmeticExpression);
        addVisit("Sub", this::visitArithmeticExpression);
        addVisit("Mul", this::visitArithmeticExpression);
        addVisit("Div", this::visitArithmeticExpression);
        addVisit("LessThan", this::visitArithmeticExpression);

        addVisit("And", this::visitBooleanExpression);
        addVisit("Not", this::visitBooleanExpression);
        addVisit("Condition", this::visitBooleanExpression);

        addVisit("Assign", this::visitAssignment);

        addVisit("Var", this::visitVariable);
        addVisit("Size", this::visitSize);
        addVisit("ArrayAccess", this::visitArrayAccess);

        addVisit("Dot", this::visitDot);

        addVisit("Import", this::visitImport);
    }

    private Object visitImport(JmmNode node, List<Report> reports) {
        String name = node.get("module");
        String importedClass;
        if(name.lastIndexOf('.') != -1)
            importedClass = name.substring(name.lastIndexOf('.') + 1);
        else
            importedClass = name;

        importedClasses.add(importedClass);
        return null;
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

        String varName;

        if (leftChild.getKind().equals("Var")) {
            varName = leftChild.get("name");
        }
        else if (leftChild.getKind().equals("ArrayAccess")) {
            JmmNode arrayAccessLeftChild = leftChild.getChildren().get(0);

            if (arrayAccessLeftChild.getKind().equals("Var")) {
                varName = arrayAccessLeftChild.get("name");
            }
            else {
                return null;
            }
        }
        else {
            String message = "Left side of assignment is not a variable.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            return null;
        }

        initializedVariables.add(varName);

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

        JmmNode siblingNode = node.getParent().getChildren().get(1);
        if(!siblingNode.getKind().equals("Func")) { // TO DO: Solve this sibling search? Can we do: importedClasses.contains(name) inside "if" instead?
            if (symbol == null) {
                String message = "Error: symbol " + name + " is undefined.";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
                return null;
            }

            if (!initializedVariables.contains(name)) {
                String message = "Error: variable " + name + " was used without being initialized.";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            }
        }

        return null;
    }
    
    private Object visitSize(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        JmmNode child = node.getChildren().get(0);

        Type childType = getExpressionType(child, signature);

        // Child (array size expression) needs to evaluate to an int
        if (!intType.equals(childType)) {
            String message = "Array size must be an integer";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), message));
        }

        return null;
    }

    private Object visitArrayAccess(JmmNode node, List<Report> reports) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        String signature;
        if (methodNode.isPresent()) {
            signature = Utils.generateMethodSignature(methodNode.get());
        }
        else {
            return null;
        }

        JmmNode var = node.getChildren().get(0);

        if (!var.getKind().equals("Var")) {
            String message = "Not an array";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(var.get("line")), Integer.parseInt(var.get("col")), message));
        }
        else {
            String varName = var.get("name");
            Type varType = getVariableType(signature, varName);
            
            if (varType == null || !varType.equals(intArrayType)) {
                String message = "Invalid array access: variable " + varName + " is not an array";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(var.get("line")), Integer.parseInt(var.get("col")), message));
            }
        }

        JmmNode index = node.getChildren().get(1);
        Type indexType = getExpressionType(index, signature);

        // Child (array index) needs to evaluate to an int
        if (!intType.equals(indexType)) {
            String message = "Invalid index type for array";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), message));
        }

        return null;
    }

    public Object visitDot(JmmNode node, List<Report> reports) {
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

        if (rightChild.getKind().equals("Length") && (leftType == null || !leftType.isArray())) {
            String message = "Builtin \"length\" can only be used with arrays.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(leftChild.get("line")), Integer.parseInt(leftChild.get("col")), message));
        } else if(rightChild.getKind().equals("Func")) {
            String className = symbolTable.getClassName();
            String extendsName = symbolTable.getSuper();
            String funcName = rightChild.get("name");

            if(leftChild.getKind().equals("Var") && initializedVariables.contains(leftChild.get("name"))) {
                Type varType = getVariableType(signature, leftChild.get("name"));
                if(varType.getName().equals(className)) { // Same class
                    if (extendsName == null) {
                        String calledFuncSignature = getNodeFunctionSignature(signature, rightChild);
                        if (!symbolTable.methodSymbolTableMap.containsKey(calledFuncSignature)) {
                            String message = "Invoqued method \"" + funcName + "\" does not exist inside class.";
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                        }
                    }
                }
            } else if(leftChild.getKind().equals("This")) {
                if (extendsName == null) {
                    String calledFuncSignature = getNodeFunctionSignature(signature, rightChild);
                    if (!symbolTable.methodSymbolTableMap.containsKey(calledFuncSignature)) {
                        String message = "Invoqued method \"" + funcName + "\" does not exist inside class.";
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                    }
                }
            } else { // Imported class
                if (!importedClasses.contains(leftChild.get("name"))) {
                    String message = "Class \"" + leftChild.get("name") + "\" not included in imports.";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                }
            }
        }

        return null;
    }



    public Type getExpressionType(JmmNode node, String methodSignature) {
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
            case "ArrayAccess": // TODO: if args array in main will be used, change this
                return new Type("int", false);
            case "This":
                if (parentNode.getKind().equals("Dot")) { // this.method
                    return getReturnType(methodSignature, parentNode.getChildren().get(1));
                }
                break;
            case "Var":
                return getVariableType(methodSignature, node.get("name")); // Variable
            case "Dot": {
                JmmNode leftChild = node.getChildren().get(0);
                JmmNode rightChild = node.getChildren().get(1);

                if (rightChild.getKind().equals("Length")) {
                    return new Type("int", false);
                } else if (rightChild.getKind().equals("Func")) {
                    // Var is same class or This -> get type | else assume int (unknown class)
                    if(leftChild.getKind().equals("This") || leftChild.get("name").equals(symbolTable.getClassName()))
                        return getExpressionType(rightChild, methodSignature);
                    else
                        return new Type("int", false);
                }

                break;
            }
            case "Func":
                return getReturnType(methodSignature, node);
            case "NewArray":
                return new Type("int", true);
            case "NewInstance":
                return new Type(node.get("class"), false);
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