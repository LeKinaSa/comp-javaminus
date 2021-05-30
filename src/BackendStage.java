import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
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

public class BackendStage implements JasminBackend {
    private StringBuilder jasminBuilder = new StringBuilder();
    private final StringBuilder tabs = new StringBuilder(); // Improves Jasmin code formatting

    private static int stackSize = 0;
    private static int maxStackSize = 0;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            //ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            //ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            buildJasminCode(ollirClass);
            String jasminCode = jasminBuilder.toString();

            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode, reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private void addTab() {
        tabs.append('\t');
    }

    private void removeTab() {
        if (tabs.length() > 0) {
            tabs.deleteCharAt(tabs.length() - 1);
        }
    }

    private StringBuilder lineWithTabs() {
        return jasminBuilder.append(tabs);
    }

    private String getQualifiedClassName(ClassUnit ollirClass, String className) {
        for (String importName : ollirClass.getImports()) {
            if (importName.endsWith(className)) {
                return importName.replace('.', '/');
            }
        }

        return className;
    }

    private String translateType(ClassUnit ollirClass, Type ollirType) {
        StringBuilder jasminType = new StringBuilder();

        switch (ollirType.getTypeOfElement()) {
            case INT32:
                jasminType.append("I");
                break;
            case BOOLEAN:
                jasminType.append("Z");
                break;
            case VOID:
                jasminType.append("V");
                break;
            case ARRAYREF: {
                ArrayType arrayType = (ArrayType) ollirType;
                Type arrayElementsType = new Type(arrayType.getTypeOfElements());

                char[] arrayDimensionsRepr = new char[arrayType.getNumDimensions()];
                Arrays.fill(arrayDimensionsRepr, '[');

                jasminType.append(arrayDimensionsRepr).append(translateType(ollirClass, arrayElementsType));
                break;
            }
            case OBJECTREF: {
                ClassType classType = (ClassType) ollirType;
                jasminType.append("L").append(getQualifiedClassName(ollirClass, classType.getName())).append(";");

                break;
            }
            case STRING:
                jasminType.append("Ljava/lang/String;");
                break;
            default:
                break;
        }

        return jasminType.toString();
    }

    private void buildJasminCode(ClassUnit ollirClass) {
        jasminBuilder.append(".class public ").append(ollirClass.getClassName()).append("\n");

        jasminBuilder.append(".super ");
        if (ollirClass.getSuperClass() == null) {
            jasminBuilder.append("java/lang/Object");
        }
        else {
            jasminBuilder.append(ollirClass.getSuperClass());
        }
        jasminBuilder.append("\n\n");

        buildFields(ollirClass);

        for (Method method : ollirClass.getMethods()) {
            buildMethod(ollirClass, method);
        }
    }

    private void buildFields(ClassUnit ollirClass) {
        for (Field field : ollirClass.getFields()) {
            // .field <access-spec> <field-name> <descriptor>
            jasminBuilder.append(".field ").append(field.getFieldAccessModifier().toString().toLowerCase())
                    .append(" ").append(Utils.escapeName(field.getFieldName())).append(" ")
                    .append(translateType(ollirClass, field.getFieldType())).append("\n");
        }

        jasminBuilder.append("\n");
    }

    private int getLocalsLimit(Method method) {
        Set<Integer> usedRegisters = new HashSet<>();
        for (Descriptor var : method.getVarTable().values()) {
            if (var.getScope() == VarScope.LOCAL || var.getScope() == VarScope.PARAMETER) {
                usedRegisters.add(var.getVirtualReg());
            }
        }
        return usedRegisters.size();
    }

