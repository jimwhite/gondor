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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link JobTemplate#setJobEnvironment(Map) setJobEnvironment} method.
 */
public class EnvironmentTest {

	private static final String contactName = EnvironmentTest.class.getSimpleName();
	private static File outputFile = null;
	private static String outputPath;
	
	/**
	 * Performs initial setup and validation before the tests are executed.
	 * Checks whether the "test.data.dir" system property has been properly set.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setup() throws Exception {
		// Let's configure the output path for where the STDOUT of the job should be saved to.
		outputPath = System.getProperty("user.home") + File.separator + contactName + ".out";
		
		outputFile = new File(outputPath);
		
		deleteOutputFile();
	}
	
	/**
	 * Test simple job submission with a wait for completion.
	 */
	@Test
	public void environmentTest() {
		String envVariableName = contactName.toUpperCase();
		String envVariableValue = "abcde";
		Map<String, String> env = new HashMap<String, String>();
		env.put(envVariableName, envVariableValue);
		
		try {
			Session session = SessionFactory.getFactory().getSession();
			session.init(contactName);
			JobTemplate jt = session.createJobTemplate();
			assertNotNull(jt);
			
			jt.setJobEnvironment(env);
			
			jt.setRemoteCommand("/usr/bin/env");
			jt.setOutputPath(outputPath);
			
			String jobId = session.runJob(jt);
			
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Delete the template
			session.deleteJobTemplate(jt);

			// Wait for the job to complete
			JobInfo jobInfo = session.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

			assertNotNull(jobInfo);

			// Sleep a little for I/O to catch up
			// TODO: This setting is arbitrary. Need to make configurable (System property?)
			Thread.sleep(3000);
			
			// Verify that the output file is actually there.
			assertTrue(outputFile.exists());
			
			// Read through the output file and look for the environment value in it.
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = null;
			boolean containsCorrectOutput = false;
			while ((line = reader.readLine()) != null) {
				if (line.contains(envVariableName) && line.contains(envVariableValue)) {
					containsCorrectOutput = true;
					break;
				}
			}
			// Close the reader
			reader.close();
			
			assertTrue(containsCorrectOutput);
			
			// Exit the session
			session.exit();
		} catch (Exception de) {
			de.printStackTrace();
			fail(de.getMessage());
		}
	}
	
	/**
	 * Runs after the test is complete and removes the output file.
	 */
	@AfterClass
	public static void cleanup() {
		deleteOutputFile();
	}
	
	private static void deleteOutputFile() {
		// Delete the file if it's there (could be a leftover from previous tests)
		if (outputFile != null && outputFile.exists()) {
			boolean deleted = false;
			
			do {
				deleted = outputFile.delete();
				if (! deleted) {
					System.err.println("Unable to delete " + outputFile.getAbsolutePath());
				}
			} while (! deleted);
		}
	}
}


