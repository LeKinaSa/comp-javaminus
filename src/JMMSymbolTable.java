
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JMMSymbolTable implements SymbolTable {
    public final Set<String> imports = new HashSet<>();
    private String className, superclassName;
    public final Set<Symbol> fields = new HashSet<>();
    public final Set<String> methods = new HashSet<>();
    public final Map<String, MethodSymbolTable> methodSymbolTableMap = new HashMap<>();

    @Override
    public List<String> getImports() {
        return new ArrayList<>(imports);
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
        return new ArrayList<>(fields);
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods);
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

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperclassName(String superclassName) {
        this.superclassName = superclassName;
    }
}
