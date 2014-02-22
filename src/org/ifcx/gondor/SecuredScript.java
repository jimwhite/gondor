package org.ifcx.gondor;

import groovy.lang.Script;

import java.io.FileDescriptor;

abstract public class SecuredScript extends Script {
    abstract protected Object runScript();

    public Object run() {
        SecurityManager oldSM = System.getSecurityManager();
        try {
            System.setSecurityManager(new NullSecurityManager() {
                @Override
                public void checkRead(FileDescriptor fd) {
                    System.err.println("read fd : " + nameForFileDescriptor(fd));
                    super.checkRead(fd);
                }

                @Override
                public void checkRead(String file) {
                    System.err.println("read fs : " + file);
                    super.checkRead(file);
                }

                @Override
                public void checkRead(String file, Object context) {
                    System.err.println("read fs in ctx : " + file + " ctx " + context);
                    super.checkRead(file, context);
                }

                @Override
                public void checkWrite(FileDescriptor fd) {
                    System.err.println("write fd : " + nameForFileDescriptor(fd));
                    super.checkWrite(fd);
                }

                private String nameForFileDescriptor(FileDescriptor fd) {
                    if (FileDescriptor.out == fd) return "out";
                    if (FileDescriptor.err == fd) return "err";
                    if (FileDescriptor.in == fd) return "in";
                    return "unk";
                }

                @Override
                public void checkWrite(String file) {
                    System.err.println("write fs : " + file);
                    super.checkWrite(file);
                }

                @Override
                public void checkExec(String cmd) {
                    System.err.println("exec : " + cmd);
                    super.checkExec(cmd);
                }

                @Override
                public void checkLink(String lib) {
                    System.err.println("linked : " + lib);
                    super.checkLink(lib);
                }

                @Override
                public void checkDelete(String file) {
                    System.err.println("delete : " + file);
                    super.checkDelete(file);
                }

                @Override
                public void checkPropertiesAccess() {
                    System.err.println("accessed all properties");
                    super.checkPropertiesAccess();
                }

                @Override
                public void checkPropertyAccess(String key) {
                    // Interesting factoid about Apple's JDK.  They hacked ProcessImpl.getEncodedBytes and did it
                    // wrong.  It does getProperty('file.encoding') on *every* call and doesn't deal at all with
                    // access controller.  The correct implementation of that code is in Charset.defaultCharset().

                    System.err.println("access property : " + key);
                    super.checkPropertyAccess(key);
                }
            });

            return runScript();
        } finally {
            System.setSecurityManager(oldSM);
        }
    }
}
