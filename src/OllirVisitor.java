
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    private final StringBuilder ollirBuilder;
    private final SymbolTable symbolTable;
    private final StringBuilder tabs = new StringBuilder(); // Improves OLLIR code formatting

    public OllirVisitor(StringBuilder ollirBuilder, SymbolTable symbolTable) {
        this.ollirBuilder = ollirBuilder;
        this.symbolTable = symbolTable;

        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
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

        // TODO: Visit the body of the method

        removeTab();
        lineWithTabs().append("}\n");

        return null;
    }
}
