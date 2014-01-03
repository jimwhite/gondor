package net.sf.igs.test;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.igs.CondorExecException;
import net.sf.igs.SessionImpl;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.NoActiveSessionException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests whether support for the {@link Session#control(String, int) control()}
 * method works properly. The tests contained in this class work by submitting
 * sleep jobs and then manipulating them by suspending them, terminating them,
 * holding and then releasing them, etc. At the conclusion of the tests, an
 * effort is made to cleanup after ourselves by removing any leftover test jobs
 * from the grid.
 * 
 * @see "The DRMAA 1.0 Java specification: ggf-drmaa-java-binding.1_0.pdf"
 */
public class ControlTest {
	private static String name = ControlTest.class.getSimpleName();
	private static Set<String> jobIdsToCleanup;
	
	/**
	 * Initialize the test class. During the initialization, a data structure
	 * is created to hold job IDs created by the testing. When the testing is
	 * concluded, each of the job IDs is reaped by the cleanup process.
	 */
	@BeforeClass
	public static void setup() {
		jobIdsToCleanup = new HashSet<String>();
	}
	
	/**
	 * Tests whether a {@link NoActiveSessionException} exception is thrown
	 * when we attempt to control a job outside of a session.
	 * 
	 * @throws DrmaaException
	 */
	@Test(expected=NoActiveSessionException.class)
	public void testControlWithoutSession() throws DrmaaException {
		Session session = SessionFactory.getFactory().getSession();
		JobTemplate sleepTemplate = TestUtils.getSleepJobTemplate(session, name);
		
		// Run the job and get the ID.
		String jobId = session.runJob(sleepTemplate);
		session.deleteJobTemplate(sleepTemplate);
		session.exit();
		
		// Now that we have exited the session, try to use the session
		// to control the job we have previously submitted. This should
		// throw an exception.
		session.control(jobId, Session.TERMINATE);
		
		// If we get here, then the test failed, which means a job actually
		// went through. Mark it for removal during the cleanup process.
		jobIdsToCleanup.add(jobId);
		fail("No exception thrown");
	}
	
	
	/**
	 * Test the {@link Session#control(String, int)} method for terminating a job.
	 */
	@Test
	public void testJobTerminate() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);
			
			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Sleep a little...
			Thread.sleep(2000);
			
			// Make sure the job is on the grid.
			boolean present = TestUtils.isJobPresent(jobId);
			assertTrue(present);
			
			// Try suspending the job
			session.control(jobId, Session.TERMINATE);
			
			// Sleep a little more...
			Thread.sleep(2000);
			
			// Determine if the job is present on the grid. We use
			// a private method to help us with this.
			present = TestUtils.isJobPresent(jobId);

			// The job should not be around (we killed it).
			assertFalse(present);
			
			if (present) {
				// Save the job ID for later removal. Can't have an accumulation
				// of held jobs just because we're testing can we?
				jobIdsToCleanup.add(jobId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the session
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test the {@link Session#control(String, int)} method for suspending a job.
	 */
	@Test
	public void testJobSuspend() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);
			
			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Sleep a little...
			Thread.sleep(3000);
			
			// Make sure the job is on the grid.
			boolean present = TestUtils.isJobPresent(jobId);
			assertTrue(present);
			
			// Try suspending the job
			session.control(jobId, Session.SUSPEND);
			
			boolean held = TestUtils.isJobHeld(jobId);
			if (held) {
				// Save the job ID for later removal. Can't have an accumulation
				// of held jobs just because we're testing can we?
				jobIdsToCleanup.add(jobId);
			}
			
			assertTrue(held);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the session
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test the {@link Session#control(String, int)} method for suspending a job.
	 */
	@Test
	public void testJobHold() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);

			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Sleep a little...
			Thread.sleep(3000);
			
			// Make sure the job is on the grid.
			boolean present = TestUtils.isJobPresent(jobId);
			assertTrue(present);
			
			// Try putting the job on hold
			session.control(jobId, Session.HOLD);
				
			boolean held = TestUtils.isJobHeld(jobId);
			if (held) {
				// Save the job ID for later removal. Can't have an accumulation
				// of held jobs just because we're testing can we?
				jobIdsToCleanup.add(jobId);
			}
			
			assertTrue(held);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the session
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test the {@link Session#control(String, int)} method for resuming a held job.
	 */
	@Test
	public void testJobResume() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			// Create a job that is suspended/held. This will let us test
			// the release/resume functionality.
			String jobId = TestUtils.createHeldJob(session, name);
			assertNotNull(jobId);
			
			// Add this job to the jobs to cleanup after tests are completed
			jobIdsToCleanup.add(jobId);
			
			// Release the job
			session.control(jobId, Session.RESUME);
			
			// Sleep a little, then make sure the job is not held anymore
			Thread.sleep(2000);
			
			boolean held = TestUtils.isJobHeld(jobId);
			assertFalse(held);
			
			// Might take a little while for the job to return to a running state.
			boolean running = false;
			int maxAttempts = 10;
			for (int count = 1; count <= maxAttempts; count++) {
				// Verify that the job is now running
				running = TestUtils.isJobRunning(jobId);
				if (running) {
					break;
				} else {
					// Wait a little bit
					Thread.sleep(3000);
				}
			}
			
			assertTrue(running);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the session
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test the {@link Session#control(String, int)} method for releasing a held job.
	 */
	@Test
	public void testJobRelease() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			// Create a job that is suspended/held. This will let us test
			// the release/resume functionality.
			String jobId = TestUtils.createHeldJob(session, name);
			assertNotNull(jobId);
			
			// Add this job to the jobs to cleanup after tests are completed
			jobIdsToCleanup.add(jobId);
			
			// Release the job
			session.control(jobId, Session.RELEASE);
			
			// Sleep a little, then make sure the job is not held anymore
			Thread.sleep(2000);
			boolean held = TestUtils.isJobHeld(jobId);
			assertFalse(held);
			
			// Might take a little while for the job to return to a running state.
			boolean running = false;
			for (int count = 1; count <= 10; count++) {
				// Verify that the job is now running
				running = TestUtils.isJobRunning(jobId);
				if (running) {
					break;
				} else {
					// Wait a little bit
					Thread.sleep(2000);
				}
			}
			
			assertTrue(running);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the session
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}

	/**
	 * The DMRAA 1.0 specification specifies that jobs submitted from other
	 * session CAN be controllable with this method. This Condor-JDRMAA
	 * implementation allows this. This method tests the feature by creating a
	 * session, submitting a job through it, deleting the template and closing
	 * the session. The test then starts a brand new session, with a different
	 * contact name, and controls the previously submitted job. It should work.
	 * 
	 * @see "ggf-drmaa-java-binding.1_0.pdf Section 5.1.27"
	 */
	@Test
	public void testHoldOutOfSession() {
		Session session = null;
		Session session2 = null;
		
		try {
			session = new SessionImpl();
			session.init(name);
			
			// Get a job template for a job that just sleeps
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);
			
			// Execute the job and retrieve the job ID
			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Sleep a little...
			Thread.sleep(2000);
			
			// Make sure the job is on the grid.
			boolean present = TestUtils.isJobPresent(jobId);
			assertTrue(present);
			
			// Now start a new session, with a different name.
			session2 = new SessionImpl();
			
			session2.init(name + "2");
			session2.control(jobId, Session.HOLD);
			
			// Sleep a little...
			Thread.sleep(2000);
			
			// Now check if we were able to hold a job that from a
			// different session.
			boolean held = TestUtils.isJobHeld(jobId);
			assertTrue(held);
			
			// We have another job that we need to reap during cleanup
			// when the tests conclude...
			if (held) {
				jobIdsToCleanup.add(jobId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the sessions
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
			
			try {
				if (session2 != null) {
					session2.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * See if we can control all the session jobs, even when the session
	 * has array jobs in it.
	 */
	@Test
	public void testControlAllSessionJobsWithArrayJobs() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {
			session.init(name);
			
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);
			
			int jobQuantity = 100;
			List<String> jobIds = session.runBulkJobs(jt, 1, jobQuantity, 1);
			
			session.deleteJobTemplate(jt);
			
			for (String jobId : jobIds) {
				// Add this job ID to the IDs to cleanup after tests are completed
				jobIdsToCleanup.add(jobId);
			}
			
			// Okay, we now have a quantity of jobs belonging to the session.
			// Let us see if we can control them all.
			session.control(Session.JOB_IDS_SESSION_ALL, Session.HOLD);
			
			// Verify that all these jobs are held
			boolean allHeld = true;
			int maxAttempts = 10;
			for (int attempt = 1; attempt <= maxAttempts; attempt++) {
				for (String jobId : jobIds) {
					boolean held = TestUtils.isJobHeld(jobId);
					if (! held) {
						allHeld = false;
						break;
					}
				}
				
				if (allHeld) {
					// Okay, everything is held, so break out of the retry loop
					break;
				} else {
					// Wait a little bit and try again
					Thread.sleep(2000);
				}
			}
			
			// Are all the jobs held?
			assertTrue(allHeld);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the sessions
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void testControlAllSessionJobs() {
		Session session = SessionFactory.getFactory().getSession();
		
		try {			
			session.init(name);
			
			// Create a bunch of jobs in the held state. This will let us test
			// the control functionality when we attempt to control all jobs
			// from the session.
			int jobQuantity = 5;
			String[] jobs = new String[jobQuantity];
			
			for (int jobIndex = 1; jobIndex <= jobQuantity; jobIndex++) {
				String jobId = TestUtils.createJob(session, name);
				jobs[jobIndex - 1] = jobId;
				
				// Add this job to the jobs to cleanup after tests are completed
				jobIdsToCleanup.add(jobId);
			}
			
			// Okay, we now have a quantity of jobs belonging to the session.
			// Let us see if we can control them all.
			session.control(Session.JOB_IDS_SESSION_ALL, Session.HOLD);
			
			// Sleep a little, then make sure the jobs are not held anymore
			Thread.sleep(2000);
			
			// Verify that all these jobs are held
			boolean allHeld = true;
			for (int jobIndex = 1; jobIndex <= jobQuantity; jobIndex++) {
				boolean held = TestUtils.isJobHeld(jobs[jobIndex - 1]);
				if (! held) {
					allHeld = false;
					break;
				}
			}
			
			assertTrue(allHeld);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				// Exit the sessions
				if (session != null) {
					session.exit();
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Runs after the testing is complete and removes the input and output files.
	 */
	@AfterClass
	public static void cleanup() {
		Iterator<String> iter = jobIdsToCleanup.iterator();
		while (iter.hasNext()) {
			String jobId = (String) iter.next();
			TestUtils.removeJob(jobId);
		}
	}
}
