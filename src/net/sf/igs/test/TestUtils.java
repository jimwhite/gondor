/**
 * 
 */
package net.sf.igs.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.igs.CondorExecException;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;

/**
 * 
 */
public class TestUtils {
	
	// Okay, this regular expression is rather ugly, but it is used to extract
	// the status of a job on the grid when processing a line output from the
	// 'condor_q' utility from Condor. The groups (in parentheses) are as follows:
	// 1 - The job ID
	// 2 - The job owner's username
	// 3 - The date submitted
	// 4 - The time submitted
	// 5 - The run time
	// 6 - The status code
	private static Pattern condorQueueRegex =
		Pattern.compile("^(\\d+\\.\\d+)\\s+(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s(\\w)");
	
	/*
	 * Start a job and return the job ID.
	 */
	public static String createJob(Session session, String jobName) throws DrmaaException {
		JobTemplate jt = getSleepJobTemplate(session, jobName);

		String jobId = session.runJob(jt);

		// Free job template resources
		session.deleteJobTemplate(jt);

		return jobId;
	}
	
	/*
	 * Return a job template that is configured to execute /bin/sleep for 5 minutes
	 * on the grid. The job template is ready to be submitted through the session that
	 * is passed in as an argument.
	 */
	public static JobTemplate getSleepJobTemplate(Session session, String name) throws DrmaaException {
		JobTemplate jt = session.createJobTemplate();

		// Create a job that will sleep for 5 minutes
		jt.setRemoteCommand("/bin/sleep");
		jt.setArgs(Collections.singletonList("300"));
		
		// Set the job name
		jt.setJobName(name);
		
		return jt;
	}
	
	/*
	 * Start a job, suspend it, and return the job ID. This is useful for the 
	 * "release" related tests.
	 */
	public static String createHeldJob(Session session, String jobName) throws DrmaaException, CondorExecException {
		try {
			JobTemplate jt = getSleepJobTemplate(session, jobName);

			String jobId = session.runJob(jt);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Sleep a little...
			Thread.sleep(2000);
			
			// Try putting the job on hold
			session.control(jobId, Session.HOLD);
			
			// TODO: Maybe have a loop here and check several times if the
			// first attempt shows the job isn't held yet...
			// Sleep a little more...
			Thread.sleep(2000);
			
			boolean held = isJobHeld(jobId);
			if (! held) {
				throw new CondorExecException("Unable to create a held job."); 
			}
			
			return jobId;
		} catch (InterruptedException ie) {
			throw new CondorExecException("Interrupted.", ie);
		}
	}
	
	/**
	 * Returns the status code for a particular job ID specified by the caller.
	 * The method determines this status code by executing "condor_q" and
	 * parsing the results.
	 */
	public static String getJobStatusCode(String jobId) throws CondorExecException {
		String statusCode = null;
		
		// Set up the command to run, with arguments. The only argument in this
		// case is the job ID.
		String[] command = new String[2];
		command[0] = "condor_q";
		command[1] = jobId;

		int exitValue;
		try {
			Process condorQueue = Runtime.getRuntime().exec(command);
			exitValue = condorQueue.waitFor();

			if (exitValue == 0) {
				// Process the output of the command
		    	Reader reader = new InputStreamReader(condorQueue.getInputStream());
		    	BufferedReader bufReader = new BufferedReader(reader);
		    	String line = null;
		    	
		    	while ((line = bufReader.readLine()) != null) {
		    		// Ignore lines that don't begin with the job ID. These lines
		    		// are either just blank or contain the output header.
		    		line = line.trim();
		    		if (line.startsWith(jobId)) {
		    			Matcher matcher = condorQueueRegex.matcher(line);
		    			if (matcher.find()) {
		    				statusCode = matcher.group(6);
		    			}
		    		}
		    	}
		    	bufReader.close();
			} else {
				// The condor_q command failed completely...
				throw new CondorExecException("The condor_q command exited abnormally with exit value " + exitValue);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new CondorExecException("Interrupted.", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CondorExecException(e.getMessage(), e);
		}
		
		return statusCode;
	}
	
	/**
	 * Confirm whether a particular job, specified with a job ID, is on the grid
	 * or not. We do NOT rely on our own DRMAA implementation to determine this.
	 * 
	 * @param jobId the Condor job ID to check
	 * @return a boolean
	 * @throws CondorExecException 
	 */ 
	public static boolean isJobPresent(String jobId) throws CondorExecException {
		boolean present = false;
		
		// Set up the command to run, with arguments. The only argument in this
		// case is the job ID.
		String[] command = new String[2];
		command[0] = "condor_q";
		command[1] = jobId;

		int exitValue;
		try {
			Process condorQueue = Runtime.getRuntime().exec("condor_q");
			exitValue = condorQueue.waitFor();

			if (exitValue == 0) {
		    	Reader reader = new InputStreamReader(condorQueue.getInputStream());
		    	BufferedReader bufReader = new BufferedReader(reader);
		    	String line = null;
		    	
		    	while ((line = bufReader.readLine()) != null) {
		    		// Ignore lines that don't begin with the job ID. These lines
		    		// are either just blank or contain the output header.
		    		line = line.trim();
		    		if (line.startsWith(jobId)) {
		    			present = true;
		    		}
		    	}
		    	bufReader.close();
			} else {
				// The 'condor_q' command failed completely...
				throw new CondorExecException("The 'condor_q' command exited abnormally with exit value " + exitValue);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new CondorExecException("Interrupted.", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CondorExecException(e.getMessage(), e);
		}
		
		return present;
	}
	
	/**
	 * Determine if a job is running.
	 * 
	 * @param jobId 
	 * @return a boolean
	 * @throws CondorExecException 
	 */
	public static boolean isJobRunning(String jobId) throws CondorExecException {
		boolean running = false;
		// Get the job status code, and then see if it has an 'r' character in it.
		// The 'r' character means that the job is running.
		String statusCode = getJobStatusCode(jobId);
		if (statusCode != null && (statusCode.length() > 0) && statusCode.toLowerCase().contains("r")) {
			running = true;
		}
		return running;
	}
	
	/**
	 *  Determine if a job is in a suspended or held state.
	 *
	 * @param jobId the Condor job ID to check
	 * @return a boolean
	 * @throws CondorExecException
	 */
	public static boolean isJobHeld(String jobId) throws CondorExecException {
		boolean held = false;
		// Get the job status code, and then see if it has an 'h' character in it.
		// The 'h' character means that the job is on hold.
		String statusCode = getJobStatusCode(jobId);
		if (statusCode != null && (statusCode.length() > 0) && statusCode.toLowerCase().contains("h")) {
			held = true;
		}
		return held;
	}
	
	/**
	 * Remove a job from Condor by executing "condor_rm". We do this
	 * in order to remove the jobs that have been placed on the grid
	 * by running this test class. The method makes no attempt at
	 * checking whether the specified job ID exists or whether it is
	 * running or not. In fact, all errors are silently ignored...
	 * 
	 * @param jobId the Condor job ID to remove
	 */
	public static void removeJob(String jobId) {
		String[] removeCmd = new String[2];
		removeCmd[0] = "condor_rm";
		removeCmd[1] = jobId;

		try {
			Process condorQueue = Runtime.getRuntime().exec(removeCmd);
			int exitValue = condorQueue.waitFor();
			if (exitValue != 0) {
				System.err.println("Problem executing condor_rm.");
			}
		} catch (Exception e) {
			// ignored (see comments above)
		}
	}
}
