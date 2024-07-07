package org.c2graph.util;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ParserUtil {
    public static void main(String[] args) {
        parseFilesInDirectory("G:\\Github\\Research-Classroom\\src\\Code2Graph\\C2Graph\\src\\main\\resources\\Memory\\leak");
    }

    public static ArrayList<String> parseFilesInDirectory(String directory) {
        File folder = new File(directory);
        if (!folder.isAbsolute()) {
            directory = Paths.get("config.json").toAbsolutePath().getParent().toString() + "\\" + directory;
            folder = new File(directory);
        }
        File[] files = folder.listFiles();
        ArrayList<String> parseFiles = new ArrayList<>();

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
