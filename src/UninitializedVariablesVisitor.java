import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class UninitializedVariablesVisitor extends PostorderJmmVisitor<List<Report>, Object> {
    private final JMMSymbolTable symbolTable;
    private final Map<String, Set<String>> initializedVariablesMap = new HashMap<>();

    public UninitializedVariablesVisitor(JMMSymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        addVisit("Var", this::visitVariable);
        addVisit("Assign", this::visitAssignment);
    }

    public Object visitVariable(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode parent = node.getParent();

        if (parent.getKind().equals("Assign") && parent.getChildren().get(0).equals(node)) {
            // Left side of the assignment, ignore
            return null;
        }

        String name = node.get("name");
        Symbol symbol = symbolTable.getSymbol(signature,name);

        initializedVariablesMap.putIfAbsent(signature, new HashSet<>());
        if (symbolTable.methodSymbolTableMap.get(signature).localVariables.contains(symbol)
                && !initializedVariablesMap.get(signature).contains(name)) {
            // Only local variables have to be checked for initialization (parameters are already initialized and we
            // cannot know if fields are initialized
            String message = "Error: variable " + name + " was used without being initialized.";
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), message));
        }

        return null;
    }

    public Object visitAssignment(JmmNode node, List<Report> reports) {
        String signature = Utils.generateMethodSignatureFromChildNode(node);

        JmmNode leftChild = node.getChildren().get(0);

        if (leftChild.getKind().equals("Var")) {
            String name = leftChild.get("name");
            Symbol symbol = symbolTable.getSymbol(signature, name);

            if (symbolTable.methodSymbolTableMap.get(signature).localVariables.contains(symbol)) {
                initializedVariablesMap.putIfAbsent(signature, new HashSet<>());
                initializedVariablesMap.get(signature).add(name);
            }
        }

        return null;
    }
}
