package com.carddemo.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Replaces CSLKPCDY.cpy area code / state / zip lookup table.
 * Loads the 1,318-line lookup data into Maps for validation.
 */
public final class LookupService {

    private static final Set<String> PHONE_AREA_CODES;
    private static final Set<String> US_STATE_CODES;
    private static final Set<String> US_STATE_ZIP_PREFIXES;
    private static final Map<String, String> AREA_CODE_TO_STATE;

    static {
        PHONE_AREA_CODES = loadLines("lookup/phone-area-codes.txt");
        US_STATE_CODES = loadLines("lookup/us-state-codes.txt");
        US_STATE_ZIP_PREFIXES = loadLines("lookup/us-state-zip-codes.txt");
        AREA_CODE_TO_STATE = buildAreaCodeStateMap();
    }

    private LookupService() {
    }

    public static boolean isValidPhoneAreaCode(String areaCode) {
        return areaCode != null && PHONE_AREA_CODES.contains(areaCode);
    }

    public static boolean isValidUsStateCode(String stateCode) {
        return stateCode != null && US_STATE_CODES.contains(stateCode.trim().toUpperCase());
    }

    public static boolean isValidStateZipPrefix(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return false;
        }
        String key = stateCode.trim().toUpperCase() + zipPrefix.substring(0, Math.min(2, zipPrefix.length()));
        return US_STATE_ZIP_PREFIXES.contains(key);
    }

    public static Set<String> getPhoneAreaCodes() {
        return Collections.unmodifiableSet(PHONE_AREA_CODES);
    }

    public static Set<String> getUsStateCodes() {
        return Collections.unmodifiableSet(US_STATE_CODES);
    }

    /**
     * Returns the state associated with a given area code, or null if not found.
     */
    public static String getStateForAreaCode(String areaCode) {
        return AREA_CODE_TO_STATE.get(areaCode);
    }

    private static Set<String> loadLines(String resourcePath) {
        try (InputStream is = LookupService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load lookup resource: " + resourcePath, e);
        }
    }

    private static Map<String, String> buildAreaCodeStateMap() {
        Map<String, String> map = new HashMap<>();
        for (String stateZip : US_STATE_ZIP_PREFIXES) {
            if (stateZip.length() >= 4) {
                String state = stateZip.substring(0, 2);
                map.putIfAbsent(stateZip.substring(2), state);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
