import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SortingTypes {
    private static String outputPath = "./";
    private static String prefix = "";
    private static boolean appendMode = false;
    private static boolean fullStats = false;
    private static final Map<String, String> fileMap = new HashMap<>();
    private static final Map<String, Statistics> statsMap = new HashMap<>();

    static {
        fileMap.put("Integer", "int.txt");
        fileMap.put("Float", "float.txt");
        fileMap.put("String", "String.txt");
    }

    public static void readFiles(String[] userRequest) {
        List<String> inputFiles = new ArrayList<>();

        for (int i = 0; i < userRequest.length; i++) {
            switch (userRequest[i]){
                case "-o":
                    outputPath += userRequest[++i];
                    break;
                case "-p":
                    prefix = userRequest[++i];
                    break;
                case "-a":
                    appendMode = true;
                    break;
                case "-f":
                    fullStats = true;
                    break;
                default:
                    if (userRequest[i].endsWith(".txt")){
                        inputFiles.add(userRequest[i]);
                    }
                    break;
            }
        }

        validateOutputPath(outputPath);
        if (inputFiles.isEmpty()) {
            throw new RuntimeException("Нет передаваемых файлов");
        }

        if (!appendMode) {
            resetStatistics();
        } else {
            loadExistingStatistics();
        }

        List<String> lines = new ArrayList<>();
        for (String inputFile : inputFiles) {
            try (BufferedReader file = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("./" + inputFile))))) {
                while (file.ready()) {
                    lines.add(file.readLine());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (String line : lines) {
            if (writeToFileIfParsed(line, "Integer", Integer::parseInt)) continue;
            if (writeToFileIfParsed(line, "Float", Float::parseFloat)) continue;
            writeToFile(line, "String");
            statsMap.get("String").update(line);
        }

        printStatistics();
    }

    private static <T> boolean writeToFileIfParsed(String line, String type, Parser<T> parser) {
        try {
            T value = parser.parse(line);
            writeToFile(line, type);
            statsMap.get(type).update(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeToFile(String content, String type) {
        String fileName = prefix + fileMap.get(type);
        if (fileName == null) return;

        File file = new File(outputPath, fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(content + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка записи в файл: " + fileName, e);
        }
    }

    private static void printStatistics() {
        statsMap.forEach((type, stats) -> {
            System.out.println("Статистика для " + type + ":");
            stats.print(fullStats);
        });
    }

    private static void resetStatistics() {
        statsMap.put("Integer", new Statistics());
        statsMap.put("Float", new Statistics());
        statsMap.put("String", new Statistics());
    }

    private static void loadExistingStatistics() {
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            File file = new File(outputPath, prefix + entry.getValue());

            statsMap.putIfAbsent(entry.getKey(), new Statistics());

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (entry.getKey().equals("Integer")) {
                            statsMap.get("Integer").update(Integer.parseInt(line));
                        } else if (entry.getKey().equals("Float")) {
                            statsMap.get("Float").update(Float.parseFloat(line));
                        } else {
                            statsMap.get("String").update(line);
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Ошибка чтения данных для " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void validateOutputPath(String path) {
        Path outputDir = Paths.get(path);
        if (Files.notExists(outputDir)) {
            throw new RuntimeException("Указанный путь не существует: " + path);
        }
        if (!Files.isDirectory(outputDir)) {
            throw new RuntimeException("Указанный путь не является директорией: " + path);
        }
        if (!Files.isWritable(outputDir)) {
            throw new RuntimeException("Нет прав на запись в директорию: " + path);
        }
    }

    @FunctionalInterface
    interface Parser<T> {
        T parse(String value) throws Exception;
    }
}

class Statistics {
    private int count = 0;
    private Double min = Double.MAX_VALUE;
    private Double max = Double.MIN_VALUE;
    private double sum = 0;
    private int minLength = Integer.MAX_VALUE;
    private int maxLength = Integer.MIN_VALUE;

    public void update(Object value) {
        count++;
        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            min = Math.min(min, num);
            max = Math.max(max, num);
            sum += num;
        } else if (value instanceof String) {
            int length = ((String) value).length();
            minLength = Math.min(minLength, length);
            maxLength = Math.max(maxLength, length);
        }
    }

    public void print(boolean fullStats) {
        System.out.println("Количество: " + count);
        if (fullStats) {
            if (min != Double.MAX_VALUE) {
                System.out.println("Мин: " + min + ", Макс: " + max + ", Сумма: " + sum + ", Среднее: " + (sum / count));
            } else {
                System.out.println("Мин. длина: " + minLength + ", Макс. длина: " + maxLength);
            }
        }
        System.out.println();
    }
}


