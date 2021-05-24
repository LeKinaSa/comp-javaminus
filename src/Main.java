
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
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
			return analysisStage.semanticAnalysis(parserResult);
		} catch (Exception e) {
			e.printStackTrace();
			return new JmmSemanticsResult((JmmNode) null, null, Reports.getReports());
		}
	}

	public OllirResult generateOllir(JmmSemanticsResult semanticsResult) {
		try {
			OptimizationStage optimizationStage = new OptimizationStage();
			return optimizationStage.toOllir(semanticsResult);
		} catch (Exception e) {
			e.printStackTrace();
			return new OllirResult(semanticsResult, null, Reports.getReports());
		}
	}

	public JasminResult generateJasmin(OllirResult ollirResult) {
		try {
			BackendStage backendStage = new BackendStage();
			return backendStage.toJasmin(ollirResult);
		}
		catch (Exception e) {
			e.printStackTrace();
			return new JasminResult(ollirResult, null, Reports.getReports());
		}
	}

    public static void main(String[] args) throws IOException {
		Main main = new Main();

		String jmmCode = new String((new FileInputStream(args[0])).readAllBytes());

		JmmParserResult parserResult = main.parse(jmmCode);
		JmmSemanticsResult semanticsResult = main.analyse(parserResult);
		OllirResult ollirResult = main.generateOllir(semanticsResult);
		JasminResult jasminResult = main.generateJasmin(ollirResult);
		jasminResult.compile(new File("compiled"));
    }
}