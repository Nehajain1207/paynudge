package com.neha.paynudge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PaynudgeApplication {

    public static void main(String[] args) {
        // Windows reports India's zone under its old name "Asia/Calcutta", which PostgreSQL 17 rejects.
        // The business runs on India time, so set the modern name before anything connects to the database.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(PaynudgeApplication.class, args);
    }
}