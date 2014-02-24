package org.ifcx.gondor

import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import groovy.transform.EqualsAndHashCode

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

import org.ggf.drmaa.DrmaaException
import org.ggf.drmaa.FileTransferMode
import org.ggf.drmaa.InvalidAttributeValueException
import org.ggf.drmaa.JobTemplate
import org.ggf.drmaa.PartialTimestamp
import org.ggf.drmaa.PartialTimestampFormat
import org.ggf.drmaa.UnsupportedAttributeException

/**
 * This class represents a remote job and its attributes.  It is used to
 * set up the environment for a job to be submitted.
 *
 * <h3>DRMMA Attributes</h3>
 *
 * <p>If the nativeSpecification property is set, all options contained therein
 * will be applied to the job template.  See
 * {@see: setNativeSpecification(String) setNativeSpecification} below for more
 * information.</p>
 *
 * <h3>Attribute Correlations</h3>
 *
 * <p>The following DRMAA attributes correspond to the following condor_submit
 * file directives:</p>
 *
 * <table>
 *  <tr><th>DRMAA Attribute</th><th>condor_submit directive</th></tr>
 *  <tr><td>remoteCommand</td><td>script file</td>
 *  <tr><td>args</td><td>script file arguments</td>
 *  <tr><td>jobSubmissionState = HOLD_STATE</td><td></td>
 *  <tr><td>jobEnvironment</td><td></td>
 *  <tr><td>workingDirectory = $PWD</td><td></td>
 *  <tr><td>jobCategory</td><td></td>
 *  <tr><td>nativeSpecification</td><td>ALL<sup>*</sup></td>
 *  <tr><td>emailAddresses</td><td></td>
 *  <tr><td>blockEmail = true</td><td></td>
 *  <tr><td>startTime</td><td></td>
 *  <tr><td>jobName</td><td></td>
 *  <tr><td>inputPath</td><td></td>
 *  <tr><td>outputPath</td><td></td>
 *  <tr><td>errorPath</td><td></td>
 *  <tr><td>joinFiles</td><td></td>
 *  <tr><td>transferFiles</td><td></td>
 * </table>
 *
 * <p><sup>*</sup> See the individual attribute setter description below</p>
 *
 * <p>The following attributes are unsupported by Condor-JDRMAA:</p>
 *
 * <ul>
 * <li>deadlineTime</li>
 * <li>hardWallclockTimeLimit</li>
 * <li>softWallclockTimeLimit</li>
 * <li>hardRunDurationTimeLimit</li>
 * <li>softRunDurationTimeLimit</li>
 * </ul>
 *
 * <p>Using the accessors for any of these attributes will result in an
 * {@link org.ggf.drmaa.UnsupportedAttributeException} being thrown.</p>
 *
 * @see org.ggf.drmaa.JobTemplate
 * @see org.ggf.drmaa.Session
 */

@EqualsAndHashCode(includeFields = true)
@AutoClone
public class JobTemplateImpl implements JobTemplate
{

    private static final String REMOTE_COMMAND = "remoteCommand";
    private static final String INPUT_PARAMETERS = "args";
    private static final String JOB_SUBMISSION_STATE = "jobSubmissionState";
    private static final String JOB_ENVIRONMENT = "jobEnvironment";
    private static final String WORKING_DIRECTORY = "workingDirectory";
    private static final String JOB_CATEGORY = "jobCategory";
    private static final String NATIVE_SPECIFICATION = "nativeSpecification";
    private static final String EMAIL_ADDRESS = "email";
    private static final String BLOCK_EMAIL = "blockEmail";
    private static final String START_TIME = "startTime";
    private static final String JOB_NAME = "jobName";
    private static final String INPUT_PATH = "inputPath";
    private static final String OUTPUT_PATH = "outputPath";
    private static final String ERROR_PATH = "errorPath";
    private static final String JOIN_FILES = "joinFiles";
    private static final String TRANSFER_FILES = "drmaa_transfer_files";
    /* Not supported
    private static final String DEADLINE_TIME = "drmaa_deadline_time"
    private static final String HARD_WALLCLOCK_TIME_LIMIT = "drmaa_wct_hlimit"
    private static final String SOFT_WALLCLOCK_TIME_LIMIT = "drmaa_wct_slimit"
    private static final String HARD_RUN_DURATION_LIMIT = "drmaa_run_duration_hlimit"
    private static final String SOFT_RUN_DURATION_LIMIT = "drmaa_run_duration_slimit"
    */

