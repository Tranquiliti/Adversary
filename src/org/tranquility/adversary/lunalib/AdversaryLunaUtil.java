package org.tranquility.adversary.lunalib;

import lunalib.lunaSettings.LunaSettings;

// Utility class to avoid a Java bug with importing LunaLib classes in an extended BaseModPlugin,
// causing the LunaLib soft dependency to become a hard dependency
public final class AdversaryLunaUtil {
    public static void addSettingsListener() {
        LunaSettings.addSettingsListener(new AdversaryLunaSettingsListener());
    }

    public static Boolean getBoolean(String modId, String fieldId) {
        return LunaSettings.getBoolean(modId, fieldId);
    }

    public static Integer getInt(String modId, String fieldId) {
        return LunaSettings.getInt(modId, fieldId);
    }
}