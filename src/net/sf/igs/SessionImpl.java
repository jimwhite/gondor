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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggf.drmaa.AlreadyActiveSessionException;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.DrmsInitException;
import org.ggf.drmaa.ExitTimeoutException;
import org.ggf.drmaa.HoldInconsistentStateException;
import org.ggf.drmaa.InternalException;
import org.ggf.drmaa.InvalidJobException;
import org.ggf.drmaa.InvalidJobTemplateException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.NoActiveSessionException;
import org.ggf.drmaa.ResumeInconsistentStateException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SimpleJobTemplate;
import org.ggf.drmaa.Version;

/**
 * The SessionImpl class provides a DRMAA interface to Condor.
 *
 * @see org.ggf.drmaa.Session
 * @see net.sf.igs.JobTemplateImpl
 * @see "http://www.cs.wisc.edu/condor/"
 */
public class SessionImpl implements Session {
	
    /**
     * This is the name of the system property that should be set if debugging
     * for the library should be activated. When debugging mode is on, generated
     * condor submit files are not removed after submission...
     */
    public static final String CONDOR_JDRMAA_DEBUG = "condor.jdrmaa.debug";


    private static final String SESSION_DIR_PREFIX = "jdrmaa_session-";
    /**
     * The prefix that all log files for submitted Condor jobs will have.
     */
    public static final String LOG_FILE_PREFIX = "jdrmaa_log-";
    /**
	 * The log file template for submitted Condor jobs.
	 */
	public static final String LOG_TEMPLATE = LOG_FILE_PREFIX + "$(Cluster).$(Process).log";

    private static final String SUBMIT_FILE_SUFFIX = ".job";

    private static final String SESSION_JOBS_DIR_NAME = "jobs";

	private static final String DRM_SYSTEM = "Condor";

    private static final String CONDOR_JDRMAA_DEFAULT_CONTACT_SUFFIX = "@condor_jdrmaa";

    private String contact;
    private boolean activeSession = false;
    private File sessionDir;
    private File session_jobs_dir;
    private int jobTemplateId = 1;
    
    // Sleep period is in seconds
    private static final int SLEEP_PERIOD = 5;
    private static final long SLEEP_PERIOD_MILLIS = SLEEP_PERIOD * 1000L;
    
    /**
     * Creates a new instance of SessionImpl
     */
    public SessionImpl() {
    	// No-arguments constructor
    }

