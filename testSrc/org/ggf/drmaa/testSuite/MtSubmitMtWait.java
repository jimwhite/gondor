/* -------------------------------------------------------------------------- */
/* Copyright 2002-2007 GridWay Team, Distributed Systems Architecture         */
/* Group, Universidad Complutense de Madrid                                   */
/*                                                                            */
/* Licensed under the Apache License, Version 2.0 (the "License"); you may    */
/* not use this file except in compliance with the License. You may obtain    */
/* a copy of the License at                                                   */
/*                                                                            */
/* http://www.apache.org/licenses/LICENSE-2.0                                 */
/*                                                                            */
/* Unless required by applicable law or agreed to in writing, software        */
/* distributed under the License is distributed on an "AS IS" BASIS,          */
/* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   */
/* See the License for the specific language governing permissions and        */
/* limitations under the License.                                             */
/* -------------------------------------------------------------------------- */

package org.ggf.drmaa.testSuite;
import org.ggf.drmaa.*;
import java.util.*;

public class MtSubmitMtWait extends Mt 
{
	public MtSubmitMtWait(String executable)
	{
		super(executable, MT_SUBMIT_MT_WAIT);
	}

	public void run()
	{
		try
		{
			for (int i=0; i<this.nThreads;i++)
			{
                Session threadSession = factory.getSession();
                threadSession.init(contact);
                System.out.println("Thread #" + i + " Session Init success");

                threads[i] = new ThreadExecutionSubmitWait(threadSession, executable, i, this.type, this.jobChunk);
				threads[i].start();
				threads[i].throwException();
			}

			for (int i=0; i<this.nThreads;i++)
			{
				threads[i].join();
				threads[i].throwException();
			}
									
			System.out.println("Succesfully finished test "+ this.type);

		}
		catch (Exception e)
		{
			System.err.println("Test " + this.type +" failed");
            		e.printStackTrace();
			this.stateAllTest = false;
		}

        for (int i=0; i<this.nThreads;++i) {
            try
            {
                    threads[i].session.exit();
            }
            catch (DrmaaException e)
            {
                System.err.println("Thread #" + i + " drmaa_exit() failed");
                        e.printStackTrace();
                this.stateAllTest = false;
            }
        }
	}
}
