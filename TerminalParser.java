import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedWriter;

public class TerminalParser {
    private Path currentDirectory;

    public TerminalParser(Path initialDirectory) {
        this.currentDirectory = initialDirectory;
    }

    public void processInput(String input) {
        if (input.equals("help")) {
            displayHelp();
            return;
        }
        String[] pipeCommands = input.split("\\|");
        List<String> results = new ArrayList<>();

        for (String command : pipeCommands) {
            command = command.trim();
            String[] tokens = command.split("\\s+");

            String outputFile = null;
            boolean append = false;

            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals(">")) {
                    outputFile = tokens[i + 1];
                    break;
                } else if (tokens[i].equals(">>")) {
                    outputFile = tokens[i + 1];
                    append = true;
                    break;
                }
            }
            StringBuilder output = new StringBuilder();
            executeCommand(tokens, output);
            if (outputFile != null) {
                writeOutputToFile(output.toString(), outputFile, append);
            } else {
                results.add(output.toString());
            }
        }
        if (!results.isEmpty()) {
            System.out.print(results.get(results.size() - 1));
        }
    }

    private void displayHelp() {
        System.out.println("Available commands:");
        System.out.println("  help                 : Show this help message.");
        System.out.println("  pwd                  : Print the current directory.");
        System.out.println("  cd                   : Change directory to home directory.");
        System.out.println("  cd <dir>             : Change directory to <dir>.");
        System.out.println("  cd <..>              : Change directory to previous directory.");
        System.out.println("  ls                   : List files in the directory.");
        System.out.println("  ls -a                : List all files, including hidden ones.");
        System.out.println("  ls -r                : List files in reverse order.");
        System.out.println("  mkdir <dir>          : Create a new directory.");
        System.out.println("  rmdir <dir>          : Remove an empty directory.");
        System.out.println("  touch <file>         : Create a new file.");
        System.out.println("  mv <src> <dst>       : Move or rename a file.");
        System.out.println("  rm <file>            : Remove a file.");
        System.out.println("  cat <file>           : Display file contents.");
        System.out.println("  cat > <file>         : Write input to a file until 'Exit' is entered.");
        System.out.println("  command1 | command2  : Pipe the output of command1 to command2.");
        System.out.println("  command > file       : Redirect output to a file, overwriting it.");
        System.out.println("  command >> file      : Redirect output to a file, appending to it.");
        System.out.println("  exit                 : Exit the CLI.");
    }

    private void executeCommand(String[] tokens, StringBuilder output) {
        String command = tokens[0];

        switch (command) {
            case "pwd":
                pwd(output);
                break;
            case "cd":
                cd(tokens, output);
                break;
            case "ls":
                ls(tokens, output);
                break;
            case "mkdir":
                mkdir(tokens, output);
                break;
            case "rmdir":
                rmdir(tokens, output);
                break;
            case "touch":
                touch(tokens, output);
                break;
            case "mv":
                mv(tokens, output);
                break;
            case "rm":
                rm(tokens, output);
                break;
            case "cat":
                cat(tokens, output);
                break;
            default:
                output.append("Unknown command: ").append(command).append(System.lineSeparator());
                break;
        }
    }

    private void pwd(StringBuilder output) {
        output.append(currentDirectory).append(System.lineSeparator());
    }

    private void cd(String[] tokens, StringBuilder output) {
        if (tokens.length == 1) {
            currentDirectory = Paths.get(System.getProperty("user.home"));
        } else if (tokens[1].equals(".")) {
            output.append(System.lineSeparator());
        } else {
            Path newPath = tokens[1].equals("..") ? currentDirectory.getParent() : currentDirectory.resolve(tokens[1]);
            if (newPath != null && Files.isDirectory(newPath)) {
                currentDirectory = newPath;
            } else {
                output.append("Directory does not exist: ").append(tokens[1]).append(System.lineSeparator());
            }
        }
    }

    public void ls(String[] tokens, StringBuilder output) {
        final boolean[] showAll = {false};
        final boolean[] reverse = {false};

        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].equals("-a")) {
                showAll[0] = true;
            } else if (tokens[i].equals("-r")) {
                reverse[0] = true;
            } else if (!tokens[i].equals(">") && !tokens[i].equals(">>")) {
                output.append("Unknown option for ls: ").append(tokens[i]).append(System.lineSeparator());
                return;
            }
        }

        try {
            Files.list(currentDirectory)
                    .filter(path -> showAll[0] || !path.getFileName().toString().startsWith("."))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted(reverse[0] ? Comparator.reverseOrder() : Comparator.naturalOrder())
                    .forEach(name -> output.append(name).append(System.lineSeparator()));
        } catch (IOException e) {
            output.append("Error listing directory: ").append(e.getMessage()).append(System.lineSeparator());
        }
    }

    public void mkdir(String[] tokens, StringBuilder output) {
        if (tokens.length < 2) {
            output.append("Usage: mkdir <dir>").append(System.lineSeparator());
        } else {
            try {
                Files.createDirectories(currentDirectory.resolve(tokens[1]));
            } catch (IOException e) {
                output.append("Error creating directory: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
    }

    public void rmdir(String[] tokens, StringBuilder output) {
        if (tokens.length < 2) {
            output.append("Usage: rmdir <dir>").append(System.lineSeparator());
        } else {
            Path dirToRemove = currentDirectory.resolve(tokens[1]);
            try {
                if (Files.exists(dirToRemove)) {
                    if (Files.isDirectory(dirToRemove) && Files.list(dirToRemove).findAny().isPresent()) {
                        output.append("Directory not empty: ").append(tokens[1]).append(System.lineSeparator());
                    } else {
                        Files.delete(dirToRemove);
                    }
                } else {
                    output.append("Directory does not exist: ").append(tokens[1]).append(System.lineSeparator());
                }
            } catch (IOException e) {
                output.append("Error removing directory: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
    }

    private void touch(String[] tokens, StringBuilder output) {
        if (tokens.length < 2) {
            output.append("Usage: touch <file>").append(System.lineSeparator());
        } else {
            try {
                Files.createFile(currentDirectory.resolve(tokens[1]));
            } catch (IOException e) {
                output.append("Error creating file: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
    }

    private void mv(String[] tokens, StringBuilder output) {
        if (tokens.length < 3) {
            output.append("Usage: mv <src> <dst>").append(System.lineSeparator());
        } else {
            try {
                Files.move(currentDirectory.resolve(tokens[1]), currentDirectory.resolve(tokens[2]));
            } catch (IOException e) {
                output.append("Error moving file: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
    }

    private void rm(String[] tokens, StringBuilder output) {
        if (tokens.length < 2) {
            output.append("Usage: rm <file>").append(System.lineSeparator());
        } else {
            Path fileToRemove = currentDirectory.resolve(tokens[1]);
            try {
                // Check if the file exists before trying to delete it
                if (Files.exists(fileToRemove)) {
                    Files.delete(fileToRemove);
                } else {
                    output.append("File does not exist: ").append(tokens[1]).append(System.lineSeparator());
                }
            } catch (NoSuchFileException e) {
                output.append("File does not exist: ").append(tokens[1]).append(System.lineSeparator());
            } catch (IOException e) {
                output.append("Error removing file: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
    }

    public void cat(String[] tokens, StringBuilder output) {
        if (tokens.length < 2) {
            output.append("Usage: cat <file> or cat > <file>").append(System.lineSeparator());
            return;
        }
        if (tokens.length == 3 && tokens[1].equals(">")) {
            writeFile(tokens[2], output);
        }
        else {
            readFile(tokens[1], output);
        }
    }

    private void readFile(String fileName, StringBuilder output) {
        Path filePath = currentDirectory.resolve(fileName);
        try {
            Files.lines(filePath).forEach(line -> output.append(line).append(System.lineSeparator()));
        } catch (NoSuchFileException e) {
            output.append("File does not exist: ").append(fileName).append(System.lineSeparator());
        } catch (IOException e) {
            output.append("Error reading file: ").append(e.getMessage()).append(System.lineSeparator());
        }
    }

    private void writeFile(String fileName, StringBuilder output) {
        Path filePath = currentDirectory.resolve(fileName);
        System.out.println("Enter text (type 'Exit' on a new line to finish):");
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("Exit".equalsIgnoreCase(line.trim())) break;
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            output.append("Error writing to file: ").append(e.getMessage()).append(System.lineSeparator());
        }
    }

    private void writeOutputToFile(String content, String outputFile, boolean append) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(currentDirectory.resolve(outputFile),
                append ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE))) {
            writer.print(content);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }
}
