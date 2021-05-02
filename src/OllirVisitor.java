
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
    private final Map<String, Integer> tempVariablesMap = new HashMap<>();

    private static final Map<String, String> arithmeticOpMap = Map.of("Add", "+", "Sub", "-", "Mul", "*", "Div", "/");
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

        addVisit("Add", this::visitArithmeticOp);
        addVisit("Sub", this::visitArithmeticOp);
        addVisit("Mul", this::visitArithmeticOp);
        addVisit("Div", this::visitArithmeticOp);

        addVisit("LessThan", this::visitBooleanOp);
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
        addVisit("Return", this::visitReturn);

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

    private StringBuilder lineWithTabs() {
        return ollirBuilder.append(tabs);
    }

    private void buildConstructor(String className) {
        lineWithTabs().append(".construct ").append(className).append("().V {\n");
        addTab();
        lineWithTabs().append("invokespecial(this, \"<init>\").V;\n");
        removeTab();
        lineWithTabs().append("}\n");
    }

    public String defaultVisit(JmmNode node, List<Report> reports) {
        // Default visit is a simple post-order visit
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

        // TODO: how to do extends?
        ollirBuilder.append(className).append(" {\n");
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
            constructorCreated = true;
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

        int bodyIdx = isMain ? 0 : 1;
        visit(node.getChildren().get(bodyIdx));

        // Visit Return node (if the method has one)
        if (!isMain) {
            visit(node.getChildren().get(bodyIdx + 1));
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

    public String visitArithmeticOp(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode leftChild = node.getChildren().get(0), rightChild = node.getChildren().get(1);
        StringBuilder arithmeticBuilder = new StringBuilder();

        arithmeticBuilder.append(visit(leftChild));
        arithmeticBuilder.append(" ").append(arithmeticOpMap.get(node.getKind())).append(".i32 ");
        arithmeticBuilder.append(visit(rightChild));

        if (node.getParent().getKind().equals("Expression") || node.getParent().getKind().equals("Assign")) {
            return arithmeticBuilder.toString();
        }

        tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
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

        if (node.getParent().getKind().equals("Expression") || node.getParent().getKind().equals("Assign")) {
            return booleanBuilder.toString();
        }

        tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
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

            tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
            String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

            lineWithTabs().append(tempVar).append(" :=.").append(convertedType).append(" ").append(fieldBuilder).append(";\n");
            return tempVar;
        }

        // Should never be reached, this should be caught during semantic analysis
        return null;
    }

    public String visitAssignment(JmmNode node, List<Report> reports) {
        // TODO: Array assignments
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        JmmNode variable = node.getChildren().get(0), expression = node.getChildren().get(1);

        if (variable.getKind().equals("Var")) {
            Symbol symbol = symbolTable.getSymbol(signature, variable.get("name"));
            Type type = symbol.getType();
            String convertedType = convertType(type);

            String finalVariable;
            StringBuilder assignmentBuilder = new StringBuilder();

            if (symbolTable.fields.contains(symbol)) {
                // We are assigning to a field, therefore we must use putfield
                String expressionOllir = visit(expression, reports);

                if (expressionOllir.startsWith("getfield") || expressionOllir.startsWith("new")) {
                    // Cannot use getfield or new within putfield, therefore we must use a temporary variable
                    tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
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

            /*
                c1.myClass :=.myClass new(myClass,3.i32).myClass;
                c1.myClass :=.myClass new(myClass,3.i32).myClass;
                invokespecial(c1.myClass,"<init>").V; // myClass c1 = new myClass(3);
            */
            
            if (expression.getKind().equals("NewInstance")) {
                assignmentBuilder.append(";\n").append(tabs).append("invokespecial(").append(finalVariable).append(", \"<init>\").V");
            }

            return assignmentBuilder.toString();
        }

        return null;
    }

    public String visitStatement(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() > 1) {
            for (JmmNode child : node.getChildren()) {
                visit(child, reports);
            }
        }

        // TODO: If and While statements
        String expressionResult = visit(node.getChildren().get(0));
        lineWithTabs().append(expressionResult).append(";\n");

        return null;
    }

    public String visitDot(JmmNode node, List<Report> reports) {
        StringBuilder dotBuilder = new StringBuilder();

        // TODO: Array length
        String signature = Utils.generateMethodSignatureFromChildNode(node);

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

            JmmNode parentNode = node.getParent();
            if (parentNode.getKind().equals("Expression") || parentNode.getKind().equals("Assign")) {
                return dotBuilder.toString();
            }

            tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
            String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

            lineWithTabs().append(tempVar).append(" :=.").append(convertedType).append(" ").append(dotBuilder).append(";\n");
            return tempVar;
        }

        return null;
    }

    public String visitNewInstance(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        String className = node.get("class");

        /*
            io.println(new Fac().compFac(10));

            aux1.Fac :=.Fac new(Fac).Fac;
            invokespecial(aux1.Fac,"<init>").V;
            aux2.i32 :=.i32 invokevirtual(aux1.Fac,"compFac",10.i32).i32;
            invokestatic(io, "println", aux2.i3).V;
        */

        StringBuilder newInstanceBuilder = new StringBuilder();
        newInstanceBuilder.append("new(").append(className).append(").").append(className);

        if (node.getParent().getKind().equals("Assign")) {
            return newInstanceBuilder.toString();
        }

        tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
        String tempVar = "t" + tempVariablesMap.get(signature) + "." + className;

        lineWithTabs().append(tempVar).append(" :=.").append(className).append(" ")
                .append(newInstanceBuilder).append(";\n");
        lineWithTabs().append("invokespecial(").append(tempVar).append(", \"<init>\").V;\n");

        return tempVar;
    }

    public String visitReturn(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        Type returnType = symbolTable.getReturnType(signature);

        String expressionOllir = visit(node.getChildren().get(0), reports);

        lineWithTabs().append("ret.").append(convertType(returnType)).append(" ").append(expressionOllir).append(";\n");
        return null;
    }
}
