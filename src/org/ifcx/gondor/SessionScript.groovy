package org.ifcx.gondor

import org.ggf.drmaa.Session
import org.ggf.drmaa.SessionFactory

public abstract class SessionScript extends GondorScript {
    Session session

    def preRun() {
        session = SessionFactory.getFactory().getSession()
        println "prepared"
    }

    def postRun() {
        System.out.println "Done with run and now executing..."
    }

    @Override
    Object run() {
        preRun()
        3.times { runScript() }
        postRun()
    }

    Object getProperty(String name) {
        try {
            super.getProperty(name)
        } catch (MissingPropertyException e) {
            if (!name.startsWith("get")) {
                invokeMethod("get" + name[0].toUpperCase() + name.substring(1), [])
            } else {
                throw e
            }
        }
    }

    abstract Object runScript();
}
