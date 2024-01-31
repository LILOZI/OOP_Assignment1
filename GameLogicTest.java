import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GameLogicTest {
    static Stream<GameLogicTest.ComparisonData> comparisonData() {
        File inputDirectory = new File("src/test/resources/input");
        File outputDirectory = new File("src/test/resources/output");
        if (!inputDirectory.exists() || !outputDirectory.exists()) {
            Assertions.fail("Input or output directory not found");
        }

        File[] inputFiles = inputDirectory.listFiles();
        if (inputFiles == null) {
            Assertions.fail("No input files found");
        }

        return Stream.of(inputFiles).filter(File::isFile).map((inputFile) -> {
            String outputFileName = inputFile.getName().replace("input", "output");
            File outputFile = new File(outputDirectory, outputFileName);
            if (outputFile.exists()) {
                return new GameLogicTest.ComparisonData(inputFile, outputFile);
            } else {
                Assertions.fail("Corresponding output file not found: " + outputFileName);
                return null;
            }
        });
    }

    @ParameterizedTest
    @MethodSource({"comparisonData"})
    void testMove(GameLogicTest.ComparisonData comparisonData) {
        File inputFile = comparisonData.inputFile();
        File outputFile = comparisonData.outputFile();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        PlayableLogic gameLogic = new GameLogic();
        String inputContent = this.readFile(inputFile);
        List<Position> moves = parse(inputContent);

        for(int i = 0; i < moves.size() - 1; i += 2) {
            Position from = (Position)moves.get(i);
            Position to = (Position)moves.get(i + 1);
            boolean result = gameLogic.move(from, to);
            Assertions.assertTrue(result);
        }

        System.setOut(System.out);
        String capturedOutput = outputStream.toString();
        String expectedOutput = this.readFile(outputFile);
        capturedOutput = capturedOutput.replaceAll("\r", "");
        Assertions.assertEquals(expectedOutput, capturedOutput);
    }

    private String readFile(File file) {
        StringBuilder content = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line;
            try {
                while((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (Throwable var7) {
                try {
                    reader.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }

                throw var7;
            }

            reader.close();
        } catch (IOException var8) {
            var8.printStackTrace();
            Assertions.fail("Error reading file: " + file.getName());
        }

        return content.toString();
    }

    public static List<Position> parse(String movesString) {
        List<Position> positions = new ArrayList();
        Pattern pattern = Pattern.compile("\\((\\d+), (\\d+)\\)");
        Matcher matcher = pattern.matcher(movesString);

        while(matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            positions.add(new Position(x, y));
        }

        return positions;
    }

    public static record ComparisonData(File inputFile, File outputFile) {
        public ComparisonData(File inputFile, File outputFile) {
            this.inputFile = inputFile;
            this.outputFile = outputFile;
        }

        public File inputFile() {
            return this.inputFile;
        }

        public File outputFile() {
            return this.outputFile;
        }
    }
}