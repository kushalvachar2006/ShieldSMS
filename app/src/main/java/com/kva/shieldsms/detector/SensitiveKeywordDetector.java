package com.kva.shieldsms.detector;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks incoming message text against a preloaded list of words/phrases
 * that commonly appear in sensitive messages (OTPs, bank alerts). This is a
 * simple, transparent keyword match - not machine learning - by design:
 * it's easy to explain, easy to test, and easy to extend.
 */
public class SensitiveKeywordDetector {

    private static final String[] KEYWORDS = {
            "otp",
            "one time password",
            "one-time password",
            "verification code",
            "security code",
            "auth code",
            "authentication code",
            "do not share",
            "don't share this code",
            "login code",
            "confirmation code",
            "account balance",
            "account statement",
            "bank statement",
            "debited",
            "credited",
            "transaction of",
            "card ending",
            "cvv",
            "pin number",
            "password reset",
            "reset your password"
    };

    private final Pattern pattern;

    public SensitiveKeywordDetector() {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (i > 0) regex.append("|");
            regex.append(Pattern.quote(KEYWORDS[i]));
        }
        pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

    public boolean isSensitive(String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) return false;
        Matcher matcher = pattern.matcher(messageBody.toLowerCase(Locale.ROOT));
        return matcher.find();
    }
}
