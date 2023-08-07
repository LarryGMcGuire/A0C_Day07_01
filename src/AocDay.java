import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

public abstract class AocDay {
    protected final String input;
    protected final PrintStream out;

    /**
     * Prepare/parse the input in preparation for running the parts.
     *
     * @param input  the entire problem input as downloaded
     * @param output any display/debug output will be sent to output
     */
    protected AocDay(String input, PrintStream output) {
        this.input = input;
        this.out = Objects.requireNonNullElseGet(output, () -> new PrintStream(OutputStream.nullOutputStream()));
    }

    /**
     * Solve part 1 of the day's challenge using the prepared input (prepare must be called first).
     *
     * @return the solution for the day's challenge.
     */
    public abstract String part1();

    /**
     * Solve part 2 of the day's challenge using the prepared input (prepare must be called first).
     *
     * @return the solution for the day's challenge.
     */
    public abstract String part2();
}
