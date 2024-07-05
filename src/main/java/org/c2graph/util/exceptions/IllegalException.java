package org.c2graph.util.exceptions;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.c2graph.util.debug.AnsiColors;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * We use this as an aspect-oriented class to
 * explicitly convey a message that this is illegal.
 */
public class IllegalException extends RuntimeException {

    public static final String NOT_IMPLEMENT_YET = "Not implement yet:";

    public IllegalException(String message) {
        super(message);
    }

    public IllegalException(String message, IASTNode node) {
        super(message);
        System.out.println(AnsiColors.ANSI_RED + "in line:" + node.getFileLocation().getStartingLineNumber());
    }
}
