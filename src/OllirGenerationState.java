import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OllirGenerationState {
    public OllirResult generateOllir(JmmSemanticsResult semanticsResult) {
        if (TestUtils.getNumReports(semanticsResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started ollir generation but there are errors from previous stage");
            return new OllirResult(semanticsResult, null, Arrays.asList(errorReport));
        }

        if (semanticsResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started ollir generation but AST root node is null");
            return new OllirResult(semanticsResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = semanticsResult.getRootNode();
        List<Report> reports = new ArrayList<>();

        StringBuilder ollirCode = new StringBuilder(); // TODO: string or string builder?

        OllirVisitor ollirVisitor = new OllirVisitor(ollirCode);
        ollirVisitor.visit(node, reports);

        return new OllirResult(semanticsResult, ollirCode.toString(), reports);
    }
}
