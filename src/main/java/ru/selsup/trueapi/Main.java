package ru.selsup.trueapi;

import ru.CryptoPro.reprov.RevCheck;

import java.security.Security;

public class Main {
    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        System.setProperty("ru.CryptoPro.reprov.enableAIAcaIssuers", "true");
        System.setProperty("java.util.prefs.syncInterval", "99999");
        Security.insertProviderAt(new ru.CryptoPro.JCSP.JCSP(), 1);
        Security.addProvider(new RevCheck());
    }

    public static void main(String[] args) {

    }

}