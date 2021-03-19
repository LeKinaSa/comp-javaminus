import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {
    @Test
    public void testHelloWorld() {
        String jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
		TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
	}

	@Test
    public void testSimple() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Simple.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testQuickSort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testLazySort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/LazySort.jmm");
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

    @Test
    public void testWhile() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        TestUtils.noErrors(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testJson() {
        String jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        System.out.println(TestUtils.parse(jmmCode).toJson());
    }
}
