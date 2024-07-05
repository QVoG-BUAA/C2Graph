package org.c2graph.config;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;

public class IncludeFileContentProvider extends InternalFileContentProvider {
    @Override
    public InternalFileContent getContentForInclusion(String path, IMacroDictionary macroDictionary) {
        return getContentUncached(path);
    }

    @Override
    public InternalFileContent getContentForInclusion(IIndexFileLocation ifl, String astPath) {
        return getContentUncached(astPath);
    }

    private InternalFileContent getContentUncached(String path) {
        if (!getInclusionExists(path)) {
            return null;
        }

        System.out.println("Loading include file " + path);
        FileContent content = FileContent.createForExternalFileLocation(path);
        return content instanceof InternalFileContent ? (InternalFileContent) content : null;
    }
}