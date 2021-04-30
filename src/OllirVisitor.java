
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

        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
        addVisit("Expression", this::visitExpression);

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

    public String visitClass(JmmNode node, List<Report> reports) {
        String className = node.get("name");

        // TODO: how to do extends?
        ollirBuilder.append(className).append(" {\n");
        addTab();

        buildConstructor(className);

        // Build OLLIR code for the children of the Class node
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
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

        removeTab();
        lineWithTabs().append("}\n");

        return null;
    }

    public String visitExpression(JmmNode node, List<Report> reports) {
        return visit(node.getChildren().get(0));
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

        List<Symbol> localVariables = symbolTable.getLocalVariables(signature);
        Optional<Symbol> optional = localVariables.stream().filter(s -> s.getName().equals(name)).findFirst();

        if (optional.isPresent()) {
            Symbol symbol = optional.get();
            return symbol.getName() + "." + convertType(symbol.getType());
        }

        List<Symbol> parameters = symbolTable.getParameters(signature);

        for (int i = 0; i < parameters.size(); ++i) {
            Symbol symbol = parameters.get(i);
            if (symbol.getName().equals(name)) {
                return "$" + i + "." + symbol.getName() + "." + convertType(symbol.getType());
            }
        }

        // TODO: fields

        // Should never be reached, this should be caught during semantic analysis
        return null;
    }

    public String visitAssignment(JmmNode node, List<Report> reports) {
        // TODO: Array assignments
        String signature = Utils.generateMethodSignatureFromChildNode(node);
        JmmNode variable = node.getChildren().get(0), expression = node.getChildren().get(1);

        if (variable.getKind().equals("Var")) {
            Type type = symbolTable.getSymbol(signature, variable.get("name")).getType();
            return visit(variable) + " :=." + convertType(type) + " " + visit(expression);
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
            Type returnType = Utils.getExpressionType(symbolTable, rightChild, signature);

            if (returnType == null) {
                /* This should be caught during the semantic analysis, therefore in theory this code should never
                be reached */
                return null;
            }

            String convertedType = convertType(returnType);

            tempVariablesMap.computeIfPresent(signature, (key, count) -> count + 1);
            String tempVar = "t" + tempVariablesMap.get(signature) + "." + convertedType;

            dotBuilder.append(tempVar).append(" :=.").append(convertedType).append(" ");

            if (leftChild.getKind().equals("This")) {
                // Calling a function from the current class (use invokevirtual)

                // Get the signature of the method that is being called
                String calledSignature = Utils.getNodeFunctionSignature(symbolTable, signature, rightChild);



                dotBuilder.append("invokevirtual(this, \"").append(rightChild.get("name")).append("\"");

                // TODO: Passing args
                JmmNode argsNode = rightChild.getChildren().get(0);
                for (JmmNode arg : argsNode.getChildren()) {
                    String argOllir = visit(arg, reports);
                    dotBuilder.append(", ").append(argOllir);
                }

                dotBuilder.append(").").append(convertedType);

                if (node.getParent().getKind().equals("Expression")) {
                    return dotBuilder.toString();
                }
            }

            lineWithTabs().append(dotBuilder).append(";\n");
            return tempVar;
        }

        return null;
    }
}
