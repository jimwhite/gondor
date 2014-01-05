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

import java.util.*;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.SessionFactory;
import org.junit.Test;

/**
 * Test simple DRMAA job submission functionality. Submit a singleton job and get the id back.
 */
public class SimpleTest {

	private static final String contactName = SimpleTest.class.getSimpleName();
	
	/**
	 * Test simple job submission with no wait for completion.
	 */
	@Test
	public void simpleSubmitNoWaitTest() {
		try {
			Session session = SessionFactory.getFactory().getSession();

			session.init(contactName);
			JobTemplate jt = session.createJobTemplate();

			// Note: The test will fail if the user's home directory does not
			// exist on the execute node.
			jt.setWorkingDirectory(System.getProperty("user.home"));
			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(SimpleTest.class.getSimpleName());
			String jobId = session.runJob(jt);
			
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Delete the template
			session.deleteJobTemplate(jt);

			// Exit the session
			session.exit();
		} catch (Exception de) {
			de.printStackTrace();
			fail(de.getMessage());
		}
	}
	
	/**
	 * Test simple job submission with a wait for completion.
	 */
	@Test
	public void simpleSubmitAndWaitForeverTest() {
		try {
			Session session = SessionFactory.getFactory().getSession();

			session.init(contactName);
			JobTemplate jt = session.createJobTemplate();
			
			// Note: The test will fail if the user's home directory does not
			// exist on the execute node.
			jt.setWorkingDirectory(System.getProperty("user.home"));
			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(SimpleTest.class.getSimpleName());
			String jobId = session.runJob(jt);
			
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Delete the template
			session.deleteJobTemplate(jt);

			// Wait for the job to complete
			JobInfo jobInfo = session.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

			assertNotNull(jobInfo);
			
			// Exit the session
			session.exit();
		} catch (Exception de) {
			de.printStackTrace();
			fail(de.getMessage());
		}
	}
	
	/**
	 * Test simple job submission with a wait for completion with timeout.
	 */
	@Test
	public void simpleSubmitAndWaitWithTimeoutTest() {
		try {
			Session session = SessionFactory.getFactory().getSession();

			session.init(contactName);
			JobTemplate jt = session.createJobTemplate();
			
			// Note: The test will fail if the user's home directory does not
			// exist on the execute node.
			jt.setWorkingDirectory(System.getProperty("user.home"));
			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(SimpleTest.class.getSimpleName());
			String jobId = session.runJob(jt);
			
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Delete the template
			session.deleteJobTemplate(jt);

			// Wait for the job to complete for (5 minutes at most)...
			JobInfo jobInfo = session.wait(jobId, 300);

			assertNotNull(jobInfo);
			
			// Exit the session
			session.exit();
		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		}
	}
	
	/**
	 * Test simple job submission with a wait for completion.
	 */
	@Test
	public void simpleSubmitAndSynchronizeTest() {
		try {
			Session session = SessionFactory.getFactory().getSession();

			session.init(contactName);
			JobTemplate jt = session.createJobTemplate();
			
			// Note: The test will fail if the user's home directory does not
			// exist on the execute node.
			jt.setWorkingDirectory(System.getProperty("user.home"));
			jt.setRemoteCommand("/bin/sleep");
			jt.setArgs(Collections.singletonList("10"));
			jt.setJobName(SimpleTest.class.getSimpleName());
			String jobId = session.runJob(jt);
			
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Delete the template
			session.deleteJobTemplate(jt);

			// Wait for the job to complete
			session.synchronize(Collections.singletonList(jobId), Session.TIMEOUT_WAIT_FOREVER, true);
			
			// Exit the session
			session.exit();
		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		}
	}
}


