package org.c2graph.util;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ParserUtil {
    public static void main(String[] args) {
        parseFilesInDirectory("G:\\Github\\Research-Classroom\\src\\Code2Graph\\C2Graph\\src\\main\\resources\\Memory\\leak");
    }

    public static boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static ArrayList<String> parseFilesInDirectory(String directory) {
        ArrayList<String> parseFiles = new ArrayList<>();
        File folder = new File(directory);
        if (folder.isFile()) {
            String parseFile = checkExtension(folder.getPath());
            if (parseFile != null) {
                parseFiles.add(parseFile);
            }
        }

        if (!folder.isAbsolute()) {
            boolean isWindows = isWindows();
            Path path = Paths.get("config.json");
            if (isWindows) {
                directory = path.toAbsolutePath().getParent().toString() + "\\" + directory;
            } else {
                directory = path.toAbsolutePath().getParent().toString() + "/" + directory;
            }
            folder = new File(directory);
        }
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String parseFile = checkExtension(file.getPath());
                    if (parseFile != null) {
                        parseFiles.add(parseFile);
                    }
                } else if (file.isDirectory()) {
                    parseFiles.addAll(parseFilesInDirectory(file.getPath()));
                }
            }
        }
        return parseFiles;
    }

    public static String checkExtension(String filePath) {
        File file = new File(filePath);
        String filename = file.getName();
        String[] splitName = filename.split("\\.");
        String oldExtension = splitName.length > 1 ? "." + splitName[splitName.length - 1] : "";

        if (!oldExtension.equals(".c")
//                && !oldExtension.equals(".cpp")
        ) {
            return null;
        }
        return filePath;
    }
}
