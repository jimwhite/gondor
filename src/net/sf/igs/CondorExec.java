package net.sf.igs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggf.drmaa.InternalException;

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

/**
 * Class that contains methods for running basic Condor operations.
 *
 */
public class CondorExec {
	
	/**
	 * Kill a Condor job.
	 * 
	 * @param jobID a <code>String</code> with the Condor job ID to terminate.
	 * @throws CondorExecException
	 * @see "The 'condor_rm' documentation or man page"
	 */
	public static void terminate(String jobID) throws CondorExecException {
		String[] releaseCmd = new String[2];
		releaseCmd[0] = "condor_rm";
		releaseCmd[1] = jobID;

		Process proc;
		try {
			proc = Runtime.getRuntime().exec(releaseCmd);
			proc.waitFor();
			// Check that the return value was 0.
			int exitValue = proc.exitValue();
			if (exitValue != 0) {
				throw new CondorExecException();
			}
		} catch (IOException ioe) {
			throw new CondorExecException("I/O problem occurred when running condor_rm.", ioe);
		} catch (InterruptedException e) {
			throw new CondorExecException("Interrupted", e);
		}
	}
	
	/**
	 * Release a Condor job that has been held.
	 * 
	 * @param jobID a <code>String</code> with the Condor job ID to release.
	 * @throws CondorExecException
	 * @see "The 'condor_release' man page"
	 * @see "The 'condor_hold' man page"
	 * @see #suspend(String)
	 */
	public static void release(String jobID) throws CondorExecException {
		String program = "condor_release";
		runSimpleCondorBinary(program, jobID);
	}
	
	private static void runSimpleCondorBinary(String program, String jobID) throws CondorExecException {
		String[] command = new String[2];
		command[0] = program;
		command[1] = jobID;
		
		Process releaseProc;
		try {
			releaseProc = Runtime.getRuntime().exec(command);
			releaseProc.waitFor();
			// Check that the return value was 0.
			int exitValue = releaseProc.exitValue();
			if (exitValue != 0) {
				throw new CondorExecException("Program " + program + " exited with value " + exitValue);
			}
		} catch (IOException ioe) {
			throw new CondorExecException("I/O problem occurred when running condor_release.", ioe);
		} catch (InterruptedException e) {
			throw new CondorExecException("Interrupted", e);
		}
	}
	
	/**
	 * Suspend a running or waiting Condor job. After a job is suspended, it can be
	 * released with the {@link #release(String) release} method.
	 * 
	 * @param jobID a <code>String</code> containing the Condor job ID to suspend.
	 * @throws CondorExecException
	 * @see "The 'condor_hold' man page"
	 * @see #release(String)
	 */
	public static void suspend(String jobID) throws CondorExecException {
		String program = "condor_hold";
		runSimpleCondorBinary(program, jobID);
	}
	
	/**
	 * Submit a job to Condor using the specified submit file.
	 * 
	 * @param submitPath a <code>String</code> with the path to the submit file.
	 * @return a <code>String</code> containing a Condor job ID.
	 * @throws CondorExecException
	 * @see "The 'condor_submit' man page"
	 */
	public static String submit(String submitPath) throws CondorExecException {    	
    	String jobID = null;
    	
    	try {
        	String[] command = {"condor_submit", submitPath};
        	Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			int exitValue = process.exitValue();
			
			if (exitValue == 0) {
				// The condor_submit command succeeded. It ran and exited 0...
		    	Reader reader = new InputStreamReader(process.getInputStream());
		    	BufferedReader bufReader = new BufferedReader(reader);
		    	String line = null;
		    	
		    	// Read through the output
		    	while ( (line = bufReader.readLine()) != null ) {
		    		if (line.contains("submitted to cluster")) {
		    			Pattern pattern = Pattern.compile("\\d+\\.$");
		    			Matcher matcher = pattern.matcher(line);
		    			if (matcher.find()) {
		    				jobID = matcher.group();
		    				jobID = jobID + "0";
		    			}
		    		}
		    	}
		    	bufReader.close();
			} else {
				// The condor_submit invocation failed.
				throw new InternalException("condor_submit failed. Exit value: " + exitValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Wrap the exception that we got.
			throw new CondorExecException(e.getMessage(), e);
		}

		return jobID;
	}
	
	/**
	 * Given a {@link File} describing a Condor submit file, submit the job to
	 * Condor for execution.
	 * 
	 * @param submitFile a {@link File}
	 * @return a <code>String</code> containing a Condor job ID.
	 * @throws CondorExecException
	 * @see "The 'condor_submit' man page"
	 */
	public static String submit(File submitFile) throws CondorExecException {
		if (submitFile.exists() && submitFile.isFile() && submitFile.canRead()) {
			return submit(submitFile.getAbsolutePath());
		} else {
			throw new IllegalArgumentException("Submit file doesn't exist or isn't a readable file.");
		}
	}
}
