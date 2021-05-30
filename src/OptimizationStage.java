import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class OptimizationStage implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        if (TestUtils.getNumReports(semanticsResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.LLIR, -1,
                    "Started ollir generation but there are errors from previous stage");
            return new OllirResult(semanticsResult, null, Arrays.asList(errorReport));
        }

        if (semanticsResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.LLIR, -1,
                    "Started ollir generation but AST root node is null");
            return new OllirResult(semanticsResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = semanticsResult.getRootNode();
        List<Report> reports = new ArrayList<>();

        StringBuilder ollirCode = new StringBuilder();

        OllirVisitor ollirVisitor = new OllirVisitor(ollirCode, semanticsResult.getSymbolTable());
        ollirVisitor.visit(node, reports);

        //TODO //System.out.println(ollirCode.toString()); // TODO: used to test ollir generation
        return new OllirResult(semanticsResult, ollirCode.toString(), reports);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return ollirResult;
    }

}
