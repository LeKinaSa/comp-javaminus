
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;
import java.util.stream.Collectors;

class TypeVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    private final Set<String> initializedVariables = new HashSet<>();
    private final Set<String> importedClasses;

    private static final Type intType = new Type("int", false),
            booleanType = new Type("boolean", false),
            intArrayType = new Type("int", true);

    public TypeVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        importedClasses = symbolTable.getImports().stream().map(Utils::getImportedClass).collect(Collectors.toSet());

        addVisit("VarDecl", this::visitVariableDeclaration);

        addVisit("Expression", this::visitExpression);

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
        addVisit("This", this::visitThis);
        addVisit("Size", this::visitSize);
        addVisit("ArrayAccess", this::visitArrayAccess);

        addVisit("Dot", this::visitDot);
    }

    private Object visitVariableDeclaration(JmmNode node, List<Report> reports) {
        Set<String> primitiveTypes = Set.of("int", "boolean", "int[]");
        String type = node.get("type");

        if (!primitiveTypes.contains(type) && !importedClasses.contains(type) && !type.equals(symbolTable.getClassName())) {
            String message = "The type \"" + type + "\" is missing";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    private Object visitExpression(JmmNode node, List<Report> reports) {
        JmmNode child = node.getChildren().get(0);
        Set<String> allowedExpressions = Set.of("Assign", "NewInstance");

        if (node.getParent().getKind().equals("Statement") && !allowedExpressions.contains(child.getKind())
            && !(child.getKind().equals("Dot") && child.getChildren().get(1).getKind().equals("Func"))) {
            String message = "Not a valid statement.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

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

        Type leftType = Utils.getExpressionType(symbolTable, leftChild, signature);
        Type rightType = Utils.getExpressionType(symbolTable, rightChild, signature);

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
        Type firstType = Utils.getExpressionType(symbolTable, firstChild, signature);

        if (!booleanType.equals(firstType)) {
            String message = "Invalid type for operation " + node.getKind();
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), message));
        }
        
        if (node.getNumChildren() == 2) { // And
            JmmNode secondChild = node.getChildren().get(1);
            Type secondType = Utils.getExpressionType(symbolTable, secondChild, signature);

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

        Type leftType = Utils.getExpressionType(symbolTable, leftChild, signature);
        Type rightType = Utils.getExpressionType(symbolTable, rightChild, signature);

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

        // TODO: This is shown even if right side is an undefined variable
        if (leftType == null || !leftType.equals(rightType)) {
            String message = "Type mismatch in assignment";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    public Object visitVariable(JmmNode node, List<Report> reports) {
        Optional<JmmNode> optional = node.getAncestor("Method");
        JmmNode methodNode;

        String signature;
        if (optional.isPresent()) {
            methodNode = optional.get();
            signature = Utils.generateMethodSignature(methodNode);
        }
        else {
            return null;
        }

        String name = node.get("name");
        Symbol symbol = symbolTable.getSymbol(signature, name);

        if (symbolTable.methodSymbolTableMap.get(signature).getLocalVariable(name) == null
                && symbolTable.getField(name) != null && methodNode.get("name").equals("main")) {
            String message = "The class field \"" + name + "\" cannot be accessed from a static context.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        JmmNode parentNode = node.getParent();
        if (parentNode.getNumChildren() == 2 && parentNode.getChildren().get(1).getKind().equals("Func")) {
            // Don't verify imported variables
            Type varType = Utils.getVariableType(symbolTable, signature, name);

            if (varType == null) {
                return null;
            }
            else if (varType.getName().equals("int") || varType.getName().equals("boolean")) {
                String message = "Literal \"" + name + "\" cannot call a method.";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            }
        }

        if (symbol == null) {
            String message = "Error: symbol " + name + " is undefined.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            return null;
        }

        if (!initializedVariables.contains(name) && symbolTable.methodSymbolTableMap.get(signature).localVariables.contains(symbol)) {
            // Only local variables have to be checked for initialization (parameters are already initialized and we
            // cannot know if fields are initialized
            String message = "Error: variable " + name + " was used without being initialized.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    private Object visitThis(JmmNode node, List<Report> reports) {
        Optional<JmmNode> optional = node.getAncestor("Method");

        if (optional.isPresent()) {
            JmmNode methodNode = optional.get();
            if (methodNode.get("name").equals("main")) {
                String message = "\"this\" cannot be referenced from a static context.";
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

        Type childType = Utils.getExpressionType(symbolTable, child, signature);

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
            Type varType = Utils.getVariableType(symbolTable, signature, varName);
            
            if (varType == null || !varType.equals(intArrayType)) {
                String message = "Invalid array access: variable " + varName + " is not an array";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(var.get("line")), Integer.parseInt(var.get("col")), message));
            }
        }

        JmmNode index = node.getChildren().get(1);
        Type indexType = Utils.getExpressionType(symbolTable, index, signature);

        // Child (array index) needs to evaluate to an int
        if (!intType.equals(indexType)) {
            String message = "Invalid index type for array";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), message));
        }

        return null;
    }

    public Object visitDot(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        if (signature == null) {
            return null;
        }

        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        Type leftType = Utils.getExpressionType(symbolTable, leftChild, signature);

        if (rightChild.getKind().equals("Length") && (leftType == null || !leftType.isArray())) {
            String message = "Builtin \"length\" can only be used with arrays.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(leftChild.get("line")), Integer.parseInt(leftChild.get("col")), message));
        }
        else if (rightChild.getKind().equals("Func")) {
            String className = symbolTable.getClassName();
            String extendsName = symbolTable.getSuper();
            String funcName = rightChild.get("name");

            if (leftChild.getKind().equals("Var") && initializedVariables.contains(leftChild.get("name"))) {
                Type varType = Utils.getVariableType(symbolTable, signature, leftChild.get("name"));
                if (varType.getName().equals(className)) { // Same class
                    // TODO: REFACTOR
                    if (extendsName == null) {
                        String calledFuncSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);
                        String methodName = calledFuncSignature.substring(0, calledFuncSignature.indexOf("("));
                        if (!checkMethodExistence(methodName)) {
                            String message = "Invoked method \"" + funcName + "\" does not exist inside class.";
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                        }
                        else {
                            List<String> passedArgs = getFunctionPassedArguments(calledFuncSignature);
                            verifyCalledMethodSignature(calledFuncSignature, methodName, passedArgs, rightChild, reports);
                        }
                    }
                }
            }
            else if (leftChild.getKind().equals("This")) {
                if (extendsName == null) {
                    String calledFuncSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);
                    String methodName = calledFuncSignature.substring(0, calledFuncSignature.indexOf("("));
                    // TODO: REFACTOR
                    if (!checkMethodExistence(methodName)) {
                        String message = "Invoked method \"" + funcName + "\" does not exist inside class.";
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                    }
                    else {
                        List<String> passedArgs = getFunctionPassedArguments(calledFuncSignature);
                        verifyCalledMethodSignature(calledFuncSignature, methodName, passedArgs, rightChild, reports);
                    }
                }
            }
            else if (leftChild.getKind().equals("NewInstance") && leftChild.get("class").equals(symbolTable.getClassName())) {
                String calledFuncSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);
                String methodName = calledFuncSignature.substring(0, calledFuncSignature.indexOf("("));
                // TODO: REFACTOR
                if (!checkMethodExistence(methodName)) {
                    String message = "Invoked method \"" + funcName + "\" does not exist inside class.";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                }
                else {
                    List<String> passedArgs = getFunctionPassedArguments(calledFuncSignature);
                    verifyCalledMethodSignature(calledFuncSignature, methodName, passedArgs, rightChild, reports);
                }
            }
            else { // Imported class or new Class() (with same filename)
                if (leftType != null && leftType.getName().equals(symbolTable.getClassName())) {
                    String calledFuncSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);
                    String methodName = calledFuncSignature.substring(0, calledFuncSignature.indexOf("("));
                    List<String> passedArgs = getFunctionPassedArguments(calledFuncSignature);
                    verifyCalledMethodSignature(calledFuncSignature, methodName, passedArgs, rightChild, reports);
                }
                else if (leftChild.getKind().equals("NewInstance") && !importedClasses.contains(leftChild.get("class")) || !leftChild.getKind().equals("NewInstance") && !importedClasses.contains(leftChild.get("name"))) {
                    String message = "Class \"" + ((leftChild.getKind().equals("NewInstance")) ? leftChild.get("class") : leftChild.get("name")) + "\" not included in imports.";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(rightChild.get("line")), Integer.parseInt(rightChild.get("col")), message));
                }
            }
        }

        return null;
    }

    public Boolean checkMethodExistence(String methodName) {
        for (String methodSignature : symbolTable.getMethodsSymbolTable().keySet()) {
            String name = methodSignature.substring(0, methodSignature.indexOf("("));
            if (name.equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    public void verifyCalledMethodSignature(String calledFuncSignature, String methodName, List<String> passedArgs, JmmNode funcNode, List<Report> reports) {
        int numSupposedMethodArgs = 0;
        int numCommonArgs = 0;

        int numberOfPassedArgs = passedArgs.size();
        for (String methodSignature: symbolTable.getMethodsSymbolTable().keySet()) {
            String name = methodSignature.substring(0, methodSignature.indexOf("("));

            if (calledFuncSignature.equals(methodSignature))
                return;
            else if (name.equals(methodName)) {
                List<String> methodArgs = getFunctionPassedArguments(methodSignature);
                int numberOfMethodArgs = methodArgs.size();
                int numArgs = Math.min(numberOfPassedArgs, numberOfMethodArgs);
                int auxNumCommonArgs = 0;

                for (int i = 0; i < numArgs; i++) {
                    if (passedArgs.get(i).equals(methodArgs.get(i))) {
                        auxNumCommonArgs++;
                    }
                }

                if (auxNumCommonArgs > numCommonArgs || numCommonArgs == 0) {
                    numCommonArgs = auxNumCommonArgs;
                    numSupposedMethodArgs = numberOfMethodArgs;
                }
            }
        }

        String message;
        if (numCommonArgs < numberOfPassedArgs && numSupposedMethodArgs == numberOfPassedArgs) {
            message = "Incorrect parameter types in method \"" + methodName + "\".";
        }
        else {
            message = "Incorrect number of arguments in method \"" + methodName + "\".";
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(funcNode.get("line")), Integer.parseInt(funcNode.get("col")), message));
    }

    public List<String> getFunctionPassedArguments(String methodSignature) {
        String argsWithCommas = methodSignature.substring(methodSignature.indexOf("(") + 1, methodSignature.indexOf(")"));
        return Arrays.asList(argsWithCommas.split(", "));
    }
}