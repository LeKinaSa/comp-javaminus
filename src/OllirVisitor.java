import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class OllirVisitor extends AJmmVisitor<List<Report>, Object> {
    private final StringBuilder ollirCode;

    public OllirVisitor(StringBuilder ollirCodeBuilder) {
        this.ollirCode = ollirCodeBuilder;

    }
}
