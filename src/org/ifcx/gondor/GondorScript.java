package org.ifcx.gondor;

import com.beust.jcommander.JCommander;
import groovy.lang.Binding;
import groovy.transform.InheritConstructors;
import groovyx.cli.Default;
import groovyx.cli.JCommanderScript;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

//@InheritConstructors
public abstract class GondorScript extends JCommanderScript {
// Can't just use @InheritConstructors for brevity and general future-proofing because of AST transform ordering.
    public GondorScript() { this(new Binding()); }
    public GondorScript(Binding context) { super(context); }

//    @Override
//    public void parseScriptArguments(JCommander jc, String[] args) {
//        super.parseScriptArguments(jc, args);
//        runInitializers();
//    }
//
//    public void runInitializers() {
//        Class cls = this.getClass();
//        while (cls != null) {
//            Field[] fields = cls.getDeclaredFields();
//            for (Field field : fields) {
//                Annotation annotation = field.getAnnotation(Default.class);
//                if (annotation != null) {
//                    try {
//                        field.setAccessible(true);
//                        Class<Callable> klazz = ((Default) annotation).value();
//                        Constructor<Callable> constructor = klazz.getConstructor(Object.class, Object.class);
//                        Callable initializer = constructor.newInstance(this, this);
//                        Object value = initializer.call();
//                        field.set(this, value);
//                    } catch (Exception e) {
//                        printErrorMessage("Trying to run GondorScript initializers but got exception '" + e
//                                + "' when getting value of field " + field.getName());
//                    }
//                }
//            }
//
//            cls = cls.getSuperclass();
//        }
//    }

}
