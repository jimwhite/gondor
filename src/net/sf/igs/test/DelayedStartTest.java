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

import java.util.Calendar;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.PartialTimestamp;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.junit.Test;

/**
 * Tests whether support for the
 * {@link JobTemplate#setStartTime(PartialTimestamp) setStartTime} method works
 * properly. A job should be held be Condor until the start time is reached, at
 * which point the job should be released and scheduled for execution.
 */
public class DelayedStartTest {
	private static String name = DelayedStartTest.class.getSimpleName();
	
	/**
	 * Ensure that the {@link JobTemplate#setErrorPath(String) setErrorPath}
	 * implementation works properly.
	 */
	@Test
	public void testDelayedStart() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			session.init(DelayedStartTest.class.getSimpleName());

			// Get a job template
			JobTemplate jt = TestUtils.getSleepJobTemplate(session, name);
			assertNotNull(jt);
			
			// Need to have a start time that is in the future, but not too much in the future or
			// our test will take forever to run... How about 3 minutes?
			PartialTimestamp now = getCurrentPartialTimestamp();
			long nowMillis = now.getTimeInMillis();
			long startMillis = nowMillis + (3 * 60 * 1000);
			PartialTimestamp startTime = new PartialTimestamp();
			startTime.setTimeInMillis(startMillis);
			
			// Do some quick sanity checks
			assertNotNull(startTime);
			assertTrue(startTime.after(now));
			
			// And here it is, this is the business right here...
			jt.setStartTime(startTime);
			
			String jobId = session.runJob(jt);
			assertNotNull(jobId);
			assertTrue(jobId.length() > 0);

			boolean released = ! TestUtils.isJobHeld(jobId);
			assertFalse(released);
			
			boolean releasedAfterStartTime = false;
			byte minutesLooped = 0;
			byte maxMinutes = 6; // The maximum number of minutes to wait...
			
			while (minutesLooped < maxMinutes) {
				// Sleep for a minute
				Thread.sleep(60*1000);
				released = ! TestUtils.isJobHeld(jobId);
				
				// Get the current timestamp
				now = getCurrentPartialTimestamp();
				
				if (released) {
					if (now.before(startTime)) {
						// Bad news. Seems that the job was released before it was supposed to
						break;
					}
					
					if (now.after(startTime)) {
						// Great! The job has been released after the configured start time
						releasedAfterStartTime = true;
						break;
					}
				}

				minutesLooped++;
				System.out.println("Waiting another minute for job to be released. Minutes waited: " + minutesLooped);
			}

			// Just check that the job was released AFTER when we said it should
			assertTrue(releasedAfterStartTime);
			
			// Free job template resources
			session.deleteJobTemplate(jt);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (session != null) {
				try {
					session.exit();
					session = null;
				} catch (DrmaaException e) {
					// ignored
				}
			}
		}
	}
	
	/*
	 * Create a PartialTimestamp corresponding to the current time. Due to the
	 * nature of these partial time stamps, we add a few seconds to make sure
	 * we get a time in the near future so we don't accidentally miss it.
	 */
	private PartialTimestamp getCurrentPartialTimestamp() {
		Calendar cal = Calendar.getInstance();
		
		PartialTimestamp now = new PartialTimestamp();
		now.set(PartialTimestamp.YEAR, cal.get(Calendar.YEAR));
		now.set(PartialTimestamp.DATE, cal.get(Calendar.DAY_OF_MONTH));
		now.set(PartialTimestamp.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
		now.set(PartialTimestamp.MINUTE, cal.get(Calendar.MINUTE));
		now.set(PartialTimestamp.SECOND, cal.get(Calendar.SECOND) + 3);
		
		return now;
	}
}
