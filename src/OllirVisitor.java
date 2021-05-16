
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;
import java.util.stream.Collectors;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    private final StringBuilder ollirBuilder;
    private final JMMSymbolTable symbolTable;
    private final StringBuilder tabs = new StringBuilder(); // Improves OLLIR code formatting

    private final Map<String, Integer> tempVariablesMap = new HashMap<>(),
            ifStatementsMap = new HashMap<>(),   // Number of if statements in a method
            whileStatementMap = new HashMap<>(); // Number of while statements in a method

    private static final Map<String, String> arithmeticOpMap = Map.of("Add", "+", "Sub", "-", "Mul", "*", "Div", "/", "LessThan", "<");
    private static final Map<String, String> booleanOpMap = Map.of("LessThan", "<", "And", "&&", "Not", "!");

    private final Set<String> importedClasses;

    public OllirVisitor(StringBuilder ollirBuilder, SymbolTable symbolTable) {
        this.ollirBuilder = ollirBuilder;
        this.symbolTable = (JMMSymbolTable) symbolTable;
        importedClasses = symbolTable.getImports().stream().map(Utils::getImportedClass).collect(Collectors.toSet());

        addVisit("Import", this::visitImport);

        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
        addVisit("Expression", this::visitExpression);

        addVisit("VarDecl", this::visitVariableDeclaration);

        addVisit("If", this::visitIf);
        addVisit("While", this::visitWhile);

        addVisit("Add", this::visitArithmeticOp);
        addVisit("Sub", this::visitArithmeticOp);
        addVisit("Mul", this::visitArithmeticOp);
        addVisit("Div", this::visitArithmeticOp);

        addVisit("LessThan", this::visitArithmeticOp);
        addVisit("And", this::visitBooleanOp);
        addVisit("Not", this::visitBooleanOp);

        addVisit("Int", this::visitInt);
        addVisit("False", this::visitBool);
        addVisit("True", this::visitBool);
        addVisit("Var", this::visitVariable);
        addVisit("Assign", this::visitAssignment);
        addVisit("Statement", this::visitStatement);

        addVisit("Dot", this::visitDot);
        addVisit("NewInstance", this::visitNewInstance);
        addVisit("NewArray", this::visitNewArray);
        addVisit("Return", this::visitReturn);

        addVisit("ArrayAccess", this::visitArrayAccess);

        setDefaultVisit(this::defaultVisit);
    }

    private void addTab() {
        tabs.append('\t');
    }

    private void removeTab() {
        if (tabs.length() > 0) {
            tabs.deleteCharAt(tabs.length() - 1);
        }
    }

    private void incrementTempVariable(String signature) {
        if (!tempVariablesMap.containsKey(signature)) return;

        String name;
        do {
            tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
            name = "t" + tempVariablesMap.get(signature);
        } while (symbolTable.getSymbol(signature, name) != null);

        // There is no local variable or field with the same name as the temp variable
    }

    private StringBuilder lineWithTabs() {
        return ollirBuilder.append(tabs);
    }

    private void buildConstructor(String className) {
        lineWithTabs().append(".construct ").append(className).append("().V {\n");
        addTab();
        lineWithTabs().append("invokespecial(this, \"<init>\").V;\n");
        lineWithTabs().append("ret.V;\n");
        removeTab();
        lineWithTabs().append("}\n");
    }

    public String defaultVisit(JmmNode node, List<Report> reports) {
        // Default visit is a simple pre-order visit
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }

        return null;
    }

    public String visitImport(JmmNode node, List<Report> reports) {
        ollirBuilder.append("import ").append(node.get("module")).append(";\n");
        return null;
    }

    public String visitClass(JmmNode node, List<Report> reports) {
        String className = node.get("name");

        ollirBuilder.append(className);
        Optional<String> extendClass = node.getOptional("extends");
        if (extendClass.isPresent()) {
            ollirBuilder.append(" extends ").append(node.get("extends"));
        }
        ollirBuilder.append(" {\n");
        
        addTab();

        boolean constructorCreated = false;

        // Build OLLIR code for the children of the Class node
        for (JmmNode child : node.getChildren()) {
            if (!constructorCreated && child.getKind().equals("Method")) {
                // Place constructor before the first method (after the last field)
                buildConstructor(className);
                constructorCreated = true;
            }
            
            visit(child, reports);
        }

        if (!constructorCreated) {
            buildConstructor(className);
        }

        removeTab();
        ollirBuilder.append("}\n");

        return null;
    }

    public String convertType(Type type) {
        String name = type.getName();

        switch (name) {
            case "int":
                name = "i32";
                break;
            case "boolean":
                name = "bool";
                break;
            case "void":
                name = "V";
            default:
                break;
        }

        if (type.isArray()) {
            return "array." + name;
        }

        return name;
    }

    public String visitMethod(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");
        boolean isMain = methodName.equals("main");

        String signature = Utils.generateMethodSignature(node);
        Type returnType = symbolTable.getReturnType(signature);

        lineWithTabs().append(".method public ");
        if (isMain) ollirBuilder.append("static ");
        ollirBuilder.append(methodName).append("(");

        List<Symbol> parameters = symbolTable.getParameters(signature);
        List<String> parameterStrings = new ArrayList<>();

        for (Symbol parameter : parameters) {
            parameterStrings.add(parameter.getName() + "." + convertType(parameter.getType()));
        }

        ollirBuilder.append(String.join(", ", parameterStrings)).append(").").append(convertType(returnType)).append(" {\n");
        addTab();

        tempVariablesMap.put(signature, 0);
        ifStatementsMap.put(signature, 0);
        whileStatementMap.put(signature, 0);

        int bodyIdx = isMain ? 0 : 1;
        visit(node.getChildren().get(bodyIdx));

        // Visit Return node (if the method has one)
        if (!isMain) {
            visit(node.getChildren().get(bodyIdx + 1));
        }
        else {
            lineWithTabs().append("ret.V;\n");
        }

        removeTab();
        lineWithTabs().append("}\n");

        return null;
    }

    public String visitExpression(JmmNode node, List<Report> reports) {
        return visit(node.getChildren().get(0));
    }

    public String visitVariableDeclaration(JmmNode node, List<Report> reports) {
        JmmNode parentNode = node.getParent();
        Symbol field = symbolTable.getField(node.get("name"));

        if (parentNode.getKind().equals("Class")) {
            lineWithTabs().append(".field private ").append(field.getName()).append(".").append(convertType(field.getType())).append(";\n");
        }

        return null;
    }

    public String visitIf(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        ifStatementsMap.computeIfPresent(signature, (key, count) -> count + 1);
        int ifCount = ifStatementsMap.get(signature);

        JmmNode expressionNode = node.getChildren().get(0), thenNode = node.getChildren().get(1),
                elseNode = node.getChildren().get(2);
        
        String expressionOllir = visit(expressionNode, reports);
        lineWithTabs().append("if (").append(expressionOllir).append(") goto then").append(ifCount).append(";\n");

        addTab();
        visit(elseNode, reports);
        lineWithTabs().append("goto endif").append(ifCount).append(";\n");
        removeTab();

        lineWithTabs().append("then").append(ifCount).append(":\n");

        addTab();
        visit(thenNode, reports);
        removeTab();

        lineWithTabs().append("endif").append(ifCount).append(":\n");

        return null;
    }

    public String visitWhile(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        whileStatementMap.computeIfPresent(signature, (key, count) -> count + 1);
        int whileCount = whileStatementMap.get(signature);

        JmmNode expressionNode = node.getChildren().get(0), bodyNode = node.getChildren().get(1);
        String expressionOllir = visit(expressionNode, reports);

        lineWithTabs().append("loop").append(whileCount).append(":\n");
        
        addTab();
        lineWithTabs().append("if (").append(expressionOllir).append(") goto body").append(whileCount).append(";\n");
        lineWithTabs().append("goto endloop").append(whileCount).append(";\n");
        removeTab();

        lineWithTabs().append("body").append(whileCount).append(":\n");

        addTab();
        visit(bodyNode, reports);
        lineWithTabs().append("goto loop").append(whileCount).append(";\n");
        removeTab();

        lineWithTabs().append("endloop").append(whileCount).append(":\n");

        return null;
    }

    public String visitArithmeticOp(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode leftChild = node.getChildren().get(0), rightChild = node.getChildren().get(1);
        StringBuilder arithmeticBuilder = new StringBuilder();

        arithmeticBuilder.append(visit(leftChild));
        arithmeticBuilder.append(" ").append(arithmeticOpMap.get(node.getKind())).append(".i32 ");
        arithmeticBuilder.append(visit(rightChild));

        JmmNode parentNode = node.getParent();
        // Node kinds that can deal with a binary operation
        Set<String> acceptedParents = Set.of("Expression", "Assign");
        if (acceptedParents.contains(parentNode.getKind())) {
            return arithmeticBuilder.toString();
        }

        incrementTempVariable(signature);
        String tempVar = "t" + tempVariablesMap.get(signature) + ".i32";

        lineWithTabs().append(tempVar).append(" :=.i32 ").append(arithmeticBuilder).append(";\n");

        return tempVar;
    }

    public String visitBooleanOp(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode leftChild, rightChild;
        if (node.getChildren().size() > 1) { // LessThan And
            leftChild = node.getChildren().get(0);
            rightChild = node.getChildren().get(1);
        }
        else { // Not
            leftChild = null;
            rightChild = node.getChildren().get(0);
        }

        StringBuilder booleanBuilder = new StringBuilder();

        if (leftChild != null){
            booleanBuilder.append(visit(leftChild));
        }
        booleanBuilder.append(" ").append(booleanOpMap.get(node.getKind())).append(".bool ");
        booleanBuilder.append(visit(rightChild));

        JmmNode parentNode = node.getParent();
        // Node kinds that can deal with a binary operation
        Set<String> acceptedParents = Set.of("Expression", "Assign");
        if (acceptedParents.contains(parentNode.getKind())) {
            return booleanBuilder.toString();
        }

        incrementTempVariable(signature);
        String tempVar = "t" + tempVariablesMap.get(signature) + ".bool";

        lineWithTabs().append(tempVar).append(" :=.bool ").append(booleanBuilder).append(";\n");

        return tempVar;
    }

    public String visitInt(JmmNode node, List<Report> reports) {
        return node.get("value") + ".i32";
    }

    public String visitBool(JmmNode node, List<Report> reports) {
        return (node.getKind().equals("True") ? "1" : "0") + ".bool";
    }

    public String visitVariable(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        String name = node.get("name");

        // Access local variable
        List<Symbol> localVariables = symbolTable.getLocalVariables(signature);
        Optional<Symbol> optional = localVariables.stream().filter(s -> s.getName().equals(name)).findFirst();

        if (optional.isPresent()) {
            Symbol symbol = optional.get();
            return symbol.getName() + "." + convertType(symbol.getType());
        }

        // Access function parameter
        List<Symbol> parameters = symbolTable.getParameters(signature);

        for (int i = 0; i < parameters.size(); ++i) {
            Symbol symbol = parameters.get(i);
            if (symbol.getName().equals(name)) {
                return "$" + (i + 1) + "." + symbol.getName() + "." + convertType(symbol.getType());
            }
        }

        // Access field
        List<Symbol> fields = symbolTable.getFields();
        optional = fields.stream().filter(s -> s.getName().equals(name)).findFirst();

        if (optional.isPresent()) {
            Symbol symbol = optional.get();
            String convertedType = convertType(symbol.getType());
            StringBuilder fieldBuilder = new StringBuilder();

            fieldBuilder.append("getfield(this, ").append(symbol.getName()).append(".").append(convertedType).append(").").append(convertedType);

            JmmNode parentNode = node.getParent();
            if (parentNode.getKind().equals("Assign")) {
                return fieldBuilder.toString();
            }

            incrementTempVariable(signature);
            String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

            lineWithTabs().append(tempVar).append(" :=.").append(convertedType).append(" ").append(fieldBuilder).append(";\n");
            return tempVar;
        }

        // Should never be reached, this should be caught during semantic analysis
        return null;
    }

    public String visitAssignment(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        JmmNode variable = node.getChildren().get(0), expression = node.getChildren().get(1);

        StringBuilder assignmentBuilder = new StringBuilder();

        if (variable.getKind().equals("Var")) {
            Symbol symbol = symbolTable.getSymbol(signature, variable.get("name"));
            Type type = symbol.getType();
            String convertedType = convertType(type);

            String finalVariable;

            if (symbolTable.fields.contains(symbol)) {
                // We are assigning to a field, therefore we must use putfield
                String expressionOllir = visit(expression, reports);

                if (expressionOllir.startsWith("getfield") || expressionOllir.startsWith("new")) {
                    // Cannot use getfield or new within putfield, therefore we must use a temporary variable
                    incrementTempVariable(signature);
                    String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

                    lineWithTabs().append(tempVar).append(" :=.").append(convertedType).append(" ").append(expressionOllir).append(";\n");
                    expressionOllir = tempVar;
                }

                finalVariable = variable.get("name") + "." + convertedType;
                assignmentBuilder.append("putfield(this, ").append(finalVariable).append(", ").append(expressionOllir).append(").V");
            }
            else {
                finalVariable = visit(variable, reports);
                assignmentBuilder.append(finalVariable).append(" :=.").append(convertedType).append(" ").append(visit(expression, reports));
            }
            
            if (expression.getKind().equals("NewInstance")) {
                assignmentBuilder.append(";\n").append(tabs).append("invokespecial(").append(finalVariable).append(", \"<init>\").V");
            }

            return assignmentBuilder.toString();
        }
        else if (variable.getKind().equals("ArrayAccess")) {
            String arrayAccessOllir = visit(variable, reports);
            assignmentBuilder.append(arrayAccessOllir).append(" :=.i32 ").append(visit(expression, reports));

            return assignmentBuilder.toString();
        }

        return null;
    }

    public String visitStatement(JmmNode node, List<Report> reports) {
        if (node.getChildren().isEmpty()) return null;

        if (node.getNumChildren() > 1) {
            // Several statements inside brackets
            for (JmmNode child : node.getChildren()) {
                visit(child, reports);
            }
            return null;
        }

        JmmNode child = node.getChildren().get(0);
        if (child.getKind().equals("Expression")) {
            String expressionResult = visit(node.getChildren().get(0));
            lineWithTabs().append(expressionResult).append(";\n");
        }
        else {
            visit(child, reports);
        }

        return null;
    }

    public String visitDot(JmmNode node, List<Report> reports) {
        StringBuilder dotBuilder = new StringBuilder();

        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode parentNode = node.getParent();
        JmmNode leftChild = node.getChildren().get(0);
        JmmNode rightChild = node.getChildren().get(1);

        if (rightChild.getKind().equals("Func")) {
            Type returnType = Utils.getExpressionType(symbolTable, node, signature);

            if (returnType == null) {
                /* This should be caught during the semantic analysis, therefore in theory this code should never
                be reached */
                return null;
            }

            String convertedType = convertType(returnType);

            String invokeType, calledOn;

            switch (leftChild.getKind()) {
                case "This":
                    // Calling a function from the current class (use invokevirtual)
                    // Get the signature of the method that is being called
                    String calledSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);

                    invokeType = "invokevirtual";
                    calledOn = "this";
                    break;
                case "Var":
                    Type type = Utils.getVariableType(symbolTable, signature, leftChild.get("name"));

                    if (type == null) {
                        // Symbol not found (calling a static method of another class)
                        invokeType = "invokestatic";
                        calledOn = leftChild.get("name");
                    }
                    else {
                        // Calling a method on an existing symbol
                        invokeType = "invokevirtual";
                        calledOn = leftChild.get("name") + "." + convertType(type);
                    }
                    break;
                case "NewInstance":
                    String newInstanceVar = visit(leftChild, reports);

                    invokeType = "invokevirtual";
                    calledOn = newInstanceVar;
                    break;
                default:
                    return null;
            }

            dotBuilder.append(invokeType).append("(").append(calledOn).append(", \"").append(rightChild.get("name")).append("\"");

            JmmNode argsNode = rightChild.getChildren().get(0);
            for (JmmNode arg : argsNode.getChildren()) {
                String argOllir = visit(arg, reports);
                dotBuilder.append(", ").append(argOllir);
            }

            dotBuilder.append(").").append(convertedType);

            if (parentNode.getKind().equals("Expression") || parentNode.getKind().equals("Assign")) {
                return dotBuilder.toString();
            }

            incrementTempVariable(signature);
            String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

            lineWithTabs().append(tempVar).append(" :=.").append(convertedType).append(" ").append(dotBuilder).append(";\n");
            return tempVar;
        }
        else if (rightChild.getKind().equals("Length")) {
            dotBuilder.append("arraylength(");
            String arrayOllir = visit(leftChild, reports);
            dotBuilder.append(arrayOllir).append(").i32");

            if (parentNode.getKind().equals("Assign")) {
                return dotBuilder.toString();
            }

            incrementTempVariable(signature);
            String tempVar = "t" + tempVariablesMap.get(signature) + ".i32";

            lineWithTabs().append(tempVar).append(" :=.i32 ").append(dotBuilder.toString()).append(";\n");
            return tempVar;
        }

        return null;
    }

    public String visitNewInstance(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        String className = node.get("class");

        StringBuilder newInstanceBuilder = new StringBuilder();
        newInstanceBuilder.append("new(").append(className).append(").").append(className);

        if (node.getParent().getKind().equals("Assign")) {
            return newInstanceBuilder.toString();
        }

        incrementTempVariable(signature);
        String tempVar = "t" + tempVariablesMap.get(signature) + "." + className;

        lineWithTabs().append(tempVar).append(" :=.").append(className).append(" ")
                .append(newInstanceBuilder).append(";\n");
        lineWithTabs().append("invokespecial(").append(tempVar).append(", \"<init>\").V;\n");

        return tempVar;
    }

    public String visitNewArray(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode parentNode = node.getParent();
        JmmNode sizeNode = node.getChildren().get(0);

        String sizeExpressionOllir = visit(sizeNode.getChildren().get(0), reports);
        StringBuilder newArrayBuilder = new StringBuilder();

        newArrayBuilder.append("new(array, ").append(sizeExpressionOllir).append(").array.i32");

        if (parentNode.getKind().equals("Assign")) {
            return newArrayBuilder.toString();
        }

        incrementTempVariable(signature);
        String tempVar = "t" + tempVariablesMap.get(signature) + ".array.i32";

        lineWithTabs().append(tempVar).append(" :=.array.i32 ").append(newArrayBuilder).append(";\n");
        return tempVar;
    }

    public String visitReturn(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        Type returnType = symbolTable.getReturnType(signature);

        String expressionOllir = visit(node.getChildren().get(0), reports);

        lineWithTabs().append("ret.").append(convertType(returnType)).append(" ").append(expressionOllir).append(";\n");
        return null;
    }

    public String visitArrayAccess(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode arrayNode = node.getChildren().get(0),
                indexNode = node.getChildren().get(1),
                parentNode = node.getParent();
        
        String arrayOllir = visit(arrayNode, reports);
        String indexOllir = visit(indexNode, reports);

        // Remove type information from the array variable (to conform with the OLLIR specification)
        if (arrayOllir.startsWith("$")) {
            // Parameter (we need to keep the name of the parameter as well as its number)
            arrayOllir = arrayOllir.substring(0, arrayOllir.indexOf('.', arrayOllir.indexOf('.') + 1));
        }
        else {
            // Local variable (we only need to keep the name)
            arrayOllir = arrayOllir.substring(0, arrayOllir.indexOf('.'));
        }

        StringBuilder arrayAccessBuilder = new StringBuilder();
        arrayAccessBuilder.append(arrayOllir).append("[").append(indexOllir).append("].i32");

        if (parentNode.getKind().equals("Assign")) {
            return arrayAccessBuilder.toString();
        }

        incrementTempVariable(signature);
        String tempVar = "t" + tempVariablesMap.get(signature) + ".i32";

        lineWithTabs().append(tempVar).append(" :=.i32 ").append(arrayAccessBuilder).append(";\n");
        return tempVar;
    }
}
