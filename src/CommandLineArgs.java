
public class CommandLineArgs {
    public String path;
    public boolean optimize;
    public Integer maxRegisters;

    public CommandLineArgs(String path, boolean optimize, Integer maxRegisters) {
        this.path = path;
        this.optimize = optimize;
        this.maxRegisters = maxRegisters;
    }
}
