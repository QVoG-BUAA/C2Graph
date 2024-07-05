package org.c2graph;

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.c2graph.config.ProjectConfig;
import org.c2graph.model.C2Graph;
import org.c2graph.util.GremlinUtil;
import org.c2graph.util.ParserUtil;
import org.c2graph.util.debug.AnsiColors;
import org.c2graph.util.debug.DebugUtil;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

/**
 * read the config from config.json and start to parse it.
 */
public class Main {
    private final ProjectConfig config;
    public static GraphTraversalSource g;

    public Main() throws Exception {
        System.out.println(AnsiColors.ANSI_CYAN + "C2Graph begins at: "
                + DebugUtil.getCurrentTime() + "." + AnsiColors.ANSI_RESET);

        config = ProjectConfig.loadConfig();

        System.out.println(AnsiColors.ANSI_RED + "Try to connect to " + config.getHost() + ":" + config.getPort() + "." + AnsiColors.ANSI_RESET);

        g = traversal().withRemote(DriverRemoteConnection.using(config.getHost(), config.getPort(), "g"));

        System.out.println(AnsiColors.ANSI_RED + "Connect success at " + DebugUtil.getCurrentTime() + "." + AnsiColors.ANSI_RESET);
        System.out.println(AnsiColors.ANSI_RED + "Initialize/Clear the graph at " + DebugUtil.getCurrentTime() + "." + AnsiColors.ANSI_RESET);

        int batchSize = 100000;
        while (true) {
            long count = g.V().limit(batchSize).count().next();
            g.V().limit(count).drop().iterate();
            if (count < batchSize) {
                break;
            }
        }

        System.out.println(AnsiColors.ANSI_RED + "Initialize/Clear the graph success at " + DebugUtil.getCurrentTime() + "." + AnsiColors.ANSI_RESET);
        System.out.println("--------------------------------");
        long startTime = System.currentTimeMillis();

        parseDir();

        g.tx().commit();
        g.close();

        long duration = (System.currentTimeMillis() - startTime + 999) / 1000;
        long hs = duration / 3600;
        long ms = (duration % 3600) / 60;
        long ss = duration % 60;

        System.out.println("--------------------------------");
        System.out.println(AnsiColors.ANSI_CYAN + "System exits successfully at " + DebugUtil.getCurrentTime() + ", time costs " + hs + "h " + ms + "m " + ss + "s. " + AnsiColors.ANSI_RESET);
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public void parseDir() throws Exception {
        ArrayList<String> fileList = ParserUtil
                .parseFilesInDirectory(config.getProject() + config.getDir());
        ArrayList<C2Graph> c2Graphs = new ArrayList<>();

        System.out.println("there are " + fileList.size() + " files need to handle.");
        for (String filePath : fileList) {
            if (!c2Graphs.isEmpty()) {
                c2Graphs.get(c2Graphs.size() - 1).edgeCleaUp();
            }
            System.out.println("--------------------------------");
            String newPath = filePath.substring(config.getProject().length());
            System.out.println(newPath + " starts at " + DebugUtil.getCurrentTime());

            IASTTranslationUnit unit = getASTTranslationUnit(filePath);

            System.out.println(newPath + " starts to build at " + DebugUtil.getCurrentTime());

            C2Graph c2Graph = new C2Graph(unit, newPath);
            c2Graph.build();
            c2Graphs.add(c2Graph);
        }

        System.out.println("--------------------------------");
        System.out.println("waiting for the cleanup to finish at " + DebugUtil.getCurrentTime());
        for (C2Graph c2Graph : c2Graphs) {
            c2Graph.neo4jCleanUp();
        }
        GremlinUtil.addCgReturnEdge(null, true);
        GremlinUtil.nodeCleanUp(null, true);
    }

    /**
     * refer to [CDT Visualizer], call some api of CDT to get AST tree.
     */
    public IASTTranslationUnit getASTTranslationUnit(String filePath) throws CoreException {
        FileContent fileContent = FileContent.createForExternalFileLocation(filePath);

        HashMap<String, String> definedSymbols = new HashMap<>();
        ArrayList<String> includePaths = new ArrayList<>();
        includePaths.add(config.getProject() + config.getDir());
        includePaths.addAll(Arrays.asList(config.getIncludePath()));

        ExtendedScannerInfo info = new ExtendedScannerInfo(definedSymbols, includePaths.toArray(new String[0]));
        IParserLogService logger = new DefaultLogService();
        IncludeFileContentProvider includeFileContentProvider = new org.c2graph.config.IncludeFileContentProvider();

        int opts = ILanguage.OPTION_IS_SOURCE_UNIT;

        if (filePath.endsWith(".c"))
            return GCCLanguage.getDefault().getASTTranslationUnit(fileContent, info, includeFileContentProvider, null, opts, logger);
        return GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, includeFileContentProvider, null, opts, logger);
    }
}