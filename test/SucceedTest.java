import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class SucceedTest {
    public void testSucceed(String path) {
        String jmmCode = SpecsIo.getResource(path);
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        System.out.println("Semantic reports: " + semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        OptimizationStage optimizationStage = new OptimizationStage();
        OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);

        BackendStage backendStage = new BackendStage();
        JasminResult jasminResult = backendStage.toJasmin(ollirResult);

        System.out.println(jasminResult.getJasminCode());
        jasminResult.run();
    }

    @Test
    public void testHelloWorld() {
        testSucceed("fixtures/public/HelloWorld.jmm");
    }

    @Test
    public void testSimple() {
        testSucceed("fixtures/public/Simple.jmm");
    }

    public void testSemantic(String path) {
        String jmmCode = SpecsIo.getResource(path);
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        System.out.println("Semantic reports: " + semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());
    }

    @Test
    public void testQuickSort() {
        testSemantic("fixtures/public/QuickSort.jmm");
    }

    @Test
    public void testLazySort() {
        testSemantic("fixtures/public/Lazysort.jmm");
    }

    @Test
    public void testFindMaximum() {
        testSemantic("fixtures/public/FindMaximum.jmm");
    }

    @Test
    public void testLife() {
        testSemantic("fixtures/public/Life.jmm");
    }

    @Test
    public void testMonteCarloPi() {
        testSemantic("fixtures/public/MonteCarloPi.jmm");
    }

    @Test
    public void testTicTacToe() {
        testSemantic("fixtures/public/TicTacToe.jmm");
    }
}