    private static PartialTimestampFormat ptf = new PartialTimestampFormat();

    // Job instance data
    private List<String> args = [];
    private String remoteCommand;
    private String jobCategory;
    private String jobName;
    private String nativeSpecification;
    private Set<String> email = new HashSet<String>();
    private boolean blockEmail;
    private FileTransferMode transferMode = new FileTransferMode();
    private String workingDirectory;
    private String inputPath;
    private String outputPath;
    private String errorPath;
    private boolean joinFiles;
    private int jobSubmissionState = ACTIVE_STATE;
    private PartialTimestamp startTime;
    private Map<String, String> jobEnvironment = Collections.unmodifiableMap([:]);

    private static final List<String> ATTRIBUTES = [REMOTE_COMMAND, INPUT_PARAMETERS,
            JOB_SUBMISSION_STATE, JOB_ENVIRONMENT,
            WORKING_DIRECTORY, JOB_CATEGORY,
            NATIVE_SPECIFICATION, EMAIL_ADDRESS,
            BLOCK_EMAIL, START_TIME,
            JOB_NAME, INPUT_PATH,
            OUTPUT_PATH, ERROR_PATH,
            JOIN_FILES, TRANSFER_FILES];

    private static Set<String> attributeSet = Collections.unmodifiableSet(new HashSet<String>(ATTRIBUTES));

