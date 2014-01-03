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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggf.drmaa.InvalidJobException;
import org.ggf.drmaa.JobInfo;

/**
 * Parses Condor job log files and creates {@link JobInfo} objects from them as well
 * as providing access to job details such as the start, execute and end times
 * of the job.
 */
public class JobLogParser {

	private String jobId = null;
	private File logFile;
	private boolean parsed = false;
	private Date submissionTime, startTime, endTime;
	private Pattern pattern = Pattern.compile("\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}");
	private int runBytesSent = 0;
	private int exitValue = -1;
	private int signal = -1;
	
	/**
	 * Constructor.
	 * 
	 * @param logFile a <code>String</code>
	 * @throws InvalidJobException 
	 * @throws IOException 
	 */
	public JobLogParser(File logFile) throws InvalidJobException, IOException {
		this.logFile = logFile;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param jobId a <code>String</code>
	 * @throws InvalidJobException 
	 */
	public JobLogParser(String jobId) throws InvalidJobException {
		if (! Util.validJobId(jobId)) {
			throw new InvalidJobException("Invalid job id: " + jobId);
		}
		String log = Util.getLogFromId(jobId);
		File logFile = new File(log);
		
		if (! (logFile.exists() && logFile.isFile() && logFile.canRead())) {
			throw new IllegalArgumentException("Log file doesn't exist or isn't readable.");
		}
		this.logFile = logFile;
	}
	
	/**
	 * Retrieve the submission time of the job.
	 * 
	 * @return a {@link Date} object
	 */
	public Date getSubmitTime() {
		if (! parsed) {
			throw new IllegalStateException("File hasn't been parsed yet.");
		}
		return submissionTime;
	}
	
	/**
	 * Retrieve the start time of the job.
	 * 
	 * @return a {@link Date} object
	 */
	public Date getStartTime() {
		if (! parsed) {
			throw new IllegalStateException("File hasn't been parsed yet.");
		}
		return startTime;
	}
	
	/**
	 * Retrieve the end time of the job.
	 * 
	 * @return a {@link Date} object
	 */
	public Date getEndTime() {
		if (! parsed) {
			throw new IllegalStateException("File hasn't been parsed yet.");
		}
		return endTime;
	}
	
	/**
	 * Parses the job's log file and returns a {@link JobInfo} object that describes
	 * the job and its status.
	 * 
	 * @return a {@link JobInfo} object
	 * @throws IOException 
	 */
	public JobInfo parse() throws IOException {
		int status = 0x00000000;
		
		try {
			BufferedReader buf = new BufferedReader(new FileReader(logFile));
			String line;
			while ((line = buf.readLine()) != null) {
				if (line.contains("Job submitted from host") && (submissionTime == null)) {
					// We check if submissionTime is null so that we don't overwrite
					// a previously determined submission time, if multiple submission times
					// are found in the log file.
					submissionTime = parseDate(line);
				} else if (line.contains("Job executing on host")) {
					startTime = parseDate(line);
				} else if (line.contains("Job terminated")) {
					// If we have finished, but never started executing, then we never
					// actually ran. Perhaps we were aborted or some such...
					if (startTime == null) {
						status += JobInfoImpl.NEVERRAN_BIT;
					}
					// Okay, we know the job is done. But was it successful?
					status += JobInfoImpl.EXITED_BIT;
					
					endTime = parseDate(line);

					while ((line = buf.readLine()) != null) {
						if (line.contains("Normal termination")) {
							Pattern rvPattern = Pattern.compile("return value (\\d+)");
							Matcher rvMatcher = rvPattern.matcher(line);
							if (rvMatcher.find()) {
								exitValue = Integer.parseInt(rvMatcher.group(1));
								// Set the status by performing the correct bit operations
								status += (exitValue << JobInfoImpl.EXIT_STATUS_OFFSET);
							}
						} else if (line.contains("Abnormal termination")) {
							Pattern signalPattern = Pattern.compile("signal \\d+");
							Matcher sigMatcher = signalPattern.matcher(line);
							if (sigMatcher.find()) {
								signal = Integer.parseInt(sigMatcher.group(1));
								status += JobInfoImpl.SIGNALED_BIT;
							}
							line = buf.readLine();
							if (line.contains("Corefile in:")) {
								status += JobInfoImpl.COREDUMP_BIT;
							}
						}
					}
				}
			}
			buf.close();
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		
		parsed = true;
		
		// Put together the JobInfoImpl that we will return.
		String[] resources = getResourceUsage();
		// TODO: The signal is probably wrong here
		JobInfoImpl info = new JobInfoImpl(jobId, status, resources, signal + "");
		return info;
	}
	
	/**
	 * Parse a Condor date string found in job log files into a {@link Date}
	 * If the line provided does not contain a date, null is returned.
	 * 
	 * @param condorLogLine a <code>String</code> containing a log file line
	 * @return a {@link Date}
	 */
	private Date parseDate(String condorLogLine) {
		Date date = null;
		Matcher matcher = pattern.matcher(condorLogLine);
		if (matcher.find()) {
			String dateSpec = matcher.group();
			int year = Calendar.getInstance().get(Calendar.YEAR);
            String dateSpecWithYear = year + "/" + dateSpec; 
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			try {
				date = sdf.parse(dateSpecWithYear);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return date;
	}
	
	/**
	 * After the log file is parsed with the {@link #parse()} method, the caller
	 * can retrieve resource usage information with this method. The returned
	 * list contains strings which are key/value pairs delimited by an '=' sign.
	 * 
	 * @return an array of strings
	 */
	public String[] getResourceUsage() {
		if (! parsed) {
			throw new IllegalStateException("File hasn't been parsed yet.");
		}
		// The KV suffix is for Key/Value
		ArrayList<String> resources = new ArrayList<String>();
		if (submissionTime != null) {
			String submissionKV = "submission_time=" + submissionTime;
			resources.add(submissionKV);
		}
		if (submissionTime != null && startTime != null) {
			String startTimeKV = "start_time=" + startTime;
			resources.add(startTimeKV);
		}
		if (submissionTime != null && startTime != null && endTime != null) {
			String endTimeKV = "end_time=" + endTime;
			resources.add(endTimeKV);
		}
		
		String runBytesSentKV = "run_bytes_sent=" + runBytesSent;
		resources.add(runBytesSentKV);
		String[] specArray = resources.toArray(new String[resources.size()]);
		return specArray;
	}
	
	/**
	 * A main method so that this class can be invoked as an application.
	 * 
	 * @param args an array of Strings
	 */
	public static void main(String[] args) {
		// Check that we have been invoked correctly (1 argument)
		if (args.length != 1) {
			System.err.println("java " + JobLogParser.class.getCanonicalName() + "<condor job log>");
			System.exit(1);
		}

		try {
			String jobLog = args[0];
			File jobLogFile = new File(jobLog);
			JobLogParser jlp = new JobLogParser(jobLogFile);
			JobInfo info = jlp.parse();
			Map<String, String> resources = info.getResourceUsage();
			
			// Now that we have the resources map, iterator through the keys
			// and output the values in an intelligible way.
			Iterator<String> iter = resources.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				System.out.println(key + "=" + resources.get(key));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