    private void buildMethod(ClassUnit ollirClass, Method method) {
        Type returnType = method.getReturnType();

        lineWithTabs().append(".method ");

        if (method.isConstructMethod()) {
            jasminBuilder.append("public <init>()");
        }
        else {
            jasminBuilder.append(method.getMethodAccessModifier().toString().toLowerCase()).append(" ");
            if (method.isStaticMethod()) jasminBuilder.append("static ");
            jasminBuilder.append(method.getMethodName()).append("(");
            buildMethodParameters(ollirClass, method);
            jasminBuilder.append(")");
        }

        jasminBuilder.append(translateType(ollirClass, returnType)).append("\n");
        addTab();

        StringBuilder savedJasminBuilder = null;
        if (!method.isConstructMethod()) {
            // Method should be on a different builder
            savedJasminBuilder = jasminBuilder;
            jasminBuilder = new StringBuilder();
            stackSize = 0;
            maxStackSize = 0;
        }

        buildMethodBody(ollirClass, method);

        if (!method.isConstructMethod()) {
            // Method stored in an auxiliary builder
            StringBuilder methodJasminBuilder = jasminBuilder;
            jasminBuilder = savedJasminBuilder;
            lineWithTabs().append(".limit stack ").append(maxStackSize).append("\n");

            int numLocals = getLocalsLimit(method);
            // "this" is not explicitly present in the var table unless the function uses the keyword
            if (!method.isStaticMethod() && !method.getVarTable().containsKey("this")) numLocals += 1;

            lineWithTabs().append(".limit locals ").append(numLocals).append("\n");
            jasminBuilder.append(methodJasminBuilder);
        }

        removeTab();
        lineWithTabs().append(".end method\n\n");
    }

    private void buildMethodParameters(ClassUnit ollirClass, Method method) {
        for (Element parameter : method.getParams()) {
            jasminBuilder.append(translateType(ollirClass, parameter.getType()));
        }
    }

    private void buildMethodBody(ClassUnit ollirClass, Method method) {
        for (Instruction instruction : method.getInstructions()) {
            buildInstruction(ollirClass, method, instruction);
        }
    }

    private void buildInstruction(ClassUnit ollirClass, Method method, Instruction instruction) {
        for (String label : method.getLabels(instruction)) {
            jasminBuilder.append(label).append(":\n");
        }

        switch (instruction.getInstType()) {
            case ASSIGN:
                buildAssignInstruction(ollirClass, method, (AssignInstruction) instruction);
                break;
            case BINARYOPER:
                buildBinaryOpInstruction(method, (BinaryOpInstruction) instruction);
                break;
            case BRANCH:
                buildBranchInstruction(method, (CondBranchInstruction) instruction);
                break;
            case CALL:
                buildCallInstruction(ollirClass, method, (CallInstruction) instruction);
                break;
            case NOPER:
                buildNoperInstruction(method, (SingleOpInstruction) instruction);
                break;
            case GOTO:
                buildGotoInstruction((GotoInstruction) instruction);
                break;
            case RETURN:
                buildReturnInstruction(method, (ReturnInstruction) instruction);
                break;
            case GETFIELD:
                buildGetFieldInstruction(ollirClass, method, (GetFieldInstruction) instruction);
                break;
            case PUTFIELD:
                buildPutFieldInstruction(ollirClass, method, (PutFieldInstruction) instruction);
                break;
            default:
                break;
        }
    }

