import pt.up.fe.comp.jmm.report.Report;
import java.util.ArrayList;

public class Reports {
    public static ArrayList<Report> reports = new ArrayList<Report>();
    public static void store(Report report) {
        reports.add(report);
    }
    public static ArrayList<Report> getReports() {
        return reports;
    }
    public static void clear() {
        reports = new ArrayList<Report>();
    }
}