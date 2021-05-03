
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

import jasmin.sym;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    private final Set<String> fieldNames = new LinkedHashSet<>();
    private final Map<String, Set<String>> parametersAndLocalVariablesMap = new HashMap<>();

    public SymbolTableVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Import", this::visitImport);
        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
        addVisit("VarDecl", this::visitVariableDeclaration);
        addVisit("Param", this::visitParameter);
    }

    private Object visitImport(JmmNode node, List<Report> reports) {
        String module = node.get("module");

        if (!symbolTable.imports.add(module)) {
            String message = "The import " + module + " was already included";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    private Object visitClass(JmmNode node, List<Report> reports) {
        symbolTable.setClassName(node.get("name"));

        Optional<String> extendClass = node.getOptional("extends");
        if (extendClass.isPresent()) {
            symbolTable.setSuperclassName(node.get("extends"));
        }
        
        return null;
    }

    private Object visitMethod(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignature(node);
        Type returnType = Utils.getTypeFromString(node.get("returnType"));

        if (!symbolTable.methods.add(signature)) {
            String message = "Method with signature " + signature + " already exists";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        Set<String> names = new LinkedHashSet<>();
        Set<Symbol> parameters = new LinkedHashSet<>();

        if (node.get("name").equals("main")) {
            names.add(node.get("cmdArgsName"));
            parameters.add(new Symbol(new Type("String", true), node.get("cmdArgsName")));
        }

        parametersAndLocalVariablesMap.put(signature, names);
        symbolTable.methodSymbolTableMap.put(signature, new MethodSymbolTable(returnType, parameters, new LinkedHashSet<>()));
        return null;
    }

    private Object visitVariableDeclaration(JmmNode node, List<Report> reports) {
        String name = node.get("name"), typeName = node.get("type");

        Type type = Utils.getTypeFromString(typeName);
        Symbol symbol = new Symbol(type, name);

        if (node.getParent().getKind().equals("Class")) { // Class field
            symbolTable.fields.add(symbol);

            if (!fieldNames.add(name)) {
                String message = "A field named " + name + " in class " + node.getParent().get("name") + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            }
        }
        else if (node.getParent().getKind().equals("Body")) { // Method local variable
            Optional<JmmNode> methodNode = node.getAncestor("Method");

            if (methodNode.isPresent()) {
                String signature = Utils.generateMethodSignature(methodNode.get());
                symbolTable.methodSymbolTableMap.get(signature).addLocalVariable(symbol);

                if (!parametersAndLocalVariablesMap.get(signature).add(name)) {
                    String message = "A variable named " + name + " in the method with signature " + signature + " already exists";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
                }
            }
        }

        return null;
    }

    private Object visitParameter(JmmNode node, List<Report> reports) {
        String name = node.get("name"), typeName = node.get("type");

        Type type = Utils.getTypeFromString(typeName);
        Symbol symbol = new Symbol(type, name);
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        if (methodNode.isPresent()) {
            String signature = Utils.generateMethodSignature(methodNode.get());
            symbolTable.methodSymbolTableMap.get(signature).addParameter(symbol);

            if (!parametersAndLocalVariablesMap.get(signature).add(name)) {
                String message = "A parameter named " + name + " in the method with signature " + signature + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
            }
        }

        return null;
    }
}

