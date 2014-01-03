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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests whether support for the {@link JobTemplate#setInputPath(String)
 * setInputPath} method really works. The test operates by generating an input
 * file with some text in it (the name of the test class) and then invoking the
 * "cat" command with the file as the input. The output file should then have
 * the same text as the input file.
 * 
 * @see "cat command man page"
 */
public class InputPathTest {
	private static String name = InputPathTest.class.getSimpleName();
	private static File inputFile, outputFile = null;
	private static String inputPath, outputPath = null;
	
	/**
	 * Tests whether support for the {@link JobTemplate#setOutputPath(String) setOutputPath}
	 * works properly.
	 * @throws IOException 
	 */
	@BeforeClass
	public static void setup() throws IOException {
		// Let's configure the output path for where the STDOUT of the job should be saved to.
		inputPath = System.getProperty("user.home") + File.separator + name + ".txt";
		outputPath = System.getProperty("user.home") + File.separator + name + ".out";
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		
		// Delete any file that may still be around from previous tests. The content might
		// not be what we want in there...
		deleteInputFile();
		
		// Create the input file with what we know will have the right content for the test
		createInputFile();
	}

	// If the input file exists, delete it.
	private static void deleteInputFile() {
		// Delete the file if it's there (could be a leftover from previous tests)
		if (inputFile != null && inputFile.exists()) {
			boolean deleted = false;
			do {
				deleted = inputFile.delete();
				if (! deleted) {
					System.err.println("Unable to delete file " + inputFile.getAbsolutePath());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						System.err.println("Test interrupted.");
						break;
					}
				}
			} while (! deleted);
		}
	}
	
	// Create the file that will act as the input file for this test and
	// write some content into the file. Then close the file in preparation
	// for the test execution.
	private static void createInputFile() throws IOException {
		File input = new File(inputPath);
		input.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(input));
		writer.write(name);
		writer.newLine();
		writer.close();
	}
	
	/**
	 * Test the {@link JobTemplate#setInputPath(String) setInputPath} method.
	 */
	@Test
	public void testInputPath() {
		try {
			Session session = SessionFactory.getFactory().getSession();
			session.init(name);
			JobTemplate jt = session.createJobTemplate();

			jt.setRemoteCommand("/bin/cat");
			
			// Set the job name
			jt.setJobName(name);

			// Make sure we have the input file for the test
			assertTrue(inputFile.exists());
			
			jt.setInputPath(":" + inputPath);
			jt.setOutputPath(":" + outputPath);
			
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
			
			// Now check that the STDOUT file is actually there
			assertTrue(outputFile.exists());
			
			// More detailed checking. Actually see if the output has the right information in it.
			boolean correctOutput = false;
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains(name)) {
					correctOutput = true;
					break;
				}
			}
			reader.close();
			assertTrue(correctOutput);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Runs after the test is complete and removes the input and output files.
	 */
	@AfterClass
	public static void cleanup() {
		deleteInputFile();
		if (outputFile.exists()) {
			outputFile.delete();
		}
	}
}
