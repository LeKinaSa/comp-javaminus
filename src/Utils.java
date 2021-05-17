
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Utils {
    public static InputStream toInputStream(String text) {
        try {
            return new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static Type getTypeFromString(String str) {
        boolean isArray = str.endsWith("[]");
        if (isArray) str = str.substring(0, str.length() - 2);
        return new Type(str, isArray);
    }

    public static String generateMethodSignature(JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main")) {
            signature += "(String[])";
        }
        else {
            Optional<JmmNode> optional = node.getChildren().stream().filter(child -> child.getKind().equals("Params")).findFirst();

            signature += "(";

            if (optional.isPresent()) {
                List<String> types = new ArrayList<>();

                for (JmmNode param : optional.get().getChildren()) {
                    types.add(param.get("type"));
                }

                signature += String.join(", ", types);
            }

            signature += ")";
        }

        return signature;
    }

    public static String generateMethodSignatureFromChildNode(JmmNode node) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");
        return methodNode.map(Utils::generateMethodSignature).orElse(null);
    }

    public static String getImportedClass(String moduleName) {
        String importedClass;

        if (moduleName.lastIndexOf('.') != -1) {
            importedClass = moduleName.substring(moduleName.lastIndexOf('.') + 1);
        }
        else {
            importedClass = moduleName;
        }

        return importedClass;
    }

    public static String getNodeFunctionSignature(JMMSymbolTable symbolTable, String methodSignature, JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main")) {
            signature += "(String[])";
        }
        else {
            JmmNode argsNode = node.getChildren().get(0);
            if (argsNode != null) {
                List<String> types = new ArrayList<>();

                signature += "(";

                for (JmmNode argNode : argsNode.getChildren()) {
                    Type varType = getExpressionType(symbolTable, argNode, methodSignature);
                    // String varName = expressionNode.getChildren().get(0).get("name");
                    // Type varType = getVariableType(methodSignature, varName);

                    if (varType != null) {
                        types.add((varType.isArray()) ? varType.getName().concat("[]") : varType.getName());
                    }
                }

                signature += String.join(", ", types) + ")";
            }
        }
        return signature;
    }

    public static Type getReturnType(JMMSymbolTable symbolTable, String methodSignature, JmmNode funcNode) {
        String newSignature = getNodeFunctionSignature(symbolTable, methodSignature, funcNode);

        if (symbolTable.methodSymbolTableMap.containsKey(newSignature)) {
            return symbolTable.methodSymbolTableMap.get(newSignature).returnType;
        }

        return null;
    }

    public static Type getVariableType(JMMSymbolTable symbolTable, String signature, String name) {
        Symbol symbol = symbolTable.getSymbol(signature, name);
        return symbol == null ? null : symbol.getType();
    }

    public static Type getExpressionType(JMMSymbolTable symbolTable, JmmNode node, String methodSignature) {
        JmmNode parentNode = node.getParent();

        switch (node.getKind()) {
            case "And":
            case "LessThan":
            case "Not":
            case "True":
            case "False":
                return new Type("boolean", false);
            case "Expression":
                if (node.getNumChildren() == 1) {
                    return getExpressionType(symbolTable, node.getChildren().get(0), methodSignature);
                }
                break;
            case "Add":
            case "Sub":
            case "Mul":
            case "Div":
            case "Int":
                return new Type("int", false);
            case "ArrayAccess": {
                Type arrayType = getExpressionType(symbolTable, node.getChildren().get(0), methodSignature);
                return new Type(arrayType.getName(), false);
            }
            case "This":
                return new Type(symbolTable.getClassName(), false);
            case "Var":
                return getVariableType(symbolTable, methodSignature, node.get("name")); // Variable
            case "Dot": {
                JmmNode leftChild = node.getChildren().get(0);
                JmmNode rightChild = node.getChildren().get(1);

                if (rightChild.getKind().equals("Length")) {
                    return new Type("int", false);
                }
                else if (rightChild.getKind().equals("Func")) {
                    Type thisClassType = new Type(symbolTable.getClassName(), false);

                    String calledMethodSignature = getNodeFunctionSignature(symbolTable, methodSignature, rightChild);
                    if (calledMethodSignature != null && symbolTable.methodSymbolTableMap.containsKey(calledMethodSignature)
                        && thisClassType.equals(getExpressionType(symbolTable, leftChild, methodSignature))) {
                        // Calling a method from this class on an instance of this class (lookup the return type of the method in the symbol table)
                        return getExpressionType(symbolTable, rightChild, methodSignature);
                    }
                    else {
                        // Calling a method from the superclass or an imported class, assume correct types for function calls
                        Set<String> integerOps = Set.of("Add", "Sub", "Mul", "Div", "LessThan", "ArrayAccess", "Size"),
                                booleanOps = Set.of("Not", "And");

                        if (parentNode.getKind().equals("Expression")) {
                            // No way to infer return type, assume void
                            return new Type("void", false);
                        }
                        else if (parentNode.getKind().equals("Assign")) {
                            // Assignment, assume return type is the same as the destination variable
                            JmmNode sibling = parentNode.getChildren().get(0);
                            return getExpressionType(symbolTable, sibling, methodSignature);
                        }
                        else if (booleanOps.contains(parentNode.getKind())) {
                            // Boolean operation, assume boolean return type
                            return new Type("boolean", false);
                        }
                        else if (integerOps.contains(parentNode.getKind())) {
                            // Integer operation, assume int return type
                            return new Type("int", false);
                        }
                    }
                }

                break;
            }
            case "Func":
                return getReturnType(symbolTable, methodSignature, node);
            case "NewArray":
                return new Type("int", true);
            case "NewInstance":
                return new Type(node.get("class"), false);
            default:
                break;
        }

        return null;
    }

    public static String escapeName(String name) {
        return name.replaceAll("ret", "_ret")
                .replaceAll("field", "_field")
                .replaceAll("array", "_array");
    }
}