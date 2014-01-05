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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.ggf.drmaa.AlreadyActiveSessionException;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.InvalidContactStringException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.ggf.drmaa.Version;
import org.junit.Test;

/**
 * Test DRMAA sessions.
 */
public class SessionTest {

	private static final String CONTACT = SessionTest.class.getSimpleName();
	
	/**
	 * Test session initialization.
	 */
	@Test
	public void sessionInitializationTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			assertNotNull(session);
			
			session.init(CONTACT);
			assertNotNull(session);
		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Tests whether a call to initialize a session with {@link Session#init(String) init}
	 * when there is already an initialized session that is active results in the
	 * proper exception being thrown or not.
	 * 
	 * @throws DrmaaException
	 */
	@Test(expected=AlreadyActiveSessionException.class)
	public void testInitWithActiveSession() throws DrmaaException {
		Session session = null;
		session = SessionFactory.getFactory().getSession();
		assertNotNull(session);

		try {
			session.init(CONTACT);
		} catch (DrmaaException de) {
			// We should NOT get an exception after the first call
			fail("Exception on first init.");
		}
		
		try {
			// Yet another init() call should result in the exception
			// we are expecting
			session.init(CONTACT);
		} catch (DrmaaException de) {
			throw de;
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test whether a contact of null is allowed when initializing a
	 * session. In this DRMAA implementation, a null is not allowed
	 * and should cause an {@link InvalidContactStringException}.
	 * @throws DrmaaException 
	 */
	@Test(expected=InvalidContactStringException.class)
	public void nullSessionInit() throws DrmaaException {
		Session session = SessionFactory.getFactory().getSession();
		assertNotNull(session);

		// Can we have a null session?
		try {
			session.init(null);
		} catch (DrmaaException e) {
			throw e;
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	
	/**
	 * Test the {@link Session#getContact()} method before session initialization.
	 */
	@Test
	public void getContactBeforeInitTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			
			String contact = session.getContact();
			assertNotNull(contact);
			assertTrue(contact.length() > 0);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}

	/**
	 * Test the {@link Session#getContact()} method after session initialization.
	 * 
	 * @see Session
	 * @see "DRMAA 1.0 specification."
	 */
	@Test
	public void getContactAfterInitTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			
			session.init(CONTACT);
			assertNotNull(session);
			
			String actualContact = session.getContact();
			assertNotNull(actualContact);
			assertTrue(actualContact.length() > 0 );
			assertEquals(CONTACT, actualContact);

		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}

	/**
	 * Test the {@link Session#getDrmSystem()} method.
	 * 
	 * @see Session
	 * @see "DRMAA 1.0 specification."
	 */
	@Test
	public void drmSystemTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			assertNotNull(session);
			
			session.init(CONTACT);
			assertNotNull(session);
			
			String system = session.getDrmSystem();
			assertNotNull(system);
			assertTrue(system.equalsIgnoreCase("condor"));

		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * Test the {@link Session#getDrmaaImplementation()} method.
	 * 
	 * @see Session 
	 * @see "DRMAA 1.0 specification"
	 */
	@Test
	public void drmImplementationTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			assertNotNull(session);

			session.init(CONTACT);
			assertNotNull(session);

			String implementation = session.getDrmaaImplementation();

			assertNotNull(implementation);
			assertTrue(implementation.equalsIgnoreCase("condor"));

		} catch (DrmaaException de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
	
	
	/**
	 * Test the {@link Session#getVersion()} method.
	 * 
	 * @see Session 
	 * @see "DRMAA 1.0 specification"
	 */
	@Test
	public void getVersionTest() {
		Session session = null;
		try {
			session = SessionFactory.getFactory().getSession();
			Object v = session.getVersion();
			
			assertNotNull(v);
			assertTrue(v instanceof Version);
			Version version = (Version) v;
			assertTrue(version.getMajor() >= 0);
			assertTrue(version.getMinor() >= 0);
		} catch (Exception de) {
			de.printStackTrace();
			fail(de.getMessage());
		} finally {
			try {
				if (session != null) {
					session.exit();
					session = null;
				}
			} catch (Exception e) {
				// ignored
			}
		}
	}
}
