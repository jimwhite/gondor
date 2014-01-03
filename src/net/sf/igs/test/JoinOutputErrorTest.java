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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests whether support for the {@link JobTemplate#setJoinFiles(boolean) setJoinFiles}
 * method works properly.
 */
public class JoinOutputErrorTest {
	private static String name = JoinOutputErrorTest.class.getSimpleName();
	private static File combinedFile, errorFile = null;
	private static String combinedPath, errorPath;
	private static String script = null;
	
	/**
	 * Establish that files/scripts are not around from previous invocations. Copy
	 * a special script from the distribution's test directory into position so that
	 * it can be invoked as a remote command.
	 * 
	 * @throws IOException 
	 */
	@BeforeClass
	public static void setup() throws IOException {
		String base = System.getProperty("user.home") + File.separator + name;
		// Let's configure the error path for where the STDERR of the job should be saved to.
		combinedPath = base + ".combined";
		errorPath = base + ".err";
		
		combinedFile = new File(combinedPath);
		errorFile = new File(errorPath);
		
		String scriptBasename = "out_and_err.pl";
		script = System.getProperty("user.home") + File.separator + scriptBasename;
		
		// Ensure we don't have files around from previous tests
		deleteFiles();
		
		// Position the special script we have into place (into the user's home
		// directory).
		copyScript(scriptBasename);
	}

	// Need a way to copy files without polluting the test code. Here's a private method.
	private static void copyFile(String source, String destination) throws IOException {
		File inFile = new File(source);
		File outFile = new File(destination);
		FileReader in = new FileReader(inFile);
		FileWriter out = new FileWriter(outFile);
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		in.close();
		out.close();
	}
	
	/**
	 * Copy the script that generates both STDOUT and STDERR from the test
	 * directory (configured with the "test.data.dir" system property) to the
	 * user's home directory and grant execute permissions to it so that it
	 * can be invoked as the remote command for the test.
	 * 
	 * @param basename a <code>String</code> with the basename of the script
	 * @throws IOException
	 */
	private static void copyScript(String basename) throws IOException {
		String testDataDir = System.getProperty("test.data.dir");
		if (testDataDir == null) {
			throw new RuntimeException("The test.data.dir system property isn't set.");
		}
		
		File distScript = new File(testDataDir, basename);
		if (distScript.exists()) {
			try {
				// Copy the file to the user's home directory
				copyFile(distScript.getAbsolutePath(), script);
				
				// Well, this only works on Linux/Unix...
				Process process =  Runtime.getRuntime().exec("chmod 755 " + script);
				process.waitFor();
				
				// Make sure we got a good exit value.
				if (process.exitValue() != 0) {
					throw new RuntimeException("Unable to grant execute permissions to " + script);
				}
			} catch (InterruptedException e) {
				fail("Test interrupted.");
			}
		} else {
			throw new RuntimeException("Unable to find " + basename);
		}
	}
	
	// Delete files that are products of running the test. Let's be nice and not
	// leave the directory polluted with stuff.
	private static void deleteFiles() {
		// Delete the file if it's there (could be a leftover from previous tests)
		if (combinedFile.exists()) {
			combinedFile.delete();	
		}
		if (errorFile.exists()) {
			errorFile.delete();
		}
		File scriptFile = new File(script);
		if (scriptFile != null && scriptFile.exists()) {
			scriptFile.delete();
		}
	}
	
	/**
	 * Ensure that the {@link JobTemplate#setJoinFiles(boolean) setJoinFiles}
	 * implementation works properly.
	 */
	@Test
	public void testStreamMerging() {
		// Formulate the list of arguments for the script that we have copied
		// from the test directory to the user's home directory. This script
		// will be the remote command to run. It is specially crafter to take
		// the first argument and send it to STDOUT. The second argument is
		// sent to STDERR. Therefore, this script generates both and is specially
		// suited to test the merging of the streams.
		String stdoutArgument = "theGood";
		String stderrArgument = "theBad";
		List<String> args = new ArrayList<String>();
		args.add(stdoutArgument);
		args.add(stderrArgument);
		
		try {
			Session session = SessionFactory.getFactory().getSession();
			session.init(name);
			JobTemplate jt = session.createJobTemplate();
			
			// Set the remote command to be the script that we have copied from
			// the distribution's test directory to the user's home directory.
			jt.setRemoteCommand(script);
			jt.setArgs(args);
			
			// Set the job name
			jt.setJobName(name);

			// Test the behavior if we specify the error path, the output path, and yet
			// ALSO specify that STDERR and STDOUT are to be joined. Someone has to lose...
			jt.setErrorPath(":" + errorPath);
			jt.setOutputPath(":" + combinedPath);
			
			// The crucial setting
			jt.setJoinFiles(true);
			
			// Make sure we don't have files around from previous test invocations
			assertFalse(combinedFile.exists());
			assertFalse(errorFile.exists());
			
			// Make sure we do have the script to run
			assertTrue(new File(script).exists());
			
			// Start the job
			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);
			
			// Wait for the job to complete (wait indefinitely)
			session.wait(jobId, Session.TIMEOUT_WAIT_FOREVER);

			// Free job template resources
			session.deleteJobTemplate(jt);
			
			// Exit the session
			session.exit();
			
			// Sleep a little to allow I/O to happen
			Thread.sleep(3000);
			
			// Now check that the STDERR file is NOT there and that
			// the combined file is... What we're saying here, is that when
			// streams are to be merged, the combined file should be the one
			// that was specified as the STDOUT file.
			assertFalse(errorFile.exists());
			assertTrue(combinedFile.exists());

			// More detailed checking. Actually see if the error has the right information in it.
			boolean correctOutput = false;
			boolean correctError = false;
			
			// Read the combined file and check that both the STDERR and the STDOUT
			// are in there.
			BufferedReader reader = new BufferedReader(new FileReader(combinedFile));
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				if (line.contains(stdoutArgument)) {
					correctOutput = true;
				} else if (line.contains(stderrArgument)) {
					correctError = true;
				}
			}
			reader.close();
			
			// Both STDOUT and STDERR should be there (streams merged)
			assertTrue(correctOutput && correctError);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Perform cleanup operations when the test is concluded. Remove files
	 * generated by the test.
	 */
	@AfterClass
	public static void cleanup() {
		deleteFiles();
	}
}
