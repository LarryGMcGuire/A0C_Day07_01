import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class AoC {

    public static final double NS_TO_MS = 1 / 1_000_000D;

    private static final String RESULTS_PATH = FileUtil.BASE_PATH + "results/";
    private static final String INPUT_PATH = FileUtil.BASE_PATH + "input/";
    private static final String DAYS_PATH = "com.yoki.advent_of_code.aoc.days";

    private final int year;
    private final int day;
    private final boolean onlyToday;
    private final String sampleInput;
    private final PrintStream outStream;

    private AoC(int year, int day, boolean onlyToday, String sampleInput, boolean displayOutput) {
        this.year = year;
        this.day = day;
        this.onlyToday = onlyToday;
        this.sampleInput = sampleInput;
        this.outStream = displayOutput ? System.out : null;
    }

    public static AocBuilder builder(int year, int day) {
        return new AocBuilder(year, day);
    }

    public void run() {
        // template idea from https://github.com/mrbdahlem/Advent-of-Code
        System.out.println("\u001b[0m\033[2J\033[HAdvent of Code " + this.year);

        int today = this.day;
        int maxDay = today;
        int minDay = this.onlyToday ? today : 1;

        long totalTime = 0;
        for (today = minDay; today <= maxDay; today++) {
            displayHeader(today);

            Result result = runDay(today);
            recordResult(today, result);
            result.display();
            result.displayTiming();

            totalTime = totalTime + result.total();
        }

        // Display the runtime for all days that have been run
        System.out.println(DisplayUtil.prefixColorBold(GREY) + "Total runtime: " + format("%,12.2f ms", (totalTime) * NS_TO_MS));
    }

    private void displayHeader(int day) {
        for (int i = 0; i < 30; i++) {
            System.out.print(DisplayUtil.prefixColor(RED) + "*" + DisplayUtil.prefixColor(GREEN) + "*");
        }
        System.out.println("\n" + DisplayUtil.prefixColor(RESET) + "Day " + day + ":");
    }

    private Result runDay(int day) {

        Result result = new Result();
        try {
            // Load the day's solution class
            Constructor<?> cnstrct = Class.forName(DAYS_PATH + year + ".Day" + day).getDeclaredConstructor(String.class, PrintStream.class);

            // If a sample input file has been provided, run against the sample data
            String input = this.onlyToday && StringUtils.isNotEmpty(this.sampleInput) ? getSampleInput(): getInput(day);

            // Allow the solution to preprocess the input data
            long startTime;
            startTime = System.nanoTime();
            AocDay solution = (AocDay) cnstrct.newInstance(input, outStream);
            result.prepTime = System.nanoTime() - startTime;

            // Run the first part of the solution
            startTime = System.nanoTime();
            result.part1Result = solution.part1();
            result.part1Time = System.nanoTime() - startTime;

            // Run the second part of the solution
            startTime = System.nanoTime();
            result.part2Result = solution.part2();
            result.part2Time = System.nanoTime() - startTime;

        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("Could not execute day " + day + ". " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return result;
    }

    private String getSampleInput() {
        try {
            return fileContents(INPUT_PATH + year + "/sample/" + this.sampleInput);
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            System.exit(1);
            return "";
        }
    }

    /**
     * A method to load everything from a file into a String
     *
     * @param filename the name (and path) of the file to load
     * @return the contents of the file
     */
    private String fileContents(String filename) throws IOException {
        return Files.readString(new File(filename).toPath());
    }

    /**
     * Automatically download problem input if the input.txt file doesn't exist
     *
     * @param day      the day (1-25) of input to download, -1 to automatically determine
     * @return the the input data
     */
    private String getInput(int day) {
        String filename = format("%s%s/day%02d.txt", INPUT_PATH, year, day);

        // Attempt to load the input file if it is already downloaded
        File inputFile = new File(filename);
        if (inputFile.exists()) {
            System.out.println("Using cached input from " + filename);
            try {
                return fileContents(filename);
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
        return loadInputFromSession(day, filename, inputFile);
    }

    private String loadInputFromSession(int day, String filename, File inputFile) {
        // make sure a session cookie has been provided for downloading
        // authorization
        String cookie = System.getenv("SESSION");
        if (cookie == null) {
            System.err.println("You need to set SESSION cookie in .env file.");
            throw new RuntimeException("Need to set SESSION cookie in .env file.");
        }

        StringBuilder data = new StringBuilder(); // The downloaded file data
        try {
            // Create parent directories as necessary for input file
            inputFile.getParentFile().mkdirs();

            System.out.println("Downloading input data for " + year + " day " + day);
            URL url = new URL("https://adventofcode.com/" + year + "/day/" + day + "/input");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Cookie", "session=" + cookie);

            con.setInstanceFollowRedirects(true);
            int status = con.getResponseCode();

            if (status > 299) {
                System.out.println("Connection status " + status + " downloading input for " + year + " day " + day);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                    }
                    System.exit(0);
                }
            }

            // Streams to handle input from http connection, output to file
            try (InputStream is = con.getInputStream()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                try (FileOutputStream fos = new FileOutputStream(inputFile)) {
                    PrintStream ps = new PrintStream(fos);
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        data.append(inputLine).append("\n");
                        ps.println(inputLine);
                    }
                }
                System.out.println("Input data saved to " + filename);
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
        return data.toString();
    }

    /**
     * Record the results for a given day's parts. If a Result is provided for a part of the day, it will be recorded to a file. If
     * the file exists, it will be checked to determine if the result has been tried before.
     */
    private void recordResult(int day, Result results) {
        String filebase = format("%s%s/Day%02d Part%%1d.txt",RESULTS_PATH,year, day);
        results.part1Unique = checkAndAppend(format(filebase, 1), results.part1Result);
        results.part2Unique = checkAndAppend(format(filebase, 2), results.part2Result);
    }

    /**
     * Record data to a file. If the file exists, it will be checked to determine if the data is already contained in it.
     *
     * @returns true if there is no data, or if the data does not appear in the file
     */
    private boolean checkAndAppend(String filename, String data) {
        if (data == null || data.trim().isEmpty()) {
            return true;
        }

        // Since the file will be broken down by newlines, replace any in the
        // data with carriage returns
        String dataLine = data.replace("\n", "\r");

        // Determine if the file contains any lines that match the data
        boolean unique = Arrays.stream(loadPrevious(filename)).noneMatch(dataLine::equals);

        if (unique) {
            // If the data is a new response
            appendData(filename, dataLine);
        }

        return unique;
    }

    private void appendData(String filename, String dataLine) {
        try {
            // make sure the result file parent directory exists
            new File(filename).getParentFile().mkdirs();
            // add the data to the end of the file
            Files.write(
                    Paths.get(filename),
                    (dataLine + "\n").getBytes(),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("Could not append to " + filename);
            System.err.println(e);
        }
    }

    private String[] loadPrevious(String filename) {
        String[] previous;
        try {
            previous = fileContents(filename).split("\n");
        } catch (IOException e) {
            previous = new String[0];
        }
        return previous;
    }

    private static class Result {

        public static final String F_MS_N = "%,12.2f ms%n";
        private long prepTime = 0;
        private long part1Time = 0;
        private long part2Time = 0;
        private String part1Result;
        private String part2Result;
        private boolean part1Unique;
        private boolean part2Unique;

        public long total() {
            return prepTime + part1Time + part2Time;
        }

        public void display() {
            System.out.println(DisplayUtil.prefixColor("RED") + "Part 1: > " + part1Result + " < " + (part1Unique ? "" : "REPEATED RESPONSE"));
            System.out.println(DisplayUtil.prefixColor("GREEN") + "Part 2: > " + part2Result + " < " + (part2Unique ? "" : "REPEATED RESPONSE"));
        }

        public void displayTiming() {
            System.out.print(DisplayUtil.prefixColor("GREY") + "--- Prep:   ");
            System.out.printf(F_MS_N, (prepTime) * NS_TO_MS);
            System.out.print(DisplayUtil.prefixColor("RED") + "--- Part 1: ");
            System.out.printf(F_MS_N, (part1Time) * NS_TO_MS);
            System.out.print(DisplayUtil.prefixColor("GREEN") + "--- Part 2: ");
            System.out.printf(F_MS_N, (part2Time) * NS_TO_MS);
        }
    }

    public static class AocBuilder {

        private final int year;
        private final int day;
        private boolean onlyToday = false;
        private boolean displayOutput = true;
        private String sampleInput;

        public AocBuilder(int year, int day) {
            this.year = year;
            this.day = day;
        }

        public AocBuilder oneDayOnly() {
            this.onlyToday = true;
            return this;
        }

        public AocBuilder disableDisplay() {
            this.displayOutput = false;
            return this;
        }

        public AocBuilder sampleInput(String input) {
            this.sampleInput = input;
            return this;
        }

        public AoC build() {
            return new AoC(year, day, onlyToday, sampleInput, displayOutput);
        }
    }
}
