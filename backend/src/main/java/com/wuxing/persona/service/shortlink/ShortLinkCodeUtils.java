package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import java.util.regex.Pattern;

public final class ShortLinkCodeUtils {

    static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final int CODE_LENGTH = 6;
    static final int MAX_RETRY = 5;
    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[0-9a-zA-Z]{6,7}$");

    private ShortLinkCodeUtils() {
    }

    public static void validate(String shortCode) {
        if (shortCode == null || !SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
            throw new BusinessException("shortCode must be base62 and length 6 or 7");
        }
    }

    static String extractFromFullShortUrl(String fullShortUrl) {
        if (fullShortUrl == null || fullShortUrl.isBlank()) {
            throw new BusinessException("external short link response missing fullShortUrl");
        }
        String value = fullShortUrl.trim();
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int fragmentIndex = value.indexOf('#');
        if (fragmentIndex >= 0) {
            value = value.substring(0, fragmentIndex);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        int lastSlashIndex = value.lastIndexOf('/');
        String shortCode = lastSlashIndex >= 0 ? value.substring(lastSlashIndex + 1) : value;
        validate(shortCode);
        return shortCode;
    }
}
