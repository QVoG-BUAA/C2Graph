package org.c2graph.util.debug;

import org.c2graph.config.ProjectConfig;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DebugUtil {

    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static String getCurrentTime() {
       return LocalDateTime.now().format(formatter);
    }

    public static String projectName;
    public static int lastLineNumber;
    public static String lastFileName;

    public static void printNodeInfo(IASTNode node, String message) {
        System.out.println(message + " " + node.getContainingFilename() + " : " + node.getFileLocation().getStartingLineNumber());
    }

    public static void printNodeInfo(IASTNode node) {
        System.out.println(node.getContainingFilename() + " : " + node.getFileLocation().getStartingLineNumber());
    }

    public static void printNodeInfo(IASTNode source, IASTNode sink) {
        String sourceFile = source.getContainingFilename();
        String sinkFile = sink.getContainingFilename();
        int sourceSt = source.getFileLocation().getStartingLineNumber();
        int sinkSt = sink.getFileLocation().getStartingLineNumber();
        if (Objects.equals(sourceFile, sinkFile)) {
            if (sourceSt < sinkSt) {
                System.out.println("def: " + sourceFile + " : " + sourceSt);
                System.out.println("use: " + sinkFile + " : " + sinkSt);
            }
        } else {
            System.out.println("def: " + sourceFile + " : " + sourceSt);
            System.out.println("use: " + sinkFile + " : " + sinkSt);
        }
    }

    public static void printNodeInfoEveryThousand(IASTNode node) {
        if (projectName == null) {
            projectName = ProjectConfig.loadConfig().getProject();
        }
        String containingFilename = node.getContainingFilename();
        if (containingFilename.startsWith(projectName)) {
            containingFilename = containingFilename.substring(projectName.length());
        } else {
            return;
        }
        if (lastFileName != null) {
            if (!containingFilename.equals(lastFileName)) {
                lastLineNumber = 0;
            }
        }
        int nodeLine = node.getFileLocation().getStartingLineNumber();

        if (nodeLine - lastLineNumber >= 1000) {
            lastLineNumber = nodeLine - nodeLine % 1000;
            System.out.println(AnsiColors.ANSI_CYAN + containingFilename + " is handing node at " + nodeLine + " now." + AnsiColors.ANSI_RESET);
        }
        lastFileName = containingFilename;
    }
}
