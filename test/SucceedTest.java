import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class SucceedTest {
    @Test
    public void testHelloWorld() {
        String jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        System.out.println("Semantic reports: " + semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        OptimizationStage optimizationStage = new OptimizationStage();
        OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);
    }

    @Test
    public void testSimple() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Simple.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        System.out.println("Semantic reports: " + semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        OptimizationStage optimizationStage = new OptimizationStage();
        OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);
    }

    @Test
    public void testQuickSort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testLazySort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Lazysort.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testFindMaximum() {
        String jmmCode = SpecsIo.getResource("fixtures/public/FindMaximum.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testLife() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Life.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testMonteCarloPi() {
        String jmmCode = SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testTicTacToe() {
        String jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }
}
