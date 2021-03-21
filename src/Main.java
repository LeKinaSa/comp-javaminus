
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;
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
    		//System.out.println(root.toJson());
    		return new JmmParserResult(root, Reports.getReports());
		} catch (ParseException ex) {
			Reports.store(new Report(ReportType.ERROR, Stage.SYNTATIC, ex.currentToken.beginLine, ex.getMessage()));
			return new JmmParserResult(null, Reports.getReports());
		}
	}

    public static void main(String[] args) {
        System.out.println("Executing with args: " + Arrays.toString(args));
        if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }
    }


}