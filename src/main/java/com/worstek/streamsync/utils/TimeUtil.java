package com.worstek.streamsync.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*([smhdSMHD])");

    public static long parseTimeSeconds(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }

        String raw = input.trim();

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {}

        Matcher matcher = TIME_PATTERN.matcher(raw);
        long totalSeconds = 0;
        boolean foundAny = false;

        while (matcher.find()) {
            foundAny = true;
            long val = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).toLowerCase().charAt(0);

            switch (unit) {
                case 's':
                    totalSeconds += val;
                    break;
                case 'm':
                    totalSeconds += val * 60L;
                    break;
                case 'h':
                    totalSeconds += val * 3600L;
                    break;
                case 'd':
                    totalSeconds += val * 86400L;
                    break;
            }
        }

        if (foundAny) {
            return totalSeconds;
        }

        String digitsOnly = raw.replaceAll("\\D+", "");
        if (!digitsOnly.isEmpty()) {
            try {
                return Long.parseLong(digitsOnly);
            } catch (NumberFormatException ignored) {}
        }

        return 0;
    }
}