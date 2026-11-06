package org.project.ttokttok.global.util;

public final class LogMasker {

    private static final String MASK = "***";

    private LogMasker() {
    }

    // kim@sangmyung.kr -> k***@sangmyung.kr
    public static String email(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at <= 0) {
            return MASK;
        }
        return email.charAt(0) + MASK + email.substring(at);
    }
}
