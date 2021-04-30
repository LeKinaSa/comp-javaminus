import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

public class FailTest {
    /* SYNTAX ERROR TESTS */

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

    /* SEMANTIC ERROR TESTS */

    public void testSemantic(String path) {
        String jmmCode = SpecsIo.getResource(path);
        JmmParserResult result = TestUtils.parse(jmmCode);
        TestUtils.noErrors(result.getReports());

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(result);

        TestUtils.mustFail(semanticsResult.getReports());

        for (Report report : semanticsResult.getReports()) {
            System.out.println(report);
        }
    }

    @Test
    public void testSemanticExtra() {
        testSemantic("fixtures/public/fail/semantic/extra/miss_type.jmm");
    }
    
    @Test
    public void testSemanticArrIndexNotInt() {
        testSemantic("fixtures/public/fail/semantic/arr_index_not_int.jmm");
    }
    
    @Test
    public void testSemanticArrSizeNotInt() {
        testSemantic("fixtures/public/fail/semantic/arr_size_not_int.jmm");
    }
    
    @Test
    public void testSemanticBadArguments() {
        testSemantic("fixtures/public/fail/semantic/badArguments.jmm");
    }

    @Test
    public void testSemanticBinOpIncomp() {
        testSemantic("fixtures/public/fail/semantic/binop_incomp.jmm");
    }

    @Test
    public void testSemanticFuncNotFound() {
        testSemantic("fixtures/public/fail/semantic/funcNotFound.jmm");
    }

    @Test
    public void testSemanticSimpleLength() {
        testSemantic("fixtures/public/fail/semantic/simple_length.jmm");
    }

    @Test
    public void testSemanticVarExpIncomp() {
        testSemantic("fixtures/public/fail/semantic/var_exp_incomp.jmm");
    }

    @Test
    public void testSemanticVarLitIncomp() {
        testSemantic("fixtures/public/fail/semantic/var_lit_incomp.jmm");
    }

    @Test
    public void testSemanticVarUndef() {
        testSemantic("fixtures/public/fail/semantic/var_undef.jmm");
    }

    @Test
    public void testSemanticVarNotInit() {
        testSemantic("fixtures/public/fail/semantic/varNotInit.jmm");
    }
}
