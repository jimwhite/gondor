package net.sf.igs;

import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.*;

/**
 * This class provides information about a completed Grid Engine job.
 * @see org.ggf.drmaa.JobInfo
 * @author  dan.templeton@sun.com
 * @since 0.5
 * version 1.0
 */
public class JobInfoImpl implements JobInfo {
    public static final int EXITED_BIT = 0x00000001;
    public static final int SIGNALED_BIT = 0x00000002;
    public static final int COREDUMP_BIT = 0x00000004;
    public static final int NEVERRAN_BIT = 0x00000008;
    /* POSIX exit status has only 8 bit */
    public static final int EXIT_STATUS_BITS = 0x00000FF0;
    public static final int EXIT_STATUS_OFFSET = 4;
    private final String signal;
    private final int status;
    private final String jobId;
    private final Map resources;
    
    /**
     * Creates a new instance of JobInfoImpl
     * @param jobId the job id string
     * @param status an opaque status code
     * @param resourceUsage an array of name=value resource usage pairs
     * @param signal the string description of the terminating signal
     */
    public JobInfoImpl(String jobId, int status, String[] resourceUsage, String signal) {
        this.jobId = jobId;
        this.status = status;
        this.resources = nameValuesToMap(resourceUsage);
        this.signal = signal;
    }
    
    public int getExitStatus() {
        if (!hasExited()) {
            throw new IllegalStateException();
        }

        return ((status & EXIT_STATUS_BITS) >> EXIT_STATUS_OFFSET);
    }
    
    /**
     * If hasSignaled() returns true, this method returns a representation of
     * the signal that caused the termination of the job. For signals declared
     * by POSIX or otherwise known to Grid Engine, the symbolic names are
     * returned (e.g., SIGABRT, SIGALRM).<BR>
     * For signals not known by Grid Engine, the string &quot;unknown
     * signal&quot; is returned.
     * @return the name of the terminating signal
     */
    public String getTerminatingSignal() {
        if (!hasSignaled()) {
            throw new IllegalStateException();
        }

        return signal;
    }
    
    public boolean hasCoreDump() {
        return ((status & COREDUMP_BIT) != 0);
    }
    
    public boolean hasExited() {
        return ((status & EXITED_BIT) != 0);
    }
    
    public boolean hasSignaled() {
        return ((status & SIGNALED_BIT) != 0);
    }
    
    public boolean wasAborted() {
        return ((status & NEVERRAN_BIT) != 0);
    }

    public String getJobId() {
        return jobId;
    }

    public Map getResourceUsage() {
        return resources;
    }
    
    private static Map nameValuesToMap(String[] nameValuePairs) {
        Map map = null;

        if (nameValuePairs != null) {
            map = new HashMap();

            for (int count = 0; count < nameValuePairs.length; count++) {
                int equals = nameValuePairs[count].indexOf('=');
                map.put(nameValuePairs[count].substring(0, equals), nameValuePairs[count].substring(equals + 1));
            }
        }

        return map;
    }
}
