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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import net.sf.igs.JobInfoImpl;
import net.sf.igs.JobLogParser;

import org.ggf.drmaa.JobInfo;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link JobLogParser} class.
 */
public class JobLogParserTest {

	private static File testDir;
	
	/**
	 * Performs initial setup and validation before the tests are executed.
	 * Checks whether the "test.data.dir" system property has been properly set.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setup() throws Exception {
		String testDataDir = System.getProperty("test.data.dir");
		if (testDataDir == null) {
			throw new Exception("Please define the test.data.dir system property.");
		}
		
		testDir = new File(testDataDir);
		if (! (testDir.exists() && testDir.isDirectory())) {
			throw new Exception("Invalid path to the test data directory: " + testDataDir);
		}
	}
	
	/**
	 * Test the creation of parsers
	 */
	@Test
	public void constructorTest() {
		File test1 = new File(testDir, "test1.log");

		try {
			// Test the constructor using a file
			JobLogParser parser = new JobLogParser(test1);
			assertNotNull(parser);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Test Condor job log parsing.
	 */
	@Test
	public void parseTest() {
		File test1 = new File(testDir, "test1.log");

		try {
			// Test the constructor using a file
			JobLogParser parser = new JobLogParser(test1);
			assertNotNull(parser);
			
			JobInfo info = parser.parse();
			assertNotNull(info);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the mechanism for defining the status of jobs, which is controlled
	 * by a bit vector.
	 */
	@Test
	public void testStatusBits() {
		int status = JobInfoImpl.EXITED_BIT;
		status += JobInfoImpl.SIGNALED_BIT;

		JobInfoImpl j1 = new JobInfoImpl(null, status, null, null);
		assertNotNull(j1);
		assertTrue(j1.hasExited());
		assertTrue(j1.hasSignaled());
		
		status = 0;
		JobInfoImpl j2 = new JobInfoImpl(null, status, null, null);
		assertNotNull(j2);
		assertFalse(j2.hasExited());
		assertFalse(j2.hasSignaled());
		
		status += JobInfoImpl.SIGNALED_BIT;
		JobInfoImpl j3 = new JobInfoImpl(null, status, null, null);
		assertNotNull(j3);
		assertFalse(j3.hasExited());
		assertTrue(j3.hasSignaled());
	}
	
	/**
	 * Test the method of setting the exit value when using {@link JobLogParser}.
	 * The exit value is encoded with a bit vector.
	 */
	@Test
	public void testExitValueBits() {
		int exitValue = 25;
		int status = 0x00000000 + JobInfoImpl.EXITED_BIT;
		status += (exitValue << JobInfoImpl.EXIT_STATUS_OFFSET);
		JobInfoImpl j = new JobInfoImpl(null, status, null, null);
		assertNotNull(j);
		assertEquals(exitValue, j.getExitStatus());
	}
}
