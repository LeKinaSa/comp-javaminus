
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();
        List<Report> reports = new ArrayList<>();

        // Get Symbol Table
        JMMSymbolTable symbolTable = new JMMSymbolTable();
        SymbolTableVisitor symbolTableVisitor = new SymbolTableVisitor(symbolTable);
        symbolTableVisitor.visit(node, reports);
        
        TypeVisitor typeVisitor = new TypeVisitor(symbolTable);
        typeVisitor.visit(node, reports);

        UninitializedVariablesVisitor uninitializedVariablesVisitor = new UninitializedVariablesVisitor(symbolTable);
        uninitializedVariablesVisitor.visit(node, reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}