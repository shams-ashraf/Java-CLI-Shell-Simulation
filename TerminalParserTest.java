import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

public class TerminalParserTest {
    private TerminalParser parser;
    private Path testDir;
    private StringBuilder output;

    @BeforeEach
    void setUp() throws IOException {
        testDir = Files.createTempDirectory("testDir");
        parser = new TerminalParser(testDir);
        output = new StringBuilder();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(testDir)
                .map(Path::toFile)
                .forEach(file -> file.delete());
    }

    @Test
    public void testPwd() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        parser.processInput("pwd");

        System.setOut(System.out);
        assertEquals(testDir.toString() + System.lineSeparator(), outputStream.toString());
    }

    @Test
    public void testCd() throws IOException {
        String subDirName = "subDir";
        Path subDir = testDir.resolve(subDirName);
        Files.createDirectory(subDir);

        parser.processInput("cd " + subDirName);
        assertEquals(subDir, parser.getCurrentDirectory());

        parser.processInput("cd ..");
        assertEquals(testDir, parser.getCurrentDirectory());

        Path initialDir = parser.getCurrentDirectory();
        parser.processInput("cd .");
        assertEquals(initialDir, parser.getCurrentDirectory());

        parser.processInput("cd");
        assertEquals(Paths.get(System.getProperty("user.home")), parser.getCurrentDirectory());
    }

    @Test
    public void testMkdir() {
        String newDirName = "newDir";
        parser.processInput("mkdir " + newDirName);

        assertTrue(Files.isDirectory(testDir.resolve(newDirName)));
    }

    @Test
    public void testTouch() {
        String fileName = "newFile.txt";
        parser.processInput("touch " + fileName);

        assertTrue(Files.isRegularFile(testDir.resolve(fileName)));
    }

    @Test
    public void testRm() throws IOException {
        String fileName = "fileToDelete.txt";
        Path filePath = testDir.resolve(fileName);
        Files.createFile(filePath);

        parser.processInput("rm " + fileName);

        assertFalse(Files.exists(filePath));
    }

    @Test
    public void testMv() throws IOException {
        String srcFileName = "srcFile.txt";
        String dstFileName = "dstFile.txt";
        Path srcFilePath = testDir.resolve(srcFileName);
        Path dstFilePath = testDir.resolve(dstFileName);

        Files.createFile(srcFilePath);
        parser.processInput("mv " + srcFileName + " " + dstFileName);

        assertFalse(Files.exists(srcFilePath));
        assertTrue(Files.exists(dstFilePath));
    }

    @Test
    public void testRedirectAndAppend() throws IOException {
        String sourceFileName = "source.txr";
        String destinationFileName = "destination.txr";

        String createSourceCommand = "touch " + sourceFileName;
        parser.processInput(createSourceCommand);

        Files.writeString(testDir.resolve(sourceFileName), "This is the first line." + System.lineSeparator());

        String createDestinationCommand = "touch " + destinationFileName;
        parser.processInput(createDestinationCommand);

        String commandCatToDest = "cat " + sourceFileName + " > " + destinationFileName;
        parser.processInput(commandCatToDest);

        Path destinationFilePath = testDir.resolve(destinationFileName);
        assertTrue(Files.exists(destinationFilePath));

        assertEquals("This is the first line." + System.lineSeparator(), Files.readString(destinationFilePath));

        String commandAppendToDest = "cat " + sourceFileName + " >> " + destinationFileName;
        parser.processInput(commandAppendToDest);

        String expectedContent = "This is the first line." + System.lineSeparator() + "This is the first line." + System.lineSeparator();
        assertEquals(expectedContent, Files.readString(destinationFilePath));
    }

    @Test
    public void testCat() throws IOException {
        String fileName = "test.txt";
        String command = "cat > " + fileName;

        String Input = "This is a test line." + System.lineSeparator() + "Exit" + System.lineSeparator();
        System.setIn(new ByteArrayInputStream(Input.getBytes()));

        parser.processInput(command);

        Path filePath = testDir.resolve(fileName);
        assertTrue(Files.exists(filePath));

        String expectedContent = "This is a test line." + System.lineSeparator();
        assertEquals(expectedContent, Files.readString(filePath));

        StringBuilder output = new StringBuilder();
        String Command = "cat " + fileName;
        String[] tokens = Command.split("\\s+");

        parser.cat(tokens,output);
        assertEquals(expectedContent,output.toString());

    }

    @Test
    public void testPipe() throws IOException {
        String dirName = "test.txt";

        parser.processInput("mkdir " + dirName + " | ls");

        Path newDirPath = testDir.resolve(dirName);
        assertTrue(Files.isDirectory(newDirPath));

        StringBuilder output = new StringBuilder();
        parser.ls(new String[]{"ls"}, output);

        assertTrue(output.toString().contains(dirName));
    }

    @Test
    void testRmdirWithoutArguments() {
        String[] tokens = {"rmdir"};
        parser.rmdir(tokens, output);

        assertEquals("Usage: rmdir <dir>" + System.lineSeparator(), output.toString());
    }

    @Test
    void testRmdirNonExistentDirectory() {
        String[] tokens = {"rmdir", "nonExistentDir"};
        parser.rmdir(tokens, output);

        assertEquals("Directory does not exist: nonExistentDir" + System.lineSeparator(), output.toString());
    }

    @Test
    void testRmdirEmptyDirectory() throws IOException {
        Path emptyDir = Files.createDirectory(testDir.resolve("emptyDir"));
        String[] tokens = {"rmdir", "emptyDir"};

        parser.rmdir(tokens, output);

        assertTrue(Files.notExists(emptyDir));
        assertEquals("", output.toString());
    }

    @Test
    void testRmdirNonEmptyDirectory() throws IOException {
        Path nonEmptyDir = Files.createDirectory(testDir.resolve("nonEmptyDir"));
        Files.createFile(nonEmptyDir.resolve("file.txt"));
        String[] tokens = {"rmdir", "nonEmptyDir"};

        parser.rmdir(tokens, output);

        assertTrue(Files.exists(nonEmptyDir));
        assertEquals("Directory not empty: nonEmptyDir" + System.lineSeparator(), output.toString());
    }

    @Test
    public void testLs() throws IOException {
        String fileName1 = "file1.txt";
        String fileName2 = "file2.txt";

        parser.processInput("touch " + fileName1);
        parser.processInput("touch " + fileName2);

        StringBuilder output = new StringBuilder();
        parser.ls(new String[]{"ls"}, output);
        assertEquals(fileName1 + System.lineSeparator() + fileName2 + System.lineSeparator(), output.toString());
    }

    @Test
    public void testLsAll() throws IOException {
        String fileName1 = "file1.txt";
        String fileName2 = "file2.txt";
        String hiddenFile = ".hiddenFile";

        parser.processInput("touch " + fileName1);
        parser.processInput("touch " + fileName2);
        parser.processInput("touch " + hiddenFile);

        StringBuilder output = new StringBuilder();
        parser.ls(new String[]{"ls", "-a"}, output);
        assertTrue(output.toString().contains(hiddenFile), "Hidden file not found in ls -a output");
    }

    @Test
    public void testLsReverse() throws IOException {
        String fileName1 = "file1.txt";
        String fileName2 = "file2.txt";

        parser.processInput("touch " + fileName1);
        parser.processInput("touch " + fileName2);

        StringBuilder output = new StringBuilder();
        parser.ls(new String[]{"ls", "-r"}, output);
        assertEquals(fileName2 + System.lineSeparator() + fileName1 + System.lineSeparator(), output.toString());
    }
}
