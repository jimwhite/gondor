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

/**
 * Problem occurred when running a Condor binary invocation.
 */
public class CondorExecException extends Exception {

	private static final long serialVersionUID = 6154809652126192112L;

	/**
     * Creates a new instance of <code>CondorExecException</code> without a detailed
     * message.
     */
    public CondorExecException() {
    	// No-arguments exception
    }

    /**
     * Constructs an instance of <code>CondorExecException</code> with the
     * specified detailed message.
     * 
     * @param message a detailed message.
     */
    public CondorExecException(String message) {
        super(message);
    }
    
    /**
     * A constructor that accepts a {@link Throwable} so that exceptions can be wrapped.
     * 
     * @param message a detailed message
     * @param cause a {@link Throwable}
     */
    public CondorExecException(String message, Throwable cause) {
        super(message, cause);
    } 
}
