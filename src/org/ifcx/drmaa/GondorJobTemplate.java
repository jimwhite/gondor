package org.ifcx.drmaa;

import org.ggf.drmaa.JobTemplate;

import java.util.List;

/**
 * Created by jim on 8/2/14.
 */
public interface GondorJobTemplate extends JobTemplate {
    GondorJobTemplate inFile(String path);
    GondorJobTemplate inDir(String path);
    GondorJobTemplate outFile(String path);
    GondorJobTemplate outDir(String path);

    List<String> getInFilePaths();
    List<String> getInDirPaths();
    List<String> getOutFilePaths();
    List<String> getOutDirPaths();
}
