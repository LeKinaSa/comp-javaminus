import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    private final StringBuilder ollirBuilder;
    private final JMMSymbolTable symbolTable;
    private final StringBuilder tabs = new StringBuilder(); // Improves OLLIR code formatting
    private final Map<String, Integer> tempVariablesMap = new HashMap<>();

    private static final Map<String, String> arithmeticOpMap = Map.of("Add", "+", "Sub", "-", "Mul", "*", "Div", "/");

    public OllirVisitor(StringBuilder ollirBuilder, SymbolTable symbolTable) {
        this.ollirBuilder = ollirBuilder;
        this.symbolTable = (JMMSymbolTable) symbolTable;

        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
        addVisit("Expression", this::visitExpression);

        addVisit("Add", this::visitArithmeticOp);
        addVisit("Sub", this::visitArithmeticOp);
        addVisit("Mul", this::visitArithmeticOp);
        addVisit("Div", this::visitArithmeticOp);

        addVisit("Int", this::visitInt);
        addVisit("Var", this::visitVariable);
        addVisit("Assign", this::visitAssignment);
        addVisit("Statement", this::visitStatement);

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
        // Default visit is a simple postorder visit
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
        // TODO: Visit the body of the method
        visit(node.getChildren().get(0));

    public OllirVisitor(StringBuilder ollirCodeBuilder) {
        this.ollirCode = ollirCodeBuilder;

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

    public String visitInt(JmmNode node, List<Report> reports) {
        return node.get("value") + ".i32";
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
}
