
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.math.NumberUtils.min;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Day07_01a extends AocDay {


    // Advent of Code 2022 Day 7   Part 1
    //  Read about the problem here  https://adventofcode.com/2022/day/7

    private final System sys;

    /**
     * Prepare/parse the input in preparation for running the parts.
     *
     * @param input  the entire problem input as downloaded
     * @param output any display/debug output will be sent to output
     */
    //public Day07_01a(String input, PrintStream output) {
    public Day07_01a(String input, PrintStream output) {



        super(input, output);
        // Load the system
        sys = loadSystem();
    }

    public String part1() {
        // Print the tree from root
        out.println("RooT" + sys.getRoot());
        return String.valueOf(sys.getRoot().sumByMaxSize(100_000));
    }


    public String part2() {
        int spaceAvailable = 70_000_000;
        int unusedSpaceNeeded = 30_000_000;
        int spaceToFree = unusedSpaceNeeded - (spaceAvailable - sys.getRoot().getSize());

        int fileToDelete = sys.getRoot().findFileToDelete(spaceToFree);
        return String.valueOf(fileToDelete);
    }

    private System loadSystem() {
        System sys = new System(new File("src/2022/day01/input.txt"));
        for (var ins : this.input.lines().toList()) {
            if (ins.startsWith("$")) {
                if (ins.contains("cd")) sys.cd(ins.split("cd ")[1]);
            } else {
                sys.add(ins);
            }
        }
        return sys;
    }

    static class System {

        private final File root;
        private File currentDir;

        public System(File root) {
            this.root = root;
            this.currentDir = root;
        }

        public void cd(String name) {
            if (name.equals("..")) {
                currentDir = currentDir.parent;
            } else {
                find(name).ifPresent(f -> currentDir = f);
            }
        }

        public void add(String line) {
            String[] lsLine = line.split(" ");
            File f = lsLine[0].equals("dir") ? new File(lsLine[1]) : new File(lsLine[1], parseInt(lsLine[0]));
            this.currentDir.addSub(f);
        }

        private Optional<File> find(String name) {
            return currentDir.getSubFiles().stream().filter(f -> f.getName().equals(name)).findFirst();
        }

        public File getRoot() {
            return root;
        }
    }

    static class File {

        private final String name;
        private final boolean isDir;
        private final Set<File> subFiles = new HashSet<>();
        private File parent;
        private int size = 0;

        public File(String name, int size) {
            this.name = name;
            this.size = size;
            this.isDir = false;
        }

        public File(String name) {
            this.name = name;
            this.isDir = true;
        }

        public void addSub(File f) {
            f.parent = this;
            this.subFiles.add(f);
        }

        public int getSize() {
            if (this.size == 0)
                size = subFiles.stream().mapToInt(File::getSize).sum();
            return size;
        }

        public int sumByMaxSize(int max) {
            return (this.getSize() <= max ? this.getSize() : 0) + this.subFiles.stream()
                    .filter(File::isDir)
                    .mapToInt(f -> f.sumByMaxSize(max))
                    .sum();
        }

        public int findFileToDelete(int min) {
            if (!this.isDir || this.getSize() < min) return MAX_VALUE;
            int m = this.getSize();
            for (var f : this.getSubFiles()) {
                if (m > findFileToDelete(min)){
                    m = findFileToDelete(min);
                }
                //m = min(m, f.findFileToDelete(min));
            }
            return m;
        }

        public boolean isDir() {
            return isDir;
        }

        public String getName() {
            return name;
        }

        public Set<File> getSubFiles() {
            return subFiles;
        }

        @Override
        public String toString() {
            return this.toString(1);
        }

        private String toString(int lvl) {
            String indent = new String(new char[lvl]).replace("\0", "\t");
            return this.getSize() + " " + this.getName() + "\n" +
                    this.subFiles.stream()
                            .map(f -> f.toString(lvl + 1))
                            .map(s -> indent + s)
                            .collect(Collectors.joining());
        }
    }

}

