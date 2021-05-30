
import java.util.*;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

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
    private final CommandLineArgs args;

    public OptimizationStage(CommandLineArgs args) {
        this.args = args;
    }

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

        System.out.println(ollirCode.toString()); // TODO: used to test ollir generation
        OllirResult result = new OllirResult(semanticsResult, ollirCode.toString(), reports);

        ClassUnit ollirClass = result.getOllirClass();

        if (args.maxRegisters != null) {
            ollirClass.buildCFGs();
            ollirClass.buildVarTables();
            result = optimize(result);
        }

        return result;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return semanticsResult;
    }

    public void addElementToUse(Element element, int idx, Method method, List<Set<String>> use) {
        if (element != null && !element.isLiteral()) {
            Operand operand = (Operand) element;
            Descriptor descriptor = method.getVarTable().get(operand.getName());

            ElementType type = element.getType().getTypeOfElement();

            if (type != ElementType.THIS && type != ElementType.CLASS && descriptor.getScope() == VarScope.LOCAL) {
                use.get(idx).add(operand.getName());
            }
        }
    }

    public void handleInstruction(Instruction instruction, int idx, Method method, List<Set<String>> use,
                                  List<Set<String>> def, List<Set<Integer>> succ) {
        List<Instruction> instructions = method.getInstructions();

        switch (instruction.getInstType()) {
            case ASSIGN: {
                AssignInstruction assignInstruction = (AssignInstruction) instruction;

                Operand destination = (Operand) assignInstruction.getDest();
                Descriptor descriptor = method.getVarTable().get(destination.getName());
                if (descriptor.getScope() == VarScope.LOCAL) {
                    def.get(idx).add(destination.getName());
                }

                Instruction rhs = assignInstruction.getRhs();
                handleInstruction(rhs, idx, method, use, def, succ);
                break;
            }
            case BINARYOPER: {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;

                addElementToUse(binaryOpInstruction.getLeftOperand(), idx, method, use);
                addElementToUse(binaryOpInstruction.getRightOperand(), idx, method, use);
                break;
            }
            case BRANCH: {
                CondBranchInstruction branchInstruction = (CondBranchInstruction) instruction;
                int branchIndex = instructions.indexOf(method.getLabels().get(branchInstruction.getLabel()));
                succ.get(idx).add(branchIndex);

                addElementToUse(branchInstruction.getLeftOperand(), idx, method, use);
                addElementToUse(branchInstruction.getRightOperand(), idx, method, use);
                break;
            }
            case CALL: {
                CallInstruction callInstruction = (CallInstruction) instruction;
                addElementToUse(callInstruction.getFirstArg(), idx, method, use);

                for (Element element : callInstruction.getListOfOperands()) {
                    addElementToUse(element, idx, method, use);
                }
                break;
            }
            case GOTO: {
                GotoInstruction gotoInstruction = (GotoInstruction) instruction;
                int gotoIndex = instructions.indexOf(method.getLabels().get(gotoInstruction.getLabel()));
                succ.get(idx).add(gotoIndex);
                break;
            }
            case NOPER: {
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                addElementToUse(singleOpInstruction.getSingleOperand(), idx, method, use);
                break;
            }
            case PUTFIELD: {
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                addElementToUse(putFieldInstruction.getThirdOperand(), idx, method, use);
                break;
            }
            case RETURN: {
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                addElementToUse(returnInstruction.getOperand(), idx, method, use);
                break;
            }
        }
    }

    public static class LivenessResult {
        public List<Set<String>> liveIn, liveOut;

        public LivenessResult(List<Set<String>> liveIn, List<Set<String>> liveOut) {
            this.liveIn = liveIn;
            this.liveOut = liveOut;
        }
    }

    public LivenessResult livenessAnalysis(Method method) {
        List<Instruction> instructions = method.getInstructions();

        List<Set<String>> use = new ArrayList<>(), def = new ArrayList<>();
        List<Set<Integer>> succ = new ArrayList<>();

        for (int i = 0; i < instructions.size(); ++i) {
            use.add(new HashSet<>());
            def.add(new HashSet<>());
            succ.add(new HashSet<>());
        }

        // Build use, def and succ maps
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction instruction = method.getInstr(i);

            if (instruction.getInstType() != InstructionType.GOTO && i != instructions.size() - 1) {
                succ.get(i).add(i + 1);
            }

            handleInstruction(instruction, i, method, use, def, succ);
        }

        // Perform liveness analysis
        List<Set<String>> liveIn = new ArrayList<>(), liveOut = new ArrayList<>();
        for (int i = 0; i < instructions.size(); ++i) {
            liveIn.add(new HashSet<>());
            liveOut.add(new HashSet<>());
        }

        while (true) {
            List<Set<String>> newLiveIn = new ArrayList<>(), newLiveOut = new ArrayList<>();

            // Make copy of liveIn and liveOut
            for (int i = 0; i < instructions.size(); ++i) {
                newLiveIn.add(new HashSet<>(liveIn.get(i)));
                newLiveOut.add(new HashSet<>(liveOut.get(i)));
            }

            for (int i = use.size() - 1; i >= 0; --i) {
                Set<String> temp = new HashSet<>();
                for (Integer s : succ.get(i)) {
                    temp.addAll(newLiveIn.get(s));
                }
                newLiveOut.set(i, temp);

                temp = new HashSet<>(newLiveOut.get(i));
                temp.removeAll(def.get(i));

                newLiveIn.set(i, use.get(i));
                newLiveIn.get(i).addAll(temp);
            }

            if (newLiveIn.equals(liveIn) && newLiveOut.equals(liveOut)) {
                break;
            }

            liveIn = newLiveIn;
            liveOut = newLiveOut;
        }

        return new LivenessResult(liveIn, liveOut);
    }

    public Graph<String> constructInterferenceGraph(LivenessResult result) {
        Map<String, Set<Integer>> liveRange = new HashMap<>();

        for (int i = 0; i < result.liveOut.size(); ++i) {
            for (String varName : result.liveOut.get(i)) {
                liveRange.putIfAbsent(varName, new HashSet<>());
                liveRange.get(varName).add(i);
            }
        }

        Graph<String> graph = new Graph<>();
        liveRange.keySet().forEach(graph::addVertex);

        for (String firstVarName : liveRange.keySet()) {
            for (String secondVarName : liveRange.keySet()) {
                if (!firstVarName.equals(secondVarName)) {
                    Set<Integer> range = new HashSet<>(liveRange.get(firstVarName));
                    range.retainAll(liveRange.get(secondVarName));

                    if (!range.isEmpty()) {
                        // Interference between variables
                        graph.addEdge(firstVarName, secondVarName, false);
                    }
                }
            }
        }

        return graph;
    }

    public Map<String, Integer> registerAllocation(Graph<String> interferenceGraph, int maxRegisters) throws Exception {
        Graph<String> copy = new Graph<>(interferenceGraph);
        Stack<String> stack = new Stack<>();

        while (!copy.getVertices().isEmpty()) {
            Set<String> vertices = copy.getVertices();

            boolean found = false;

            for (String vertex : vertices) {
                if (copy.getEdges(vertex).size() < maxRegisters) {
                    found = true;
                    stack.add(vertex);
                    copy.removeVertex(vertex);
                    break;
                }
            }

            if (!found) {
                throw new Exception("Couldn't find a variable with degree < k!");
            }
        }

        Map<String, Integer> graphColoring = new HashMap<>();

        for (String variable : interferenceGraph.getVertices()) {
            graphColoring.put(variable, null);
        }

        while (!stack.empty()) {
            String variable = stack.pop();

            boolean colored = false;

            for (int i = 0; i < maxRegisters; ++i) {
                boolean canBeColored = true;
                for (String adjacent : interferenceGraph.getEdges(variable)) {
                    Integer adjacentColor = graphColoring.get(adjacent);
                    if (adjacentColor != null && adjacentColor == i) {
                        canBeColored = false;
                        break;
                    }
                }

                if (canBeColored) {
                    colored = true;
                    graphColoring.put(variable, i);
                    break;
                }
            }

            if (!colored) {
                // Throw exception!
                throw new Exception("No more colors!");
            }
        }

        return graphColoring;
    }

    public void assignRegisters(Method method, Map<String, Integer> graphColoring) {
        int startRegister = 0;

        if (!method.isStaticMethod()) {
            startRegister += 1; // "this" reference always occupies register 0
        }

        startRegister += method.getParams().size();

        Map<String, Descriptor> varTable = method.getVarTable();
        for (Map.Entry<String, Integer> entry : graphColoring.entrySet()) {
            varTable.get(entry.getKey()).setVirtualReg(startRegister + entry.getValue());
        }
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            // Perform liveness analysis to obtain the variables' live ranges
            LivenessResult result = livenessAnalysis(method);

            // Construct the interference graph
            Graph<String> interferenceGraph = constructInterferenceGraph(result);

            // Apply graph coloring for register allocation
            Map<String, Integer> graphColoring;
            try {
                graphColoring = registerAllocation(interferenceGraph, args.maxRegisters);
            }
            catch (Exception ex) {
                System.out.println(ex.getMessage());
                return ollirResult;
            }

            assignRegisters(method, graphColoring);
        }

        return ollirResult;
    }

}
