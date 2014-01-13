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

public class MtExitDuringSubmitOrWait extends Mt 
{
	public MtExitDuringSubmitOrWait(String executable)
	{
		super(executable, MT_EXIT_DURING_SUBMIT_OR_WAIT);
	}

	public void run()
	{
		try
		{
//			this.session.init(contact);
//            System.out.println("Session Init success");

			for (int i=0; i<this.nThreads;i++)
			{
                Session threadSession = factory.getSession();
                threadSession.init(contact);
                System.out.println("Thread #" + i + " Session Init success");

				threads[i] = new ThreadExecutionSubmitWait(threadSession, this.executable, i, this.type, this.jobChunk);
                threads[i].args.set(0, "100");
				threads[i].start();
				threads[i].throwException();
			}

			System.out.println("Sleep 20 seconds");
			
			try
			{
				Thread.sleep(20000);
			}
			catch(InterruptedException e){}
			
			System.out.println("20 seconds sleeped");

            for (int i=0; i<this.nThreads;++i) {
                try
                {
                    threads[i].session.exit();
                }
                catch (DrmaaException e)
                {
                    System.err.println("Thread #" + i + " drmaa_exit() failed");
                }
            }
			
			System.out.println("Join for each job");
			for (int i=0; i<this.nThreads;i++)
			{
				threads[i].join();
				threads[i].throwException();
			}
									
			System.err.println("Test " + this.type +" failed");
		}
		catch (NoActiveSessionException e)
		{
			System.out.println("Successfully finished test "+ this.type);
		}
		catch (Exception e)
		{
			System.err.println("Test " + this.type +" failed");
            e.printStackTrace();
			this.stateAllTest = false;
		}
	}
}
