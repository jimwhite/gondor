package org.ifcx.gondor;

import groovy.lang.Script;

import java.io.FileDescriptor;

abstract public class SecuredScript extends Script {
    abstract protected Object runScript();

    public Object run() {
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
        });

        return runScript();
    }
}
