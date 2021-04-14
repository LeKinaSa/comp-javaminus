
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SymbolTableVisitor extends PreorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;

    public SymbolTableVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Import", this::visitImport);
        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
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

    private Object visitMethod(JmmNode node, List<Report> reports) {
        String name = node.get("name");
        String signature = name;

        Type returnType;

        if (name.equals("main")) {
            signature += "(String[])";
            returnType = new Type("void", false);
        }
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

            String returnTypeName = node.get("returnType");
            returnType = new Type(returnTypeName, returnTypeName.endsWith("[]"));
        }

        if (!symbolTable.methods.add(signature)) {
            String message = "Method with signature " + signature + " already exists";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, message));
        }

        symbolTable.methodSymbolTableMap.put(signature, new MethodSymbolTable(returnType, new ArrayList<>(), new ArrayList<>()));
        return null;
    }
}
