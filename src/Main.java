
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
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

    public static void main(String[] args) throws IOException {
		Main main = new Main();

		String jmmCode = new String((new FileInputStream(args[0])).readAllBytes());
		JmmParserResult parserResult = main.parse(jmmCode);
		// System.out.println(parserResult.toJson());

		try {
			AnalysisStage analysisStage = new AnalysisStage();
			JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(parserResult);
		} catch (Exception e) {
			e.printStackTrace();
		}


    }


}