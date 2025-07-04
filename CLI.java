import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CLI {
    private Path currentDirectory = Paths.get(System.getProperty("user.dir"));
    private TerminalParser parser;

    public CLI() {
        parser = new TerminalParser(currentDirectory);
    }

    public void start() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Welcome to the CLI! Type 'help' for a list of commands.");
            while (true) {
                System.out.print(currentDirectory + "> "); 
                String input = reader.readLine();
                if (input == null || input.trim().equalsIgnoreCase("exit")) {
                    System.out.println("Exiting the CLI. Goodbye!");
                    break;
                }

                parser.processInput(input.trim());
                currentDirectory = parser.getCurrentDirectory(); 
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new CLI().start();
    }
}
