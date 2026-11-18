package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.data.VersionInfo;

import org.junit.Test;

// Mise à jour automatique : lecture de apk/version.json et comparaison des versions
public class UpdateCheckerTest {

    @Test
    public void parse_readsCodeAndName() {
        VersionInfo v = VersionInfo.parse("{\"versionCode\": 4, \"versionName\": \"2.2\"}");
        assertNotNull(v);
        assertEquals(4, v.versionCode);
        assertEquals("2.2", v.versionName);
    }

    @Test
    public void parse_toleratesSpacesOrderAndNewlines() {
        VersionInfo v = VersionInfo.parse("{\n  \"versionName\" : \"3.0\",\n  \"versionCode\":12\n}\n");
        assertNotNull(v);
        assertEquals(12, v.versionCode);
        assertEquals("3.0", v.versionName);
    }

    @Test
    public void parse_missingNameUsesCode() {
        VersionInfo v = VersionInfo.parse("{\"versionCode\": 7}");
        assertNotNull(v);
        assertEquals("7", v.versionName);
    }

    @Test
    public void parse_rejectsBrokenFiles() {
        assertNull(VersionInfo.parse(null));
        assertNull(VersionInfo.parse(""));
        assertNull(VersionInfo.parse("<html>404</html>"));
        assertNull(VersionInfo.parse("{\"versionName\": \"2.2\"}"));
        assertNull(VersionInfo.parse("{\"versionCode\": 99999999999}"));
    }

    @Test
    public void isNewerThan_onlyWhenCodeIsHigher() {
        VersionInfo v = new VersionInfo(4, "2.2");
        assertTrue(v.isNewerThan(3));
        assertFalse(v.isNewerThan(4));
        assertFalse(v.isNewerThan(5));
    }
}