    protected File getJobSessionFile(String jobId)
    {
        return new File(session_jobs_dir, jobId);
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
    public File getLogFromId(String jobId) {
    	return new File(sessionDir, LOG_FILE_PREFIX + jobId + ".log");
    }

    /**
     * <p>Controls Condor jobs.</p>
     * 
     * <p>This method can be used to control jobs submitted outside of the scope
     * of the DRMAA session as long as the job identifier for the job is known.</p>
     *
     * {@inheritDoc}
     *
     * TODO: Complete
     * <p>The DRMAA suspend/resume operations are equivalent to the use of</p>
     *
     * TODO: Complete
     * <p>The DRMAA hold/release operations are equivalent to the use of</p>
     *
     * <p>The DRMAA terminate operation is equivalent to the use of <code>condor_rm</code>.</p>
     * 
     * @param jobId {@inheritDoc}
     * @param action {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void control(String jobId, int action) throws DrmaaException {
    	// Check that we have an active session. You can't do this operation unless
    	// we are in an active session...
        if (! activeSession) {
        	throw new NoActiveSessionException(); 
        }
    	
        // Check if we have a valid job ID
    	if (! Util.validJobId(jobId)) {
    		throw new InvalidJobException("Invalid jobId : " + jobId);
    	}
    	
    	// A collection for all the job IDs that we need to control.
        // Check if we are operating on all job IDs. If we are, then get all
        // the job IDs for this session and perform the control on all of them
    	Set<String> jobIDs = jobId.equals(Session.JOB_IDS_SESSION_ALL) ? getAllSessionJobsIDs() : Collections.singleton(jobId);

    	try {
			// Iterate through the IDs and control them as specified.
			for (String idToControl : jobIDs) {
				controlHelper(action, idToControl);
			}
		} catch (CondorExecException e) {
			e.printStackTrace();
			throw new InternalException("Unable to run condor binary: " + e.getMessage(), e);
		}
    }

	/**
	 * @param action
	 * @param idToControl
	 * @throws CondorExecException
	 * @throws HoldInconsistentStateException
	 * @throws ResumeInconsistentStateException
	 */
	private void controlHelper(int action, String idToControl)
			throws CondorExecException, DrmaaException {
		switch (action) {
			case SessionImpl.TERMINATE:
				// Kill the job. This is equivalent to invoking condor_rm
				CondorExec.terminate(idToControl);
				break;
			case SessionImpl.HOLD:
			case SessionImpl.SUSPEND:
				// Put the job on hold
				// TODO: Check if we can run the suspension.
				boolean ableToHold = true;
				if (ableToHold) {
					CondorExec.suspend(idToControl);
				} else {
					throw new HoldInconsistentStateException();
				}
				break;
			case SessionImpl.RELEASE:
			case SessionImpl.RESUME:
				// Release the job if it's in a hold status
				// TODO: Check if we can run the resume command
				boolean ableToResume = true;
				if (ableToResume) {
					CondorExec.release(idToControl);	
				} else {
					throw new ResumeInconsistentStateException();
				}

				break;
			default:
				throw new IllegalArgumentException("Invalid control action.");
		}
	}
    
    /**
     * The exit() method closes the DRMAA session for all threads and must be
     * called before process termination.  The exit() method may be called only
     * once by a single thread in the process and may only be called after the
     * init() function has completed.  Any call to exit() before init() returns
     * or after exit() has already been called will result in a
     * NoActiveSessionException.
     *
     * <p>The exit() method does necessary clean up of the DRMAA session state</p>
     *
     * <p>Submitted jobs are not affected by the exit() method.</p>
     *
     * @throws DrmaaException {@inheritDoc}
     */
    public void exit() throws DrmaaException {
        //TODO: Reap all jobs with a zero wait synch w/ dispose then condor_rm any that remain.
        //TODO: Synch with waits and such that may be surprised by sessionDir disappearing.
    	synchronized (JOB_IDS_SESSION_ALL) {
			try {
				if (activeSession) {
					if (sessionDir != null && sessionDir.exists()) {
                        if (System.getProperty(CONDOR_JDRMAA_DEBUG) == null) {
                            Util.deleteDir(sessionDir);
                        }
					}
				} else {
					throw new NoActiveSessionException();
				}
			} catch (DrmaaException e) {
				throw e;
			} finally {
				activeSession = false;
			}
		}
    }

    /**
     * getContact() returns an opaque string containing contact information
     * related to the current DRMAA session to be used with the {@link #init(String) init}
     * method.
     *
     * <p>Before the init() method has been called, this method will always
     * return an empty string.
     * 
     * @return {@inheritDoc}
     * @see #init(String)
     */
    public String getContact() {
    	return contact;
    }

    /**
     * The getDRMSystem() method returns a string containing the DRM information.
     *
     * @return {@inheritDoc}
     */
    public String getDrmSystem() {
        return DRM_SYSTEM;
    }
    
    /**
     * The getDrmaaImplementation() method returns a string containing the DRMAA
     * Java language binding implementation version information.  The
     * method returns the same value before and after {@link #init(String) init} is called.
     * 
     * @return {@inheritDoc}
     */
    public String getDrmaaImplementation() {
        /* Because the DRMAA implementation is tightly bound to the DRM, there's
         * no need to distinguish between them. Version information can be
         * retrieved from getVersion() and language information is self-evident. */
        return this.getDrmSystem();
    }
    
    /**
     * Get the job program status.
     *
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @param jobId {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public int getJobProgramStatus(String jobId) throws DrmaaException {
    	// Check that we have an active session. You can't do this operation unless
    	// we are in an active session...
        if (! activeSession) {
        	throw new NoActiveSessionException(); 
        }
        
        // TODO: Implement
    	if (true) {
    		throw new RuntimeException("Not yet implemented.");
    	}

        return 0;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public JobTemplate createJobTemplate() throws DrmaaException {
        // Check that we have an active session. You can't do this operation unless
        // we are in an active session...
        if (! activeSession) {
            throw new NoActiveSessionException();
        }

        final int newJobTemplateId;

        synchronized (this) {
            newJobTemplateId = jobTemplateId++;
        }

        File jtFile = getJobTemplateFile(newJobTemplateId);
        try {
			jtFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalException("Unable to create job template: " + e.getMessage());
		}

        return new JobTemplateImpl(this, newJobTemplateId);
    }
    
    /*
     * TODO: Complete some documentation here.
     */
    private File getJobTemplateFile(int templateId) {
        return new File(sessionDir, "" + templateId);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @param jt {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void deleteJobTemplate(JobTemplate jt) throws DrmaaException {
        if (jt == null) {
            throw new NullPointerException("JobTemplate is null");
        } else if (jt instanceof SimpleJobTemplate) {
        	// Section 7.3 of the Java DRMAA 1.0 spec
			throw new InvalidJobTemplateException();
		} else if (jt instanceof JobTemplateImpl) {
        	jt = null;
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @return {@inheritDoc}
     */
    public Version getVersion() {
        return new Version(0, 2);
    }
    
    /**
     * The init() method initializes the Condor DRMAA API library for
     * all threads of the process and creates a new DRMAA Session. This routine
     * must be called once before any other DRMAA call, except for
     * getDrmSystem(), getContact(), and getDrmaaImplementation().
     *
     * <p><i>contact</i> is an implementation dependent string.  The contact
     * string is composed of a series of name=value pairs separated by semicolons.
     * The supported name=value pairs are:</p>
     *
     * <ul>
     *    <li>
     *      <code>session</code>: the id of the session to which to reconnect
     *    </li>
     * </ul>
     *
     * <p>The <i>contact</i> may be null or empty.
     *
     * <p>Except for the above listed methods, no DRMAA methods may be called
     * before the init() function <b>completes</b>.  Any DRMAA method which is
     * called before the init() method completes will throw a
     * NoActiveSessionException.  Any additional call to init() by any thread
     * will throw a SessionAlreadyActiveException.</p>
     *
     * <p>Once init() has been called, it is the responsibility of the developer
     * to ensure that the exit() will be called before the program
     * terminates.</p>
     *
     * @param contact {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #getContact()
     * @see #exit()
     */
    public void init(String contact) throws DrmaaException {
    	if (contact == null || contact.length() == 0) {
//            throw new InvalidContactStringException();
            contact = System.getProperty("user.name") + CONDOR_JDRMAA_DEFAULT_CONTACT_SUFFIX;
    	}
    	
    	// Check that we actually have Condor installed.
    	boolean condorPresent = Util.isCondorAvailable();
    	if (! condorPresent) {
    		throw new InternalException("No Condor installation found.");
    	}

        //TODO: What is this synch intended to do?
    	synchronized (contact) {
        	if (activeSession) {
        		throw new AlreadyActiveSessionException();
        	}
            this.contact = contact;

            try {
                File tmpFile = File.createTempFile(SESSION_DIR_PREFIX + contact + "-", "");
                sessionDir = new File(tmpFile.getParent(), tmpFile.getName() + ".dir");
                session_jobs_dir = new File(sessionDir, SESSION_JOBS_DIR_NAME);

                if (System.getProperty(CONDOR_JDRMAA_DEBUG) != null) {
                    System.err.println("sessionDir:" + sessionDir.getPath());
                }
            } catch (IOException e) {
                throw new DrmsInitException(e.getMessage());
            }

//            if (sessionDir.exists()) {
//            	boolean deleted = Util.deleteDir(sessionDir);
//            	if (! deleted) {
//            		throw new InternalException("Unable to delete " + sessionDir + " and it is in the way.");
//            	}
//            }

            sessionDir.mkdirs();
            if (System.getProperty(CONDOR_JDRMAA_DEBUG) == null) {
                sessionDir.deleteOnExit();
            }

            session_jobs_dir.mkdir();

            activeSession = true;
		}
    }

    /**
     * The runBulkJobs() method submits an array job very much as
     * if the condor_q options for array jobs had been used
     * with the corresponding attributes defined in the DRMAA JobTemplate,
     * <i>jt</i>.
     *
     * <p>On success a String array containing job identifiers for each array
     * job task is returned.</p>
     *
     * @return {@inheritDoc}
     * @param start {@inheritDoc}
     * @param end {@inheritDoc}
     * @param increment {@inheritDoc}
     * @param jt {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public List<String> runBulkJobs(JobTemplate jt, int start, int end, int increment) throws DrmaaException {
		// See Section 7.3 of the Java DRMAA 1.0 spec
		if (jt instanceof SimpleJobTemplate) {
			throw new InvalidJobTemplateException();
		}
		
    	// Check that we have an active session. You can't do this operation unless
    	// we are in an active session...
        if (! activeSession) {
        	throw new NoActiveSessionException(); 
        }
        
    	if (jt == null) {
            throw new NullPointerException("JobTemplate is null");
        } else if (! (jt instanceof JobTemplateImpl)) {
            throw new InvalidJobTemplateException();
        }

    	// TODO: It appears that Condor doesn't handle increments other than 1. Verify.
        if (increment != 1) {
			throw new IllegalArgumentException(
					"This version of Condor-JDRMAA only supports increments of 1");
		}
        
        // Start and end should be positive
        if ((start < 0) || (end < 0)) {
        	throw new IllegalArgumentException("Only positive integers are allowed for start and end.");
        }

        // The start value must be greater than the end value. Otherwise we are
        // decrementing.
        // TODO: Determine if Condor is able to decrement. Suspect the answer is no.
		if (start > end) {
			throw new InvalidJobTemplateException(
					"This version of Condor-JDRMAA does not support decreasing ranges.");
		}

		try {
			int number = end - start + 1;
			File submitFile = createSubmitFile(jt, number);
			
			// The submit() method returns the first job id regardless of whether the job
			// is a singleton or an array job. Therefore, we should always get a job ID with
			// a trailing ".0" on it. In the runBulkJobs context, we remove this and recalculate
			// the array suffixes sequentially. For this reason, we can't really support
			// increments other than 1, since Condor doesn't seem to have an easy way to do it
			// in the submit file either...
			String jobId = submit(submitFile);
			jobId = jobId.replace(".0", "");
			
			ArrayList<String> jobs = new ArrayList<String>(number);
			for (int jobIndex = 0; jobIndex < end; jobIndex++) {
				String fullJobId = jobId + "." + jobIndex;
				jobs.add(fullJobId);
			}
			
			// Save all the retrieved job IDs in the session file
			saveJobIDsInSessionFile(jobs);
			
			return jobs;
		} catch (Exception e) {
			throw new InvalidJobTemplateException(e.getMessage());
		}
    }

    /**
     * {@inheritDoc}
     * 
     * This runJob() method submits a Condor job with attributes defined in
     * the DRMAA JobTemplate <i>jt</i>. On success, the job identifier is
     * returned.
     * 
     * @param jt {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @return {@inheritDoc}
     */
    public String runJob(JobTemplate jt) throws DrmaaException {
		// Section 7.3 of the Java DRMAA spec
		if (jt instanceof SimpleJobTemplate) {
			throw new InvalidJobTemplateException();
		}
		
    	// Check that we have an active session. You can't do this operation unless
    	// we are in an active session...
        if (! activeSession) {
        	throw new NoActiveSessionException(); 
        }
        
    	String jobId = null;
        if (jt == null) {
            throw new NullPointerException("JobTemplate is null");
        } else if (! (jt instanceof JobTemplateImpl)) {
            throw new InvalidJobTemplateException();
        }
        
        try {
        	File submitFile = createSubmitFile(jt, 1);
			jobId = submit(submitFile);
			
			// Save the retrieved jobId in the session file
			saveJobIDsInSessionFile(Collections.singleton(jobId));
        } catch (Exception e) {
        	throw new InternalException(e.getMessage());
        }
        return jobId;
    }

	/*
	 * Save the job IDs in the session file for the job.
	 * 
	 * @param template a {@link JobTemplate}
	 * @param jobIDs a {@link Set}
	 * @throws IOException
	 * @throws DrmaaException 
	 */
	private void saveJobIDsInSessionFile(Collection<String> jobIDs)
			throws IOException, DrmaaException {

		// Iterate over the IDs and write them to the file
		for (String jobId : jobIDs) {
            File jobSessionFile = getJobSessionFile(jobId);
            BufferedWriter writer = new BufferedWriter(new FileWriter(jobSessionFile));
			writer.write(getLogFromId(jobId).getAbsolutePath());
			writer.newLine();
            writer.newLine();
            writer.close();
		}
	}
    
    /**
     * Create a Condor submit file for the job template.
     * 
     * @param job a {@link JobTemplate}
     * @param number the number of times to execute the job
     * @see "condor_submit man page"
     * @return a {@link File} for the submit file created
     * @throws Exception
     */
    private File createSubmitFile(JobTemplate job, int number) throws Exception {
    	// Can't submit 0 or negative jobs, can we?
    	if (number <= 0) {
    		throw new IllegalArgumentException("Job count must be a positive integer.");
    	}
    	
    	// The submit file will be a temporary file.
    	File tempFile = null;
    	
		try {
	    	tempFile = File.createTempFile(job.getJobName() + "-", SUBMIT_FILE_SUFFIX, sessionDir);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			writer.write("# Condor Submit file");
			writer.newLine();
			writer.write("# Generated by Condor-JDRMAA");
			writer.newLine();
			writer.write("#");
			writer.newLine();
			writer.write("Log=" + sessionDir.getPath() + File.separator + LOG_TEMPLATE);
			writer.newLine();
			writer.write("Universe=local");
			writer.newLine();
			writer.write("Executable=" + job.getRemoteCommand());
			writer.newLine();
			
			// Should jobs be submitted into a holding pattern
			// (don't immediately start running them)?
			if (job.getJobSubmissionState() == JobTemplate.HOLD_STATE) {
				writer.write("Hold=true");
				writer.newLine();
			}
			
			// Here we handle the job arguments, if any have been supplied.
			// We try to adhere to the "new" way of specifying the arguments
			// as explained in the 'condor_submit' man page.
			if (job.getArgs() != null && job.getArgs().size() > 0) {
                StringBuilder sb = new StringBuilder();
				sb.append("\"");
				char tick = '\'';
				Iterator<String> iter = job.getArgs().iterator();
				while (iter.hasNext()) {
					String arg = iter.next();
					if (arg.contains("\"")) {
						arg = arg.replace("\"", "\"\"");
					}
					// Replace ticks with double ticks
					if (arg.contains("\'")) {
						arg = arg.replace("\'", "\'\'");
					}
					if (arg.contains(" ")) {
						sb.append(tick).append(arg).append(tick);
					} else {
						sb.append(arg);
					}
					// Only append a space if there are more...
					if (iter.hasNext()) {
						sb.append(" ");
					}
				}
				// Finish off with the closing quote
				sb.append("\"");
				writer.write("Arguments=" + sb.toString());
				writer.newLine();
			}
			
			// If the working directory has been set, configure it.
			if (job.getWorkingDirectory() != null) {
				writer.write("InitialDir=" + job.getWorkingDirectory());
				writer.newLine();
			}
			
			// Handle any native specifications that have been set
			if (job.getNativeSpecification() != null) {
				writer.write(job.getNativeSpecification());
				writer.newLine();
			}
			
			// Handle the job category.
            //TODO: Could use priority or rank for this.
			if (job.getJobCategory() != null) {
				writer.write("# Category=" + job.getJobCategory());
				writer.newLine();
			}
			
			// Send email notifications?
			if (job.getBlockEmail()) {
				writer.write("Notification=Never");
			}
			
			// If the caller has specified a start time, then we add special
			// Condor settings into the submit file. Otherwise, don't do anything
			// special...
			if (job.getStartTime() != null) {
				long time = (job.getStartTime().getTimeInMillis() + 500) / 1000;
				writer.write("PeriodicRelease=(CurrentTime > " + time + ")");
				writer.newLine();
				writer.write("Hold=True");
				writer.newLine();
			}
			
			// Handle the naming of the job.
			if (job.getJobName() != null) {
				// TODO: The C implementation has a "+" character in front of the
				// directive. We add it here as well. Find out why (or if) this is necessary.
				writer.write("+JobName=" + job.getJobName());
				writer.newLine();
			}
			
			// Handle the job input path. Care must be taken to replace DRMAA tokens
			// with tokens that Condor understands.
			if (job.getInputPath() != null) {
				String input = job.getInputPath();
				input = input.replace(JobTemplate.PARAMETRIC_INDEX, "$(Process)");
				input = input.replace(JobTemplate.HOME_DIRECTORY,  "$ENV(HOME)");
				if (input.startsWith(":")) {
					input = input.substring(1);
				}
				writer.write("Input=" + input);
				writer.newLine();
				// Check whether to transfer the input files
				if (job.getTransferFiles() != null && job.getTransferFiles().getInputStream()) {
					writer.write("transfer_input_files=i");
					writer.newLine();
				}
			}
			
			// Handle the job output path. Care must be taken to replace DRMAA tokens
			// with tokens that Condor understands.
			if (job.getOutputPath() != null) {
				String output = job.getOutputPath();
				output = output.replace(JobTemplate.PARAMETRIC_INDEX, "$(Process)");
				output = output.replace(JobTemplate.HOME_DIRECTORY, "$ENV(HOME)");
				if (output.startsWith(":")) {
					output = output.substring(1);
				}
				writer.write("Output=" + output);
				writer.newLine();
				
				// Check if we need to join input and output files
				if (job.getJoinFiles()) {
					writer.write("# Joining Input and Output");
					writer.newLine();
					writer.write("Error=" + output);
					writer.newLine();
				}
			}
			
			// Handle the error path if specified. Do token replacement if necessary.
			if (job.getErrorPath() != null && ! job.getJoinFiles()) {
				String error = job.getErrorPath();
				error = error.replace(JobTemplate.PARAMETRIC_INDEX, "$(Process)");
				error = error.replace(JobTemplate.HOME_DIRECTORY, "$ENV(HOME)");
				if (error.startsWith(":")) {
					error = error.substring(1);
				}
				writer.write("Error=" + error);
				writer.newLine();
			}
			
			if (job.getTransferFiles() != null && job.getTransferFiles().getOutputStream()) {
				writer.write("should_transfer_files=IF_NEEDED");
				writer.newLine();
				writer.write("when_to_transfer_output=ON_EXIT");
				writer.newLine();
			}
			
			// Handle the case of the user/caller setting the environment for the job.
			if (job.getJobEnvironment() != null && ! job.getJobEnvironment().isEmpty()) {
				Map<String, String> environment = job.getJobEnvironment();
				StringBuilder sb = new StringBuilder();

				// Condor has a peculiar way of handling environment variables in the submit
				// file. Here we try to make it work by escaping quotes properly.
				// TODO: This logic should perhaps be put into a special condorEnvEscape(String)
				// method in Util.
				// TODO: Needs testing
				for (Map.Entry<String, String> entry : environment.entrySet()) {
					// Replace quotes with double quotes.
					String value = entry.getValue().replace("\"", "\"\"").replace("'", "''");
					String pair = entry.getKey() + "='" + value + "' ";
					sb.append(pair);
				}
				// This is the Condor directive for setting the job environment.
				writer.write("Environment=\"" + sb.toString() + "\"");
				writer.newLine();
			}
			
			// It appears that Condor can only handle 1 email address for notifications
			// while the DRMAA returns a set of them. If we have emails specified, then
			// just use the first one...
			if (job.getEmail() != null && job.getEmail().size() > 0) {
				if (job.getEmail().size() > 1) {
					System.err.println(SessionImpl.class.getName() + 
							" warning: Only 1 email notification address is supported.");
				}
				writer.write("Notify_user=" + (String) job.getEmail().iterator().next());
				writer.newLine();
			}
			
			// Every Condor submit file needs a Queue directive to make the job go.
			// Array jobs will have a Queue count greater than 1.
			writer.write("Queue " + number);
			writer.newLine();
			
			// Close the writer
			writer.close();
		} catch (IOException e) {
			throw new Exception("Unable to create the Condor submit file.", e);
		}
		return tempFile;
    }

    /*
     * Invokes 'condor_submit' to submit the job to the Condor scheduler. The submit
     * file is deleted after submission is complete. To leave the submit file in place,
     * for debugging perhaps, define the condor.jdrmaa.debug system property. The difference
     * between this submit() method and the one in CondorExec, is that this one handles the
     * deletion of the submit file if the condor.drmaa.debug system property is not set.
     * The version in CondorExec is dumb and basically just runs the "condor_submit"
     * binary...
     */
    private String submit(File submitFile) throws Exception {
    	// Before we do anything, check that the submit file actually exists and is
    	// readable and contains something. Otherwise, we can't submit anything can we?
    	if (! (submitFile.exists() && submitFile.isFile() && submitFile.canRead() && submitFile.length() > 0)) {
    		throw new IllegalArgumentException("Submit file does not exist or isn't readable.");
    	}
    	
    	String jobID = null;
    	
    	try {
        	jobID = CondorExec.submit(submitFile);
		} catch (CondorExecException cee) {
			throw cee;
		} finally {
			// Delete the submit file. That is, unless we are in debug mode.
			// If we are debugging, the submit file should be left behind for
			// closer inspection/analysis.
			if (System.getProperty(CONDOR_JDRMAA_DEBUG) == null) {
				submitFile.delete();
			}
		}

		return jobID;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @param jobIds {@inheritDoc}
     * @param timeout {@inheritDoc}
     * @param dispose {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void synchronize(List jobIds, long timeout, boolean dispose) throws DrmaaException {
    	// Check that we have an active session. You can't do this operation unless
    	// we are in an active session...
    	if (! activeSession) {
    		throw new NoActiveSessionException();
    	}
    	
    	// Check that we haven't been given a null or an empty set of IDs to synchronize on.
    	if (jobIds == null || jobIds.size() == 0) {
    		throw new IllegalArgumentException("jobIds is null or empty."); 
    	}
    	
    	// A flag to indicate whether to wait for all jobs in the session or not.
    	boolean waitOnAllSessionJobs = false;
    	
        for (Object jobId : jobIds)  {
    		// If any of the job IDs provided is the magical JOB_IDS_SESSION_ALL,
    		// then stop checking because we need to wait for ALL IDs belonging to
    		// this session...
    		if (jobId.equals(Session.JOB_IDS_SESSION_ALL))   {
    			waitOnAllSessionJobs = true;
    			break;
    		} else if (! Util.validJobId((String) jobId)) {
    			throw new InvalidJobException("Job " + jobId + " is invalid.");
    		}
    	}
    	
    	// So no bad IDs detected. Just need to check whether we need
    	// to scan for all the IDs belonging to the session, or to use
    	// the IDs provided by the caller.
    	Set<String> toWaitFor;
        if (waitOnAllSessionJobs) {
            // We've been told to wait for all the session jobs. Therefore
            // our set of job IDs needs to contain all the job IDs belonging
            // to the session. That means we need to scan the session directory
            // for all the job IDs belonging to it. For this, we make use of a
            // private method...
            toWaitFor = getAllSessionJobsIDs();
        } else {
            // Okay, in this case, we only need to wait for the job IDs that have
            // been explicitly passed to us. None of the IDs was equal to the
            // special value indicating that we should wait for all... Therefore
            // we create a new set, and all the IDs that we've been told about.
            toWaitFor = new HashSet<String>(jobIds);
        }

    	// Let's determine when we started waiting and how long we are able to wait
    	// by computing the deadline.
        // But if we're not using a positive timeout then don't change the value.
    	long now = (timeout > 0) ? Util.getSecondsFromEpoch() : 0;
    	long deadline = timeout + now;

        // We now know what job IDs to wait for. Cycle through each and wait for it.
    	// Wait for each job ID. Mind the timeout! If we exceed our
    	// deadline, break out of the loop...
    	for (String jobId : toWaitFor) {
    		// As we consume time, the timeouts get shorter and shorter...
    		long individualTimeout = deadline - now;

            // If we using a positive timeout but we've gone past the deadline
            // then use a no wait timeout (0).
            if (timeout > 0 && individualTimeout < 0) {
                individualTimeout = TIMEOUT_NO_WAIT;
            }

    		wait(jobId, individualTimeout, dispose);
    		

            if (timeout > 0) now = Util.getSecondsFromEpoch();
    	}
    }
    
    /**
     * This method scans the session directory for job files. For each found
     * file, the file represents a job belonging to that session. The file is opened
     * and the job ID is read from it.
     * 
     * @return a {@link Set} of String job IDs
     * @throws IOException
     */
    private Set<String> getAllSessionJobsIDs() throws DrmaaException {
        try {
            Set<String> idSet;

            File[] idFiles = session_jobs_dir.listFiles();
            if (idFiles == null) {
                idSet = Collections.emptySet();
            } else {
                idSet = new HashSet<String>(idFiles.length);

                // Iterate through the files
                for (File file : idFiles) {
                    // Make sure we have a readable file.
                    if (! (file.isFile() && file.canRead() /*&& file.length() > 0*/)) {
                        continue;
                    }

//			try {
//				BufferedReader reader = new BufferedReader(new FileReader(file));
//				String jobId = null;
//				while ((jobId = reader.readLine()) != null) {
//					jobId = jobId.trim();
//					//  Do one more check to make sure we have a good looking ID.
//					if (Util.validJobId(jobId)) {
//						idSet.add(jobId);
//					}
//				}
//			} catch (IOException ioe) {
//				throw ioe;
//			}

                    idSet.add(file.getName());
                }
            }

            return idSet;
        } catch (SecurityException sex) {
            throw new InternalException("Can't list files in session jobs dir.", sex);
        }
	}

	/**
     * {@inheritDoc}
     * 
     * @param jobId {@inheritDoc}
     * @param timeout {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public JobInfo wait(String jobId, long timeout) throws DrmaaException {
        return wait(jobId, timeout, true);
    }

    protected JobInfo wait(String jobId, long timeout, boolean dispose) throws DrmaaException {
    	// Make sure we have a good timeout, but check that it's not one of the
    	// predefined values.
    	if (timeout < 0 && timeout != Session.TIMEOUT_WAIT_FOREVER) {
    		throw new IllegalArgumentException("Must have a positive timeout.");
    	}

/*
        // Check if we have a bad timeout value
        if (timeout < 0 && timeout != SessionImpl.TIMEOUT_WAIT_FOREVER) {
            throw new IllegalArgumentException("Illegal timeout value.");
        }
*/

        long start = 0;

        pick_a_jobId :
        while (jobId.equals(SessionImpl.JOB_IDS_SESSION_ANY)) {
            //TODO: This should use common code with getAllSessionJobsIDs().
        	File[] files = session_jobs_dir.listFiles();
        	if (files == null || files.length < 1) {
                // There are no job session files (yet).
                // Not sure whether we should quit right away, but since we've
                // got this time out loop we'll give them a second chance.
                if (start != 0) {
                    // We've looked twice for template files and find none.
                    throw new InvalidJobException("JOB_IDS_SESSION_ANY fails because no jobs for this session.");
                }
            } else {
        		// TODO: Pick a file at random instead of just the first
        		for (File toWaitFor : files) {
//                    try {
                        if (toWaitFor.length() > 0 && toWaitFor.canRead() && toWaitFor.isFile()) {
//                            BufferedReader buf = new BufferedReader(new FileReader(toWaitFor));
//                            //TODO: Is the writing locked?  How can we be sure we get it all?
//                            // Should we be looking for the EOL?
//                            String line = buf.readLine();
//                            if (line != null) {
//                                line = line.trim();
//                                if (line.length() > 0) {
//                                    jobId = line;
//                                    break pick_a_jobId;
//                                }
//                            }
                            jobId = toWaitFor.getName();
                            break  pick_a_jobId;
                        }
//                    } catch (IOException e) {
////                        throw new InternalException("Unable to read file " + toWaitFor.getAbsolutePath());
//                    }
                }
        	}

            // Didn't get a jobId from any of those files (if there were any).
            // Wait a while if they are willing to before trying again.
            if (timeout > 0 || timeout == Session.TIMEOUT_WAIT_FOREVER) {
                if (start == 0) {
                    // We've just checked once.
                    // Set the start time now so that TIMEOUT_WAIT_FOREVER can
                    // look at it and can quit if there are no job files on the second try.
                    start = System.currentTimeMillis();
                } else {
                    if (timeout > 0) {
                        if (System.currentTimeMillis() - start >= timeout * 1000) {
                            throw new ExitTimeoutException("Timed out trying to read a jobId from sessionDir files.");
                        }
                    }
                }

                try {
                    Thread.sleep(SLEEP_PERIOD_MILLIS);
                } catch (InterruptedException e) {
                    throw new InternalException(e.getMessage());
                }
            }
        }

        // Check if we have a valid job ID
        if (! (Util.validJobId(jobId))) {
            throw new InvalidJobException("Job " + jobId + " is invalid.");
        }

        JobLogParser logParser = new JobLogParser(this, jobId);
        JobInfo info = monitor(logParser, timeout);

        if (info == null) {
            throw new ExitTimeoutException("Timed out waiting for job " + jobId);
        } else if (dispose) {
            File job_session_file = getJobSessionFile(info.getJobId());
            job_session_file.delete();
        }

        return info;
    }

    /*
     * TODO: Complete documentation
     */
	private JobInfo monitor(JobLogParser logParser, long timeout) throws DrmaaException {
    	// Get the current number of seconds since the epoch. We'll refer
    	// to this to make sure we stop waiting if we reach the timeout...
    	final long start = System.currentTimeMillis();

        JobInfo info = null;

		// Start monitoring
		while (true) {
		    try {
                info = logParser.parse();
            } catch (IOException e) {
                throw new InternalException(e.getMessage(), e);
            }
		    if (info.hasExited() || timeout == Session.TIMEOUT_NO_WAIT) {
		    	break;
		    }

            if (timeout != Session.TIMEOUT_WAIT_FOREVER) {
		        if (System.currentTimeMillis() - start >= timeout * 1000) {
		        	// We've reached the timeout
		        	break;
                }
		    }

            try {
                // SLEEP_PERIOD is in seconds
                Thread.sleep(SLEEP_PERIOD_MILLIS);
            } catch (InterruptedException e) {
                break;
            }
		}

		return info;
	}
}