    /**
     * Specifies the remote command to execute.  The remoteCommand must be
     * the path of an executable that is available at the job's execution host.
     * If the path is relative, it is assumed to be relative to the working
     * directory, usually set through the workingDirectory property.  If
     * workingDirectory is not set, the path is assumed to be relative to the
     * user's home directory.
     *
     * <p>The file pointed to by remoteCommand may either be an executable
     * binary or an executable script.  If a script, it must include the path to
     * the shell in a #! line at the beginning of the script.  By default, the
     * remote command will be executed directly, as by exec. (See the exec(2)
     * man page.)  To have the remote command executed in a shell, such as to
     * preserve environment settings, use the nativeSpecification property to
     * include the &quot;-shell yes&quot; option.  Jobs which are executed by a
     * wrapper shell fail differently from jobs which are executed directly.
     * When a job which contains a user error, such as an invalid path to the
     * executable, is executed by a wrapper shell, the job will execute
     * successfully, but exit with a return code of 1.  When a job which
     * contains such an error is executed directly, it will enter the
     * DRMAA_PS_FAILED state upon execution.</p>
     *
     * <p>No binary file management is done.</p>
     *
     * @param remoteCommand {@inheritDoc}
     * @throws org.ggf.drmaa.DrmaaException {@inheritDoc}
     */
    public void setRemoteCommand(String remoteCommand) throws DrmaaException {
        this.remoteCommand = remoteCommand;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setRemoteCommand(String)
     */
    public String getRemoteCommand() throws DrmaaException {
        return remoteCommand;
    }

    /**
     * Specifies the arguments to the job.
     *
     * @param args {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setArgs(List args) throws DrmaaException {
        this.args = Collections.unmodifiableList(args.collect { it.toString() });
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setArgs(List)
     */
    public List<String> getArgs() throws DrmaaException {
        args
    }

    /**
     * Specifies the job state at submission. The possible values are
     * {@link #HOLD_STATE} and {@link #ACTIVE_STATE}
     *
     * <ul>
     *  <li><code>ACTIVE</code> means the job is submitted in a runnable
     *  state.</li>
     *  <li><code>HOLD</code> means the job is submitted in user hold state
     *  (either <code>Session.USER_ON_HOLD</code> or
     *  <code>Session.USER_SYSTEM_ON_HOLD</code>).</li>
     * </ul>
     *
     * @param state {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setJobSubmissionState(int state) throws DrmaaException {
        if (state in [HOLD_STATE, ACTIVE_STATE]) {
            this.jobSubmissionState = state
        } else {
            throw new InvalidAttributeValueException("jobSubmissionState attribute is invalid");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setJobSubmissionState(int)
     */
    public int getJobSubmissionState() throws DrmaaException {
        jobSubmissionState
    }

    /**
     * Sets the environment values that define the remote environment.  The
     * values override the remote environment values if there is a collision.
     *
     * @param env {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setJobEnvironment(Map env) throws DrmaaException {
        jobEnvironment = Collections.unmodifiableMap(env.collectEntries { k, v -> [k.toString(), v.toString()] });
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setJobEnvironment(Map)
     */
    public Map getJobEnvironment() throws DrmaaException {
        jobEnvironment
    }

    /**
     * Specifies the directory name where the job will be executed.
     * A <CODE>HOME_DIRECTORY</CODE> placeholder at the beginning of the
     * directory name denotes that the remaining portion of the directory name
     * is to be resolved relative to the job submiter's home directory on the
     * execution host.
     *
     * <p>When the DRMAA job template is used for bulk job submission (see also
     * {@link org.ggf.drmaa.Session#runBulkJobs(org.ggf.drmaa.JobTemplate, int, int, int) runBulkJobs}
     * the <CODE>PARAMETRIC_INDEX</CODE> placeholder can be used at any position
     * within the directory name to cause a substitution with the parametric
     * job's index.  The directory name must be specified in a syntax that is
     * common at the host where the job will be executed.  If no placeholder is
     * used, an absolute directory specification is recommended. If set to a
     * relative path and no placeholder is used, a path relative to the user's
     * home directory is assumed.  If not set, the working directory will
     * default to the user's home directory.  If set, and the directory does
     * not exist, the job will enter the state <code>FAILED</code> when the job
     * is run.</p>
     *
     * @param workingDirectory {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see org.ggf.drmaa.JobTemplate#HOME_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#PARAMETRIC_INDEX
     * @see org.ggf.drmaa.Session#FAILED
     */
    public void setWorkingDirectory(String workingDirectory) throws DrmaaException {
        this.workingDirectory = workingDirectory;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setWorkingDirectory(String)
     */
    public String getWorkingDirectory() throws DrmaaException {
        return workingDirectory;
    }

    // TODO: Explain how category is used, if at all...

    /**
     * Specifies the DRMAA job category.
     *
     * @param category {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setJobCategory(String category) throws DrmaaException {
        this.jobCategory = category;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setJobCategory(String)
     */
    public String getJobCategory() throws DrmaaException {
        return jobCategory;
    }

    /**
     * Specifies native condor_submit options which will be interpreted as part of the
     * DRMAA job template. All options available to <code>condor_submit</code> may be
     * used in the nativeSpecification. Options set in the nativeSpecification will be
     * overridden by the corresponding DRMAA properties.
     *
     * @param spec {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see "condor_submit documentation/man page"
     */
    public void setNativeSpecification(String spec) throws DrmaaException {
        nativeSpecification = spec;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setNativeSpecification(String)
     */
    public String getNativeSpecification() throws DrmaaException {
        return nativeSpecification;
    }

    /**
     * Set the list of email addresses used to report the job completion and
     * status. For Condor, only one email address is supported.
     *
     * @param email {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setEmail(Set email) throws DrmaaException {
        this.email = Collections.unmodifiableSet(email.collect { it.toString() } as Set);
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setEmail(java.util.Set)
     */
    public Set getEmail() throws DrmaaException {
        email
    }

    /**
     * Specifies whether e-mail sending is to be blocked or not. By default, email
     * is not sent.
     *
     * @param blockEmail {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setBlockEmail(boolean blockEmail) throws DrmaaException {
        this.blockEmail = blockEmail
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setBlockEmail(boolean)
     */
    public boolean getBlockEmail() throws DrmaaException {
        blockEmail
    }

    /**
     * Set the earliest time when the job may be eligible to be run.
     *
     * @param startTime {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see org.ggf.drmaa.PartialTimestamp
     */
    public void setStartTime(PartialTimestamp startTime) throws DrmaaException {
        if (startTime.getTimeInMillis() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Start time is in the past.");
        }
        this.startTime = (PartialTimestamp) startTime.clone();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setStartTime(org.ggf.drmaa.PartialTimestamp)
     */
    public PartialTimestamp getStartTime() throws DrmaaException {
        (PartialTimestamp) startTime?.clone()
    }

    /**
     * Set the name of the job.  A job name must be be comprised of alpha-numeric
     * and _ characters only.
     *
     * @param name {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setJobName(String name) throws DrmaaException {
        if (!name.matches(/[A-Za-z0-9_.]+/)) {
            throw new InvalidAttributeValueException("Illegal characters in the job name.");
        }

        if (name.startsWith('_')) {
            throw new InvalidAttributeValueException("Job name can't start with '_', it is reserved for generated job names.");
        }

        this.jobName = name;
    }

//    /**
//     * Set the name of the job to one generated by our code.
//     *
//     * @param name {@inheritDoc}
//     * @throws DrmaaException {@inheritDoc}
//     */
//    void setGeneratedJobName(String name) throws DrmaaException {
//        if (!name.matches(/^[A-Za-z0-9_.]+/)) {
//            throw new InvalidAttributeValueException("Illegal characters in the job name.");
//        }
//
//        if (!name.startsWith('_')) name = '_' + name
//
//        this.jobName = name;
//    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setJobName(String)
     */
    public String getJobName() throws DrmaaException {
        jobName
    }

    /**
     * Set the job's standard input path. Unless set elsewhere, if not
     * explicitly set in the job template, the job is started with an empty
     * input stream. If the standard input is set, it specifies the network path
     * of the job's input stream file in the form of
     * <code>[hostname]:file_path</code><br>
     *
     * <p>
     * When the DRMAA job template is used for bulk job submission (see also
     * {@link org.ggf.drmaa.Session#runBulkJobs(org.ggf.drmaa.JobTemplate, int, int, int)
     * runBulkJobs} the <CODE>PARAMETRIC_INDEX</CODE> placeholder can be used at
     * any position within the file name to cause a substitution with the
     * parametric job's index. A <CODE>HOME_DIRECTORY</CODE> placeholder at the
     * beginning of the file path denotes the remaining portion of the file path
     * as a relative file specification to be resolved relative to the job
     * user's home directory at the host where the file is located. A
     * <CODE>WORKING_DIRECTORY</CODE> placeholder at the beginning of file path
     * denotes the remaining portion of the file path as a relative file
     * specification to be resolved relative to the job's working directory at
     * the host where the file is located. The file name must be specified in a
     * syntax that is common at the host where the job will be executed. If no
     * home or working directory placeholder is used, an absolute file
     * specification is recommended. If set to a relative file path and no home
     * or working directory placeholder is used, a path relative to the user's
     * home directory is assumed.
     * </p>
     *
     * <p>
     * When the job is run, if this attribute is set, and the file can't be
     * read, the job will enter the state <code>FAILED</code>.
     * </p>
     *
     * @param inputPath {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see org.ggf.drmaa.JobTemplate#HOME_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#WORKING_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#PARAMETRIC_INDEX
     * @see org.ggf.drmaa.Session#FAILED
     */
    public void setInputPath(String inputPath) throws DrmaaException {
        this.inputPath = inputPath;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setInputPath(String)
     */
    public String getInputPath() throws DrmaaException {
        return inputPath;
    }

    /**
     * Sets how to direct the job's standard output.  Unless set elsewhere, if
     * not explicitly set in the job template, the whereabouts of the job's
     * output stream is not defined.  If the standard output is set, it
     * specifies the network path of the job's output stream file in the form of
     * <code>[hostname]:file_path<code>
     *
     * <p>When the DRMAA job template is used for bulk job submission (see also
     * {@link org.ggf.drmaa.Session#runBulkJobs(org.ggf.drmaa.JobTemplate, int, int, int) runBulkJobs}
     * the <CODE>PARAMETRIC_INDEX</CODE> placeholder can be used at any position
     * within the file name to cause a substitution with the parametric
     * job's index.  A <CODE>HOME_DIRECTORY</CODE> placeholder at the beginning
     * of the file path denotes the remaining portion of the file path as a
     * relative file specification to be resolved relative to the job user's
     * home directory at the host where the file is located.  A
     * <CODE>WORKING_DIRECTORY</CODE> placeholder at the beginning of file path
     * denotes the remaining portion of the file path as a relative file
     * specification to be resolved relative to the job's working directory at
     * the host where the file is located.  The file name must be specified in a
     * syntax that is common at the host where the job will be executed.  If no
     * home or working directory placeholder is used, an absolute file
     * specification is recommended. If set to a relative file path and no home
     * or working directory placeholder is used, a path relative to the user's
     * home directory is assumed.</p>
     *
     * <p>When the job is run, if this attribute is set, and the file can't be
     * read, the job will enter the state <code>FAILED</code>.</p>
     *
     * @param outputPath {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see org.ggf.drmaa.JobTemplate#HOME_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#WORKING_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#PARAMETRIC_INDEX
     * @see org.ggf.drmaa.Session#FAILED
     */
    public void setOutputPath(String outputPath) throws DrmaaException {
        this.outputPath = outputPath;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setOutputPath(String)
     */
    public String getOutputPath() throws DrmaaException {
        return outputPath;
    }

    /**
     * Sets how to direct the job's standard error.  Unless set elsewhere, if
     * not explicitly set in the job template, the whereabouts of the job's
     * error stream is not defined.  If the standard error is set, it
     * specifies the network path of the job's error stream file in the form of
     * <code>[hostname]:file_path</code><br>
     *
     * <p>When the DRMAA job template is used for bulk job submission (see also
     * {@link org.ggf.drmaa.Session#runBulkJobs(org.ggf.drmaa.JobTemplate, int, int, int) runBulkJobs}
     * the <code>PARAMETRIC_INDEX</code> placeholder can be used at any position
     * within the file name to cause a substitution with the parametric
     * job's index.  A <code>HOME_DIRECTORY</code> placeholder at the beginning
     * of the file path denotes the remaining portion of the file path as a
     * relative file specification to be resolved relative to the job user's
     * home directory at the host where the file is located.  A
     * <code>WORKING_DIRECTORY</code> placeholder at the beginning of file path
     * denotes the remaining portion of the file path as a relative file
     * specification to be resolved relative to the job's working directory at
     * the host where the file is located.  The file name must be specified in a
     * syntax that is common at the host where the job will be executed.  If no
     * home or working directory placeholder is used, an absolute file
     * specification is recommended. If set to a relative file path and no home
     * or working directory placeholder is used, a path relative to the user's
     * home directory is assumed.</p>
     *
     * <p>When the job is run, if this attribute is set, and the file can't be
     * read, the job will enter the state <code>FAILED</code>.</p>
     *
     * @param errorPath {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see org.ggf.drmaa.JobTemplate#HOME_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#WORKING_DIRECTORY
     * @see org.ggf.drmaa.JobTemplate#PARAMETRIC_INDEX
     * @see org.ggf.drmaa.Session#FAILED
     */
    public void setErrorPath(String errorPath) throws DrmaaException {
        this.errorPath = errorPath;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setErrorPath(String)
     */
    public String getErrorPath() throws DrmaaException {
        return errorPath;
    }

    /**
     * Sets whether the error stream should be intermixed with the output
     * stream. If not explicitly set in the job template the attribute defaults
     * to <code>false</code>.  If <code>true</code>, the underlying DRM system
     * will ignore the value of the errorPath property and intermix the standard
     * error stream with the standard output stream as specified with
     * outputPath.
     *
     * @param join {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     */
    public void setJoinFiles(boolean join) throws DrmaaException {
        joinFiles = join;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setJoinFiles(boolean)
     */
    public boolean getJoinFiles() throws DrmaaException {
        return joinFiles;
    }

    /**
     * <p>Set Transfer Files</p>
     *
     * {@inheritDoc}
     *
     * @param mode {@inheritDoc}
     * @throws DrmaaException {@inheritDoc}
     * @see #setInputPath(String)
     * @see #setOutputPath(String)
     * @see #setErrorPath(String)
     */
    public void setTransferFiles(FileTransferMode mode) throws DrmaaException {
        transferMode = mode.clone()
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws DrmaaException {@inheritDoc}
     * @see #setTransferFiles(org.ggf.drmaa.FileTransferMode)
     */
    public FileTransferMode getTransferFiles() throws DrmaaException {
        transferMode.clone()
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     *
     * @param deadline a {@link PartialTimestamp}
     * @throws org.ggf.drmaa.UnsupportedAttributeException unsupported property
     */
    public void setDeadlineTime(PartialTimestamp deadline)
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The deadlineTime attribute " +
                "is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @return a {@link PartialTimestamp}
     * @throws UnsupportedAttributeException unsupported property
     */
    public PartialTimestamp getDeadlineTime()
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The deadlineTime attribute " +
                "is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     *
     * @param hardWallclockLimit a <code>long</code>
     * @throws UnsupportedAttributeException unsupported property
     */
    public void setHardWallclockTimeLimit(long hardWallclockLimit)
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The hardWallclockTimeLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @return a <code>long</code>
     * @throws UnsupportedAttributeException unsupported property
     */
    public long getHardWallclockTimeLimit()
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The hardWallclockTimeLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @param softWallclockLimit a <code>long</code>
     * @throws UnsupportedAttributeException unsupported property
     */
    public void setSoftWallclockTimeLimit(long softWallclockLimit)
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The softWallclockTimeLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @return a <code>long</code>
     * @throws UnsupportedAttributeException unsupported property
     */
    public long getSoftWallclockTimeLimit()
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The softWallclockTimeLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @throws UnsupportedAttributeException unsupported property
     */
    public void setHardRunDurationLimit(long hardRunLimit)
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The hardRunDurationLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @throws UnsupportedAttributeException unsupported property
     */
    public long getHardRunDurationLimit()
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The hardRunDurationLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @throws UnsupportedAttributeException unsupported property
     */
    public void setSoftRunDurationLimit(long softRunLimit)
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The softRunDurationLimit " +
                "attribute is not supported.");
    }

    /**
     * Unsupported property.  Will throw an UnsupportedAttributeException if
     * called.
     * @throws UnsupportedAttributeException unsupported property
     */
    public long getSoftRunDurationLimit()
    throws UnsupportedAttributeException {
        throw new UnsupportedAttributeException("The softRunDurationLimit " +
                "attribute is not supported.");
    }

//    /**
//     * Uses the SessionImpl instance to set the attribute in the native job
//     * template associated with this JobTemplateImpl instance.
//     */
//    private void setAttribute(String name, Collection value) throws DrmaaException {
//        if (!isValidAttribute(name)) {
//            throw new UnsupportedAttributeException(name + " is not a valid attribute.");
//        }
//
//        // The attributes names that we compare 'name' to below are the only
//        // ones that can handle collections of values (list, set, etc...)
//        // If another attribute is supplied with a Collection, then there is
//        // a usage error. We therefore throw an InvalidAttributeValueException.
//        if (name.equals(INPUT_PARAMETERS)) {
//            this.setArgs(Arrays.asList(value));
//        } else if (name.equals(JOB_ENVIRONMENT)) {
//            HashMap<String, String> env = new HashMap<String, String>();
//            Iterator<String> iter = value.iterator();
//            while (iter.hasNext()) {
//                String pair = (String) iter.next();
//                if (pair.contains("=")) {
//                    String[] pairArray = pair.split("=");
//                    env.put(pairArray[0], pairArray[1]);
//                }
//            }
//            this.setJobEnvironment(env);
//        } else if (name.equals(EMAIL_ADDRESS)) {
//            HashSet<String> emails = new HashSet<String>();
//            emails.addAll(value);
//            this.setEmail(emails);
//        } else {
//            throw new InvalidAttributeValueException();
//        }
//    }
//
//    private boolean isValidAttribute(String attribute) {
//        // Assume false, and work to see if it is true
//        boolean valid = false;
//
//        try {
//            Set<String> attributes;
//            attributes = this.getAttributeNames();
//            if (attributes.contains(attribute)) {
//                valid = true;
//            }
//        } catch (DrmaaException e) {
//            // This should never happen with this implementation
//            e.printStackTrace();
//        }
//
//        return valid;
//    }
//
//    /*
//     * Utility method to set attributes. Caller passes the name of the attribute
//     * and the value.
//     */
//
//    private void setAttribute(String name, String value) throws DrmaaException {
//        if (!isValidAttribute(name)) {
//            throw new UnsupportedAttributeException(name + " is not a valid attribute.");
//        }
//
//        if (name.equals(REMOTE_COMMAND)) {
//            this.setRemoteCommand(value);
//        } else if (name.equals(JOB_SUBMISSION_STATE)) {
//            int submissionState = Integer.parseInt(value);
//            this.setJobSubmissionState(submissionState);
//        } else if (name.equals(WORKING_DIRECTORY)) {
//            this.setWorkingDirectory(value);
//        } else if (name.equals(JOB_CATEGORY)) {
//            this.setJobCategory(value);
//        } else if (name.equals(NATIVE_SPECIFICATION)) {
//            this.setNativeSpecification(value);
//        } else if (name.equals(BLOCK_EMAIL)) {
//            boolean block = false;
//            if (value.equals(BLOCK_EMAIL_TRUE_STRING)) {
//                ;
//                block = true;
//            } else if (value.equals(BLOCK_EMAIL_FALSE_STRING)) {
//                block = false;
//            } else {
//                throw new InvalidAttributeValueException("Unable to parse value for email blocking.");
//            }
//            this.setBlockEmail(block);
//        } else if (name.equals(START_TIME)) {
//            PartialTimestampFormat ptsFormat = new PartialTimestampFormat();
//            PartialTimestamp pts;
//            try {
//                pts = ptsFormat.parse(value);
//            } catch (ParseException e) {
//                throw new InvalidAttributeValueException("Unable to parse start time value.");
//            }
//            this.setStartTime(pts);
//        } else if (name.equals(JOB_NAME)) {
//            this.setJobName(value);
//        } else if (name.equals(INPUT_PATH)) {
//            this.setInputPath(value);
//        } else if (name.equals(OUTPUT_PATH)) {
//            this.setOutputPath(value);
//        } else if (name.equals(ERROR_PATH)) {
//            this.setErrorPath(value);
//        } else if (name.equals(JOIN_FILES)) {
//            boolean join = false;
//            if (value.equals(JOIN_FILES_FALSE_STRING)) {
//                join = false;
//            } else if (value.equals(JOIN_FILES_TRUE_STRING)) {
//                join = true;
//            } else {
//                throw new InvalidAttributeValueException("Bad 'join files' attribute value.");
//            }
//            this.setJoinFiles(join);
//        } else if (name.equals(TRANSFER_FILES)) {
//            FileTransferMode ftm = new FileTransferMode();
//            if (value.contains("i")) {
//                ftm.setInputStream(true);
//            }
//            if (value.contains("o")) {
//                ftm.setOutputStream(true);
//            }
//            if (value.contains("e")) {
//                ftm.setErrorStream(true);
//            }
//            this.setTransferFiles(ftm);
//        } else {
//            throw new UnsupportedAttributeException(name + " is not a valid attribute.");
//        }
//    }

    /**
     * Returns the list of supported properties names.</p>
     *
     * @return {@inheritDoc}
     */
    public Set<String> getAttributeNames() throws DrmaaException {
        return attributeSet;
    }

}
