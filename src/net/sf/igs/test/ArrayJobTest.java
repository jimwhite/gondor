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

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.SessionFactory;
import org.junit.Test;

/**
 * Test DRMAA array job submission.
 */
public class ArrayJobTest {

	private static String contact = ArrayJobTest.class.getSimpleName();
	
	/**
	 * Test array job submission without waiting for completion.
	 */
	@Test
	public void arrayJobTest() {
		Session session = null;
		try {
			String name = ArrayJobTest.class.getSimpleName();
			session = SessionFactory.getFactory().getSession();

			session.init(contact);
			JobTemplate jt = session.createJobTemplate();

			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(name);
			
			// Now, make this an array job
			int jobCount = 10;
			int increment = 1;
			List<String> jobIds = session.runBulkJobs(jt, 1, jobCount, increment);
			
			// Release resources for the job template
			session.deleteJobTemplate(jt);
			
			assertNotNull(jobIds);
			assertTrue(jobIds.size() > 0);
			assertEquals(jobIds.size(), jobCount);

			for (String id : jobIds) {
				assertNotNull(id);
				assertTrue(id.length() > 0);
			}
		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}

	/**
	 * Test array job submission and wait for completion.
	 */
	@Test
	public void arrayJobWaitTest() {
		Session session = null;
		try {
			String name = ArrayJobTest.class.getSimpleName();
			session = SessionFactory.getFactory().getSession();
	
			session.init(contact);
			JobTemplate jt = session.createJobTemplate();
	
			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(name);
			String filePathBase = System.getProperty("user.home") + File.separator + 
				ArrayJobTest.class.getSimpleName() + ".out";
			jt.setOutputPath(":" + filePathBase + ".$(Process)");

			
			// Now, make this an array job;
			int jobCount = 10;
			int increment = 1;
			// Make sure we don't have any output files around from previous runs
			cleanupFiles(filePathBase, jobCount);
			
			// Run the jobs
			List<String> jobIds = session.runBulkJobs(jt, 1, jobCount, increment);
			
			assertNotNull(jobIds);
			assertTrue(jobIds.size() > 0);
			assertEquals(jobIds.size(), 10);
			
			// Now wait on those ids.
			session.synchronize(jobIds, Session.TIMEOUT_WAIT_FOREVER, true);
			
			// Okay, all the jobs completed. Let's double check that.
			boolean filesPresent = false;
			for (int jobIndex = 0; jobIndex < jobCount; jobIndex++) {
				String jobFilePath = filePathBase + "." + jobIndex;
				File jobFile = new File(jobFilePath);
				if (! jobFile.exists()) {
					filesPresent = false;
					break;
				}
			}
			
			// Did we get all the files that we expected?
			assertTrue(filesPresent);
			
			cleanupFiles(filePathBase, jobCount);
		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (DrmaaException e) {
				// ignored
			}
		}
	}

	private void cleanupFiles(String filePathBase, int jobCount) {
		// Delete the files
		for (int jobIndex = 0; jobIndex < jobCount; jobIndex++) {
			String jobFilePath = filePathBase + "." + jobIndex;
			File jobFile = new File(jobFilePath);
			if (jobFile.exists()) {
				jobFile.delete();
			}
		}
	}
}
