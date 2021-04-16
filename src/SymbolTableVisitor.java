
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
        addVisit("VarDecl", this::visitVariables);
        addVisit("Param", this::visitParameters);
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
    }
}

