package utilities;

import models.RegisterRequest;

import java.util.concurrent.atomic.AtomicInteger;

//Unique values per run, so tests never collide with each other or with earlier runs
public final class TestData {

    private static final String RUN_ID = Long.toString(System.currentTimeMillis(), 36);
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static final String PASSWORD = "Passw0rd!";
    public static final String PHONE = "0871234567";

    private TestData() {
    }

    public static String unique(String prefix) {
        return prefix + "-" + RUN_ID + "-" + COUNTER.incrementAndGet();
    }

    public static String uniqueEmail() {
        return "qa+" + RUN_ID + "-" + COUNTER.incrementAndGet() + "@example.test";
    }

    public static RegisterRequest newRegisterRequest() {
        return new RegisterRequest("QA " + unique("customer"), uniqueEmail(), PHONE, PASSWORD);
    }

    public static String repeat(char character, int length) {
        return String.valueOf(character).repeat(length);
    }
}
