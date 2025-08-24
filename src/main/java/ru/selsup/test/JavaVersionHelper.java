package ru.selsup.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaVersionHelper {
    public static final Logger logger = LoggerFactory.getLogger(JavaVersionHelper.class);

    public static int getVersion() {
        String version = System.getProperty("java.version");
        logger.info("running in JVM {}", version);
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }
}
