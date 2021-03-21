import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class FailTest {
    @Test
    public void testCompleteWhile() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        assertEquals(11, TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
    }

    @Test
    public void testLength() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/LengthError.jmm");
        TestUtils.mustFail(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testBlowUp() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/BlowUp.jmm");
        TestUtils.mustFail(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testMissingRightPar() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MissingRightPar.jmm");
        TestUtils.mustFail(TestUtils.parse(jmmCode).getReports());
    }

    @Test
    public void testMultipleSequential() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MultipleSequential.jmm");
        assertEquals(2, TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
    }

    @Test
    public void testNestedLoop() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/NestedLoop.jmm");
        assertEquals(2, TestUtils.getNumErrors(TestUtils.parse(jmmCode).getReports()));
    }
}
