
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class MethodSymbolTable {
    public Type returnType;
    public List<Symbol> parameters, localVariables;

    public MethodSymbolTable(Type returnType, List<Symbol> parameters, List<Symbol> localVariables) {
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }
}
