
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

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
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        }

        return null;
    }

    private Object visitClass(JmmNode node, List<Report> reports) {
        symbolTable.setClassName(node.get("name"));
        Optional<String> extendClass = node.getOptional("extends");
        if(extendClass.isPresent())
            symbolTable.setSuperclassName(node.get("extends"));
        return null;
    }

    private Object visitMethod(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignature(node);
        String name = node.get("name");
        Type returnType;

        if (name.equals("main")) {
            returnType = new Type("void", false);
        }
        else {
            String returnTypeName = node.get("returnType");
            boolean isArray = returnTypeName.endsWith("[]");
            if (isArray) {
                returnTypeName = returnTypeName.substring(0, returnTypeName.length() - 2);
            }

            returnType = new Type(returnTypeName, isArray);
        }

        if (!symbolTable.methods.add(signature)) {
            String message = "Method with signature " + signature + " already exists";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        }

        symbolTable.methodSymbolTableMap.put(signature, new MethodSymbolTable(returnType, new HashSet<>(), new HashSet<>()));
        return null;
    }

    private Object visitVariableDeclaration(JmmNode node, List<Report> reports) {
        String name = node.get("name"), typeName = node.get("type");
        boolean isArray = typeName.endsWith("[]");
        
        Type type = new Type(isArray ? typeName.substring(0, typeName.length() - 2) : typeName, isArray);
        Symbol symbol = new Symbol(type, name);

        if (node.getParent().getKind().equals("Class")) { // Class field
            if (!symbolTable.fields.add(symbol)) {
                String message = "The field named " + name + " of type " + typeName + " in class " +
                        node.getParent().get("name") + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
            }
        }
        else if (node.getParent().getKind().equals("Body")) { // Method local variable
            Optional<JmmNode> methodNode = node.getAncestor("Method");

            if (methodNode.isPresent()) {
                String signature = Utils.generateMethodSignature(methodNode.get());

                if (!symbolTable.methodSymbolTableMap.get(signature).addLocalVariable(symbol)
                        || symbolTable.fields.contains(symbol)
                        || symbolTable.methodSymbolTableMap.get(signature).parameters.contains(symbol)) {
                    String message = "The variable named " + name + " of type " + typeName +
                            " in the method with signature " + signature + " already exists";
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
                }
            }
        }

        return null;
    }

    private Object visitParameter(JmmNode node, List<Report> reports) {
        String name = node.get("name"), typeName = node.get("type");
        boolean isArray = typeName.endsWith("[]");

        Type type = new Type(isArray ? typeName.substring(0, typeName.length() - 2) : typeName, isArray);
        Symbol symbol = new Symbol(type, name);
        Optional<JmmNode> methodNode = node.getAncestor("Method");

        if (methodNode.isPresent()) {
            String signature = Utils.generateMethodSignature(methodNode.get());

            if (!symbolTable.methodSymbolTableMap.get(signature).addParameter(symbol)
                    || symbolTable.fields.contains(symbol)) {
                String message = "The parameter " + name + " of type " + typeName +
                        " in the method with signature " + signature + " already exists";
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
            }
        }

        return null;
    }
}

