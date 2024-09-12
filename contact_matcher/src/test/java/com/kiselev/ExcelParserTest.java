package com.kiselev;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ExcelParserTest {

    // @Test
    // public void testParseExcel() {
    // // Path to test Excel file
    // String testFilePath = "contact_matcher\\src\\main\\resources\\test.xlsx";
    // List<Contact> contacts = ExcelParser.parseExcel(testFilePath);

    // // Example assertions
    // assertNotNull(contacts, "Contacts list should not be null");
    // assertFalse(contacts.isEmpty(), "Contacts list should not be empty");
    // }

    @Test
    public void testDetermineAccuracy() {
        Contact contact1 = new Contact(1, "John", "Doe", "john@example.com", 12345, "123 Street");
        Contact contact2 = new Contact(2, "John", "Doe", "john@example.com", 12345, "123 Street");

        String accuracy = ExcelParser.determineAccuracy(contact1, contact2);
        assertEquals("High", accuracy, "Contacts should be a High match");

        Contact contact3 = new Contact(3, "Jane", "Smith", "jane@example.com", 67890, "456 Avenue");
        String noAccuracy = ExcelParser.determineAccuracy(contact1, contact3);
        assertEquals("None", noAccuracy, "Contacts should have no match");
    }

    @Test
    public void testProcessContacts() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact(1, "John", "Doe", "john@example.com", 12345, "123 Street"));
        contacts.add(new Contact(2, "John", "Doe", "john@example.com", 12345, "123 Street"));
        contacts.add(new Contact(3, "Jane", "Smith", "jane@example.com", 67890, "456 Avenue"));

        // Call the processContacts method and capture the result
        List<String> results = ExcelParser.processContacts(contacts);

        // Verify that the results contain the expected matches
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.size(), "There should be 1 match");

        // Check the content of the match
        assertEquals("1; 2; High", results.get(0), "The match between contact 1 and 2 should be 'High'");
    }
}
