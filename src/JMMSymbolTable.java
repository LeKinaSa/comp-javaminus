
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.Map;

public class JMMSymbolTable implements SymbolTable {
    private List<String> imports;
    private String className, superclassName;
    private List<Symbol> fields;
    private List<String> methods;
    private Map<String, MethodSymbolTable> methodSymbolTableMap;

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superclassName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodName) {
        return methodSymbolTableMap.get(methodName).returnType;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methodSymbolTableMap.get(methodName).parameters;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methodSymbolTableMap.get(methodName).localVariables;
    }
}
