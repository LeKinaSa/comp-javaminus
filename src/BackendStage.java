import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private final StringBuilder jasminBuilder = new StringBuilder(),
            tabs = new StringBuilder(); // Improves Jasmin code formatting

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            //ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            ollirClass.show(); // print to console main information about the input OLLIR

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
                    .append(" ").append(field.getFieldName()).append(" ").append(translateType(ollirClass, field.getFieldType())).append("\n");
        }

        jasminBuilder.append("\n");
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

        buildMethodBody(ollirClass, method);

        if (returnType.getTypeOfElement() == ElementType.VOID) {
            // Add explicit return for void functions
            lineWithTabs().append("return").append("\n");
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
        switch (instruction.getInstType()) {
            case ASSIGN:
                buildAssignInstruction(ollirClass, method, (AssignInstruction) instruction);
                break;
            case BINARYOPER:
                buildBinaryOpInstruction(method, (BinaryOpInstruction) instruction);
                break;
            case CALL:
                buildCallInstruction(ollirClass, (CallInstruction) instruction);
                break;
            case NOPER:
                buildNoperInstruction(method, (SingleOpInstruction) instruction);
                break;
            default:
                break;
        }
    }

    private void buildAssignInstruction(ClassUnit ollirClass, Method method, AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        buildInstruction(ollirClass, method, rhs);

        Operand destination = (Operand) instruction.getDest();
        Descriptor descriptor = method.getVarTable().get(destination.getName());

        if (descriptor != null) {
            ElementType type = descriptor.getVarType().getTypeOfElement();

            // Local variable
            if (type == ElementType.INT32 || type == ElementType.BOOLEAN) {
                lineWithTabs().append(istoreInstruction(descriptor.getVirtualReg())).append("\n");
            }
            else if (type == ElementType.OBJECTREF) {
                lineWithTabs().append(astoreInstruction(descriptor.getVirtualReg())).append("\n");
            }
        }
    }

    private void buildBinaryOpInstruction(Method method, BinaryOpInstruction instruction) {
        Element leftElement = instruction.getLeftOperand(),
                rightElement = instruction.getRightOperand();
        Operation operation = instruction.getUnaryOperation();

        loadElement(method, leftElement);
        loadElement(method, rightElement);

        switch (operation.getOpType()) {
            case ADD:
                lineWithTabs().append("iadd\n");
                break;
            case SUB:
                lineWithTabs().append("isub\n");
                break;
            case MUL:
                lineWithTabs().append("imul\n");
                break;
            case DIV:
                lineWithTabs().append("idiv\n");
                break;                
            default:
                break;
        }
    }

    private void buildCallInstruction(ClassUnit ollirClass, CallInstruction instruction) {
        Element firstArg = instruction.getFirstArg(),
                secondArg = instruction.getSecondArg();

        ElementType firstArgType = firstArg.getType().getTypeOfElement();

        if (firstArgType == ElementType.THIS) {
            lineWithTabs().append(aloadInstruction(0)).append("\n");
        }

        if (instruction.getInvocationType() == CallType.NEW) {
            ClassType classType = (ClassType) firstArg.getType();
            lineWithTabs().append("new ").append(getQualifiedClassName(ollirClass, classType.getName())).append("\n");
            lineWithTabs().append("dup\n");
        }
        else {
            lineWithTabs().append(instruction.getInvocationType()).append(" ");
        }

        if (secondArg != null && secondArg.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) secondArg;

            if (literalElement.getLiteral().equals("\"<init>\"")) {
                jasminBuilder.append("java/lang/Object/<init>()V\n");
            }
            else {
                Operand firstOperand = (Operand) firstArg;
                String literal = literalElement.getLiteral();

                // TODO: change hardcoded part: ()V
                jasminBuilder.append(getQualifiedClassName(ollirClass, firstOperand.getName()))
                        .append("/").append(literal.substring(1, literal.length() - 1)).append("()V\n");
            }
        }
    }

    private void buildNoperInstruction(Method method, SingleOpInstruction instruction) {
        loadElement(method, instruction.getSingleOperand());
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
                // Local variable
                if (descriptor.getVarType().getTypeOfElement() == ElementType.INT32) {
                    lineWithTabs().append(iloadInstruction(descriptor.getVirtualReg())).append("\n");
                }
            }
        }
    }

    // Instructions
    private String aloadInstruction(int index) {
        if (index <= 3) {
            return "aload_" + index;
        }
        return "aload " + index;
    }

    private String astoreInstruction(int index) {
        if (index <= 3) {
            return "astore_" + index;
        }
        return "astore " + index;
    }

    private String iloadInstruction(int index) {
        if (index <= 3) {
            return "iload_" + index;
        }
        return "iload " + index;
    }

    private String istoreInstruction(int index) {
        if (index <= 3) {
            return "istore_" + index;
        }
        return "istore " + index;
    }

    private String iconstInstruction(int constant) {
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
}
