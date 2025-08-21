package ru.selsup.test;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("Hello, World!");

        try {
            Config config = ConfigLoader.loadDefaultConfig();
            CrptApi api = new CrptApi(config);
            api.authenticate("user","pass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}