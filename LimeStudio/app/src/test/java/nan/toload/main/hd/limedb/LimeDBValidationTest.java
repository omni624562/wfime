/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package nan.toload.main.hd.limedb;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Unit tests for LimeDB table name validation logic.
 *
 * Because isValidTableName() and validateTableName() are private static methods,
 * we use reflection to invoke them directly. This allows us to verify the
 * security-critical SQL injection prevention logic without requiring Android context.
 *
 * 注意：isValidTableName 在無效輸入時拋出 IllegalArgumentException（而非回傳 false），
 * 因此 validateTableName 中的 if (!isValidTableName(...)) 分支邏輯上永遠不可到達。
 * 以下測試同時確認了這個設計行為，以及每個邊界條件的正確拋出。
 */
public class LimeDBValidationTest {

    // -------------------------------------------------------------------------
    // Helper: invoke private static isValidTableName via reflection
    // -------------------------------------------------------------------------

    /**
     * Calls the private static isValidTableName(String) method via reflection.
     *
     * @return true if validation passes
     * @throws IllegalArgumentException (unwrapped) if validation fails
     * @throws RuntimeException for unexpected reflection errors
     */
    private boolean callIsValidTableName(String tableName) {
        try {
            Method method = LimeDB.class.getDeclaredMethod("isValidTableName", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, tableName);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw new RuntimeException("Unexpected exception: " + cause, cause);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Reflection error: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the private static validateTableName(String) method via reflection.
     *
     * @return the cleaned (trimmed, lowercased) table name if valid
     * @throws IllegalArgumentException (unwrapped) if validation fails
     */
    private String callValidateTableName(String tableName) {
        try {
            Method method = LimeDB.class.getDeclaredMethod("validateTableName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, tableName);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw new RuntimeException("Unexpected exception: " + cause, cause);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Reflection error: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // isValidTableName — null / empty inputs
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_NullInput_ThrowsException() {
        callIsValidTableName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_EmptyString_ThrowsException() {
        callIsValidTableName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_WhitespaceOnly_ThrowsException() {
        callIsValidTableName("   ");
    }

    // =========================================================================
    // isValidTableName — whitelist hits (should return true)
    // =========================================================================

    @Test
    public void testIsValid_Dayi_ReturnTrue() {
        assertTrue(callIsValidTableName("dayi"));
    }

    @Test
    public void testIsValid_Phonetic_ReturnTrue() {
        assertTrue(callIsValidTableName("phonetic"));
    }

    @Test
    public void testIsValid_Custom_ReturnTrue() {
        assertTrue(callIsValidTableName("custom"));
    }

    @Test
    public void testIsValid_CJ_ReturnTrue() {
        assertTrue(callIsValidTableName("cj"));
    }

    @Test
    public void testIsValid_Array_ReturnTrue() {
        assertTrue(callIsValidTableName("array"));
    }

    // =========================================================================
    // isValidTableName — case insensitive (uppercased inputs still valid)
    // =========================================================================

    @Test
    public void testIsValid_DayiUppercase_ReturnTrue() {
        // Input "DAYI" should be lowercased internally → hits whitelist
        assertTrue(callIsValidTableName("DAYI"));
    }

    @Test
    public void testIsValid_PhoneticMixedCase_ReturnTrue() {
        assertTrue(callIsValidTableName("Phonetic"));
    }

    // =========================================================================
    // isValidTableName — _user suffix pattern
    // =========================================================================

    @Test
    public void testIsValid_DayiUser_ReturnTrue() {
        assertTrue(callIsValidTableName("dayi_user"));
    }

    @Test
    public void testIsValid_PhoneticUser_ReturnTrue() {
        assertTrue(callIsValidTableName("phonetic_user"));
    }

    @Test
    public void testIsValid_CustomUser_ReturnTrue() {
        assertTrue(callIsValidTableName("custom_user"));
    }

    // =========================================================================
    // isValidTableName — SQL injection attempts (must throw)
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_SqlInjectionDropTable_ThrowsException() {
        // Classic SQL injection: terminate statement and drop table
        callIsValidTableName("dayi; DROP TABLE dayi");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_SqlInjectionSingleQuote_ThrowsException() {
        callIsValidTableName("dayi'--");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_SqlInjectionUnionSelect_ThrowsException() {
        callIsValidTableName("dayi UNION SELECT * FROM sqlite_master");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_SqlInjectionSemicolon_ThrowsException() {
        callIsValidTableName("valid;invalid");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_HasSpace_ThrowsException() {
        callIsValidTableName("table name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_StartsWithDigit_ThrowsException() {
        // Pattern requires starts with [a-z]
        callIsValidTableName("1dayi");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_StartsWithUnderscore_ThrowsException() {
        callIsValidTableName("_dayi");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_ContainsDash_ThrowsException() {
        callIsValidTableName("my-table");
    }

    // =========================================================================
    // isValidTableName — unknown but pattern-valid name (returns true with warning)
    // =========================================================================

    @Test
    public void testIsValid_WhitelistName_ShortestValid_ReturnTrue() {
        // "cj" is the shortest whitelist entry
        assertTrue(callIsValidTableName("cj"));
    }

    @Test
    public void testIsValid_LongWhitelistName_ReturnTrue() {
        // "phonetic" is the longest whitelist entry (8 chars)
        assertTrue(callIsValidTableName("phonetic"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValid_TooLongName_ThrowsException() {
        // 65 chars total — exceeds max of 64
        String tooLong = "a" + "b".repeat(64);
        assertEquals(65, tooLong.length());
        callIsValidTableName(tooLong);
    }

    // =========================================================================
    // validateTableName — should return cleaned (lowercased, trimmed) name
    // =========================================================================

    @Test
    public void testValidate_Dayi_ReturnsLowercased() {
        assertEquals("dayi", callValidateTableName("dayi"));
    }

    @Test
    public void testValidate_DayiUppercase_ReturnsLowercased() {
        assertEquals("dayi", callValidateTableName("DAYI"));
    }

    @Test
    public void testValidate_DayiWithSpaces_ReturnsTrimmed() {
        assertEquals("dayi", callValidateTableName("  dayi  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_NullInput_ThrowsException() {
        callValidateTableName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_SqlInjection_ThrowsException() {
        callValidateTableName("dayi; DROP TABLE dayi");
    }

    // =========================================================================
    // Design verification: isValidTableName never returns false (always throws)
    //
    // This test documents the current design: the method either returns true
    // OR throws IllegalArgumentException. It never returns false.
    // Therefore validateTableName's `if (!isValidTableName(...))` branch is
    // unreachable dead code.
    // =========================================================================

    @Test
    public void testDesignDoc_IsValidTableName_NeverReturnsFalse() {
        // Valid input: returns true
        try {
            boolean result = callIsValidTableName("dayi");
            assertTrue("Should return true for valid input", result);
        } catch (IllegalArgumentException e) {
            fail("Should not throw for valid 'dayi'");
        }

        // Invalid input: throws, never returns false
        boolean caughtException = false;
        try {
            callIsValidTableName("dayi; DROP TABLE");
        } catch (IllegalArgumentException e) {
            caughtException = true;
        }
        assertTrue("Invalid input must throw, not return false", caughtException);
    }
}
