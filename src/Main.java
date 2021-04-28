
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class Main implements JmmParser {
	public JmmParserResult parse(String jmmCode) {
		try {
			Reports.clear();
			JMM jmm = new JMM(new StringReader(jmmCode));
    		SimpleNode root = jmm.Program(); // returns reference to root node

			if (Reports.getReports().isEmpty())
    			root.dump(""); // prints the tree on the screen

			// System.out.println(root.toJson());
    		return new JmmParserResult(root, Reports.getReports());
		} catch (ParseException ex) {
			Reports.store(new Report(ReportType.ERROR, Stage.SYNTATIC, ex.currentToken.beginLine, ex.getMessage()));
			return new JmmParserResult(null, Reports.getReports());
		}
	}

	public JmmSemanticsResult analyse(JmmParserResult parserResult) {
		try {
			AnalysisStage analysisStage = new AnalysisStage();
			JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(parserResult);
			return semanticsResult;
		} catch (Exception e) {
			e.printStackTrace();
			return new JmmSemanticsResult((JmmNode) null, null, Reports.getReports());
		}
	}

	public OllirResult generateOllir(JmmSemanticsResult semanticsResult) {
		try {
			OptimizationStage optimizationStage = new OptimizationStage();
			OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);
			return ollirResult;
		} catch (Exception e) {
			e.printStackTrace();
			return new OllirResult((ClassUnit) null, null, Reports.getReports());
		}
	}

    public static void main(String[] args) throws IOException {
		Main main = new Main();

		String jmmCode = new String((new FileInputStream(args[0])).readAllBytes());
		JmmParserResult parserResult = main.parse(jmmCode);
		// System.out.println(parserResult.toJson());

		JmmSemanticsResult semanticsResult = main.analyse(parserResult);

		OllirResult ollirResult = main.generateOllir(semanticsResult);
    }


}