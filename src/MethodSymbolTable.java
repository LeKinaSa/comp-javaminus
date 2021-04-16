
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Set;

public class MethodSymbolTable {
    public Type returnType;
    public Set<Symbol> parameters, localVariables;

    public MethodSymbolTable(Type returnType, Set<Symbol> parameters, Set<Symbol> localVariables) {
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }

    public boolean addParameter(Symbol symbol) {
        return this.parameters.add(symbol);
    }

    public boolean addLocalVariable(Symbol symbol) {
        return this.localVariables.add(symbol);
    }
}