    private void buildAssignInstruction(ClassUnit ollirClass, Method method, AssignInstruction instruction) {
        Operand destination = (Operand) instruction.getDest();
        Descriptor descriptor = method.getVarTable().get(destination.getName());

        ElementType assignType = instruction.getTypeOfAssign().getTypeOfElement();

        if (descriptor != null) {
            ElementType type = descriptor.getVarType().getTypeOfElement();

            if (type == ElementType.ARRAYREF && assignType == ElementType.INT32) {
                // Assigning to an array element
                // Push the array reference onto the stack
                lineWithTabs().append(aloadInstruction(descriptor.getVirtualReg())).append("\n");

                ArrayOperand destinationArray = (ArrayOperand) destination;
                // Push the index that is being accessed onto the stack
                loadElement(method, destinationArray.getIndexOperands().get(0));
            }

            Instruction rhs = instruction.getRhs();

            // If the assignment is of type a = a + <b>, where <b> is an integer literal that fits in a signed
            // byte, use iinc as a more efficient instruction
            if (type == ElementType.INT32 && rhs.getInstType() == InstructionType.BINARYOPER) {
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) rhs;

                if (binaryOpInstruction.getUnaryOperation().getOpType() == OperationType.ADD
                        && binaryOpInstruction.getRightOperand().isLiteral()
                        && !binaryOpInstruction.getLeftOperand().isLiteral()) {
                    Operand leftOperand = (Operand) binaryOpInstruction.getLeftOperand();
                    LiteralElement rightElement = (LiteralElement) binaryOpInstruction.getRightOperand();

                    int increment = Integer.parseInt(rightElement.getLiteral());

                    if (increment >= -128 && increment <= 127 && leftOperand.getName().equals(destination.getName())) {
                        lineWithTabs().append("iinc ").append(descriptor.getVirtualReg()).append(" ")
                                .append(rightElement.getLiteral()).append("\n");
                        return;
                    }
                }
            }

            buildInstruction(ollirClass, method, rhs);

            // Local variable
            switch (type) {
                case BOOLEAN:
                case INT32:
                    lineWithTabs().append(istoreInstruction(descriptor.getVirtualReg())).append("\n");
                    break;
                case ARRAYREF:
                    if (assignType == ElementType.ARRAYREF) {
                        lineWithTabs().append(astoreInstruction(descriptor.getVirtualReg())).append("\n");
                    }
                    else {
                        lineWithTabs().append("iastore\n");
                        updateStackSize(-3);
                    }
                    break;
                case OBJECTREF:
                    lineWithTabs().append(astoreInstruction(descriptor.getVirtualReg())).append("\n");
                    break;
                default:
                    break;
            }
        }
    }

    private void buildBinaryOpInstruction(Method method, BinaryOpInstruction instruction) {
        Element leftElement = instruction.getLeftOperand(),
                rightElement = instruction.getRightOperand();
        Operation operation = instruction.getUnaryOperation();

        loadElement(method, leftElement);
        if (!operation.getOpType().equals(OperationType.NOTB)) {
            loadElement(method, rightElement);
        }

        switch (operation.getOpType()) {
            case ADD:
                lineWithTabs().append("iadd\n");
                updateStackSize(-1);
                break;
            case SUB:
                lineWithTabs().append("isub\n");
                updateStackSize(-1);
                break;
            case MUL:
                lineWithTabs().append("imul\n");
                updateStackSize(-1);
                break;
            case DIV:
                lineWithTabs().append("idiv\n");
                updateStackSize(-1);
                break;
            case ANDB:
                lineWithTabs().append("iand\n");
                updateStackSize(-1);
                break;
            case LTH:
                // a < b <=> (a - b) >>> 31 (for 32 bit integers, assuming 0 = false and 1 = true, >>> is unsigned shift right)
                lineWithTabs().append("isub\n");
                updateStackSize(-1);
                lineWithTabs().append(iconstInstruction(31)).append("\n");
                lineWithTabs().append("iushr\n");
                updateStackSize(-1);
                break;
            case NOTB:
                // !b <=> b XOR 1 (assuming 0 = false and 1 = true)
                lineWithTabs().append(iconstInstruction(1)).append("\n");
                lineWithTabs().append("ixor\n");
                updateStackSize(-1);
                break;
            default:
                break;
        }
    }

    private void buildBranchInstruction(Method method, CondBranchInstruction instruction) {
        Element leftOperand = instruction.getLeftOperand(),
                rightOperand = instruction.getRightOperand();

        Operation operation = instruction.getCondOperation();

        switch (operation.getOpType()) {
            case LTH:    // i32 < i32
                loadElement(method, leftOperand);
                loadElement(method, rightOperand);
                lineWithTabs().append("if_icmplt ").append(instruction.getLabel()).append("\n");
                updateStackSize(-2);
                break;
            case ANDB:      // bool && bool
                loadElement(method, leftOperand);
                loadElement(method, rightOperand);
                lineWithTabs().append("iand\n");
                updateStackSize(-1);
                lineWithTabs().append("ifne ").append(instruction.getLabel()).append("\n");
                updateStackSize(-1);
                break;
            case NOTB:
                loadElement(method, leftOperand);
                lineWithTabs().append(iconstInstruction(1)).append("\n");
                lineWithTabs().append("ixor\n");
                updateStackSize(-1);
                lineWithTabs().append("ifne ").append(instruction.getLabel()).append("\n");
                updateStackSize(-1);
                break;
            default:
                break;
        }
    }

    private void buildCallInstruction(ClassUnit ollirClass, Method method, CallInstruction instruction) {
        Element firstArg = instruction.getFirstArg(),
                secondArg = instruction.getSecondArg();

        ElementType firstArgType = firstArg.getType().getTypeOfElement();

        StringBuilder invocationJasmin = new StringBuilder();

        if (instruction.getInvocationType() == CallType.NEW) {
            switch (firstArg.getType().getTypeOfElement()) {
                case OBJECTREF:
                case CLASS: {
                    ClassType classType = (ClassType) firstArg.getType();
                    updateStackSize(1);
                    lineWithTabs().append("new ").append(getQualifiedClassName(ollirClass, classType.getName())).append("\n");
                    return;
                }
                case ARRAYREF: {
                    Element sizeOperand = instruction.getListOfOperands().get(0);
                    loadElement(method, sizeOperand);
                    lineWithTabs().append("newarray int\n"); // New arrays in J-- can only be one-dimensional int arrays
                    return;
                }
                default:
                    break;
            }
        }
        else if (instruction.getInvocationType() == CallType.arraylength) {
            loadElement(method, firstArg);
            lineWithTabs().append("arraylength\n");
            return;
        }
        else {
            if (instruction.getInvocationType() != CallType.invokestatic) {
                loadElement(method, firstArg);
            }
            invocationJasmin.append(instruction.getInvocationType()).append(" ");
        }

        if (secondArg != null && secondArg.isLiteral()) {
            Operand firstOperand = (Operand) firstArg;
            LiteralElement literalElement = (LiteralElement) secondArg;

            if (literalElement.getLiteral().equals("\"<init>\"")) {
                if (firstArgType == ElementType.THIS) {
                    if (ollirClass.getSuperClass() == null) {
                        invocationJasmin.append("java/lang/Object");
                    }
                    else {
                        invocationJasmin.append(ollirClass.getSuperClass());
                    }
                }
                else {
                    ClassType classType = (ClassType) firstOperand.getType();
                    invocationJasmin.append(getQualifiedClassName(ollirClass, classType.getName()));
                }
                invocationJasmin.append("/<init>()V\n");
            }
            else {
                String literal = literalElement.getLiteral(),
                        methodName = literal.substring(1, literal.length() - 1);

                String firstOperandClassName;

                if (firstArgType == ElementType.CLASS) {
                    firstOperandClassName = firstOperand.getName();
                }
                else {
                    firstOperandClassName = ((ClassType) firstOperand.getType()).getName();

                    if (ollirClass.getSuperClass() != null
                            && ollirClass.getMethods().stream().noneMatch(m -> m.getMethodName().equals(methodName))) {
                        firstOperandClassName = ollirClass.getSuperClass();
                    }
                }

                invocationJasmin.append(getQualifiedClassName(ollirClass, firstOperandClassName))
                        .append("/").append(methodName).append("(");

                // Push arguments for function call onto the stack and add them to the method descriptor
                for (Element methodArg : instruction.getListOfOperands()) {
                    loadElement(method, methodArg);
                    invocationJasmin.append(translateType(ollirClass, methodArg.getType()));
                }

                invocationJasmin.append(")").append(translateType(ollirClass, instruction.getReturnType()))
                        .append("\n");
            }
        }

        int numArguments = instruction.getListOfOperands().size();

        switch (instruction.getInvocationType()) {
            case invokespecial:
            case invokevirtual:
                updateStackSize(-1 - numArguments);
                break;
            case invokestatic:
                updateStackSize(-numArguments);
                break;
            default:
                break;
        }

        lineWithTabs().append(invocationJasmin);

        // If function call returns a value, increment stack size
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            updateStackSize(1);

            if (!instruction.getPred().isEmpty()) {
                // Value of the instruction isn't being used, pop return value from the stack
                lineWithTabs().append("pop\n");
                updateStackSize(-1);
            }
        }
    }

    private void buildNoperInstruction(Method method, SingleOpInstruction instruction) {
        loadElement(method, instruction.getSingleOperand());
    }

    private void buildGotoInstruction(GotoInstruction instruction) {
        lineWithTabs().append("goto ").append(instruction.getLabel()).append("\n");
    }

    private void buildReturnInstruction(Method method, ReturnInstruction instruction) {
        Element operand = instruction.getOperand();

        if (operand != null) {
            loadElement(method, operand);

            ElementType type = operand.getType().getTypeOfElement();

            switch (type) {
                case INT32:
                case BOOLEAN:
                    lineWithTabs().append("ireturn\n");
                    break;
                case OBJECTREF:
                case ARRAYREF:
                case STRING:
                case THIS:
                    lineWithTabs().append("areturn\n");
                    break;
                default:
                    break;
            }
        }
        else {
            lineWithTabs().append("return\n");
        }
    }

    private void buildGetFieldInstruction(ClassUnit ollirClass, Method method, GetFieldInstruction instruction) {
        // Push a "this" reference onto the stack
        loadElement(method, instruction.getFirstOperand());
        
        Operand field = (Operand) instruction.getSecondOperand();

        lineWithTabs().append("getfield ").append(ollirClass.getClassName()).append("/").append(field.getName())
                .append(" ").append(translateType(ollirClass, field.getType())).append("\n");
    }

    private void buildPutFieldInstruction(ClassUnit ollirClass, Method method, PutFieldInstruction instruction) {
        // Push a "this" reference onto the stack
        loadElement(method, instruction.getFirstOperand());
        // Push the value that will be assigned to the field onto the stack
        loadElement(method, instruction.getThirdOperand());

        Operand field = (Operand) instruction.getSecondOperand();

        lineWithTabs().append("putfield ").append(ollirClass.getClassName()).append("/").append(field.getName())
                .append(" ").append(translateType(ollirClass, field.getType())).append("\n");
    }

    private void loadElement(Method method, Element element) {
        if (element.isLiteral()) {
            LiteralElement literal = (LiteralElement) element;
            lineWithTabs().append(iconstInstruction(Integer.parseInt(literal.getLiteral()))).append("\n");
        }
        else {
            Operand operand = (Operand) element;
            Descriptor descriptor = method.getVarTable().get(operand.getName());

            if (descriptor != null) {
                ElementType type = descriptor.getVarType().getTypeOfElement();

                // Local variable
                switch (type) {
                    case INT32:
                    case BOOLEAN:
                        lineWithTabs().append(iloadInstruction(descriptor.getVirtualReg())).append("\n");
                        break;
                    case OBJECTREF:
                    case THIS:
                        lineWithTabs().append(aloadInstruction(descriptor.getVirtualReg())).append("\n");
                        break;
                    case ARRAYREF: {
                        try {
                            // Indexing the array
                            ArrayOperand arrayOperand = (ArrayOperand) operand;

                            lineWithTabs().append(aloadInstruction(descriptor.getVirtualReg())).append("\n");

                            Element index = arrayOperand.getIndexOperands().get(0);
                            loadElement(method, index);

                            lineWithTabs().append("iaload\n");
                            updateStackSize(-1);
                        }
                        catch (ClassCastException ex) {
                            // Getting the array reference
                            lineWithTabs().append(aloadInstruction(descriptor.getVirtualReg())).append("\n");
                        }

                        break;
                    }
                }
            }
        }
    }

    // Instructions
    private String aloadInstruction(int index) {
        updateStackSize(1);
        if (index <= 3) {
            return "aload_" + index;
        }
        return "aload " + index;
    }

    private String astoreInstruction(int index) {
        updateStackSize(-1);
        if (index <= 3) {
            return "astore_" + index;
        }
        return "astore " + index;
    }

    private String iloadInstruction(int index) {
        updateStackSize(1);
        if (index <= 3) {
            return "iload_" + index;
        }
        return "iload " + index;
    }

    private String istoreInstruction(int index) {
        updateStackSize(-1);
        if (index <= 3) {
            return "istore_" + index;
        }
        return "istore " + index;
    }

    private String iconstInstruction(int constant) {
        updateStackSize(1);
        if (constant == -1) return "iconst_m1";
        if (constant >= 0 && constant <= 5) {
            return "iconst_" + constant;
        }

        if (constant >= -128 && constant <= 127) {
            return "bipush " + constant;
        }

        if (constant >= -32768 && constant <= 32767) {
            return "sipush " + constant;
        }

        return "ldc " + constant;
    }

    private static void updateStackSize(int increment) {
        stackSize += increment;
        maxStackSize = Math.max(maxStackSize, stackSize);
    }
}
