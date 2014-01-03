package net.sf.igs;

/*
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility functions for the Condor-JDRMAA project.
 *
 */
public class Util {

	/**
	 * The system's temp directory as determined by the JVM's "java.io.tmpdir" system property.
	 */
	public static final String TMP = System.getProperty("java.io.tmpdir");
	
    /**
     * The prefix that all log files for submitted Condor jobs will have.
     */
    public static final String LOG_FILE_PREFIX = "condor_drmaa_";
    
	/**
	 * The log file template for submitted Condor jobs.
	 */
	public static final String LOG_TEMPLATE = TMP + File.separator + LOG_FILE_PREFIX + "$(Cluster).$(Process).log";
    
	// This is the regular expression used to match the job id from the job's log file
	// The job id looks like this: (123.000.000)  The first set of 3 digits is the job
	// id.
	private static Pattern pattern = Pattern.compile("\\((\\d{3})\\.\\d{3}\\.\\d{3}\\)");
	
    /**
     * Determines if a job ID is valid or not.
     * 
     * @param jobId a <code>String</code>
     * @return a <code>boolean</code>
     */
    public static boolean validJobId(String jobId) {
    	boolean valid = false;
    	if (jobId != null && jobId.length() > 0) {
    		valid = true;
    	}
    	return valid;
    }
    
    /**
     * Given a condor job ID of a job submitted through Condor-JDRMAA, return the path to
     * the log file that belongs to it. There is no guarantee that the log file exists or
     * not as it is simply where the log file should be, for instance, if the job is not
     * yet submitted...
     * 
     * @param jobId a <code>String</code> containing the job ID.
     * @return a <code>String</code> with the absolute path.
     */
    public static String getLogFromId(String jobId) {
    	String log = TMP + File.separator + LOG_FILE_PREFIX + jobId + ".log";
    	return log;
    }

	/**
	 * Given a Condor log file, determine the job Id.
	 * 
	 * @param logFile a {@link File}
	 * @return a <code>String</code> containing the job ID.
	 * @throws IOException
	 */
	public static String getIdFromLog(File logFile) throws IOException {
		String jobId = null;
		
		if (logFile.exists() & logFile.isFile() && logFile.canRead()) {
			BufferedReader reader = new BufferedReader(new FileReader(logFile));
			
			String line = null;
			while ( (line = reader.readLine()) != null) {
				if (line.contains("Job submitted")) {
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						jobId = matcher.group(1);
					}
				}
			}
		} else {
			throw new IOException(logFile.getAbsolutePath() + " doesn't exist or isn't readable.");
		}
		return jobId;
	}

    /**
     * Deletes all files and subdirectories under the given directory.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     * 
     * @param dir a {@link File} with the directory to delete
     * @return a <code>boolean</code>
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int childIndex = 0; childIndex < children.length; childIndex++) {
                boolean success = deleteDir(new File(dir, children[childIndex]));
                if (! success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }

	/**
	 * Checks whether a Condor installation is available by checking the
	 * path (PATH environment variable) for the presence of "condor_submit".
	 * 
	 * @return a <code>boolean</code>
	 */
	public static boolean isCondorAvailable() {
    	boolean available = false;
    	String path = System.getenv("PATH");
    	String[] pathDirs = path.split(":");
    	for (String pathDir : pathDirs) {
			File condorSubmit = new File(pathDir, "condor_submit");
			if (condorSubmit.exists()) {
				available = true;
				break;
			}
		}
    	return available;
	}
	
	/**
	 * Get the current timestamp in seconds (from the epoch).
	 * 
	 * @return a <code>long</code>
	 */
	public static long getSecondsFromEpoch() {
		long now = System.currentTimeMillis() / 1000;
		return now;
	}
}
