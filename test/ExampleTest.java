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
    public void testCompleteWhileTest() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        // System.out.println(TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
        TestUtils.mustFail(TestUtils.parse(jmmCode).getReports());

        //assertEquals(11, TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
    }

    @Test
    public void testNestedLoop() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/NestedLoop.jmm");
        TestUtils.mustFail(TestUtils.parse(jmmCode).getReports());
        // System.out.println(TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
    }
}
