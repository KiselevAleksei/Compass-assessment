package com.kiselev;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelParser {

    public static List<Contact> parseExcel(String filePath) {
        List<Contact> contacts = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext())
                rows.next();

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                int contactID = (int) currentRow.getCell(0).getNumericCellValue();
                String firstName = currentRow.getCell(1).getStringCellValue();
                String lastName = currentRow.getCell(2).getStringCellValue();
                String email = currentRow.getCell(3).getStringCellValue();
                Integer postalZip = (int) currentRow.getCell(4).getNumericCellValue();
                String address = currentRow.getCell(5).getStringCellValue();

                contacts.add(new Contact(contactID, firstName, lastName, email, postalZip, address));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return contacts;
    }

    public static List<String> processContacts(List<Contact> contacts) {
        List<String> results = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>(); // Set to track processed pairs

        for (int i = 0; i < contacts.size(); i++) {
            Contact source = contacts.get(i);

            for (int j = i + 1; j < contacts.size(); j++) { // Start at i + 1 to avoid reverse duplicates
                Contact match = contacts.get(j);

                // Ensure consistent order (source < match) by sorting the IDs
                int id1 = Math.min(source.getContactID(), match.getContactID());
                int id2 = Math.max(source.getContactID(), match.getContactID());

                // Create a unique key for the pair
                String pairKey = id1 + "-" + id2;

                // Check if this pair has already been processed
                if (!processedPairs.contains(pairKey)) {
                    processedPairs.add(pairKey); // Mark this pair as processed

                    String accuracy = determineAccuracy(source, match);
                    if (!accuracy.equals("None")) {
                        results.add(String.format("%d; %d; %s", id1, id2, accuracy));
                    }
                }
            }
        }
        return results;

    }

    public static String determineAccuracy(Contact source, Contact match) {

        boolean allMatch = (source.getFirstName().equals(match.getFirstName()) ||
                source.getLastName().equals(match.getLastName()) || source.getEmail().equals(match.getEmail())) &&
                source.getPostalZip() == match.getPostalZip() &&
                source.getAddress().equals(match.getAddress());

        boolean partialMatch = source.getEmail().equals(match.getEmail()) &&
                source.getPostalZip() == match.getPostalZip();

        if (allMatch) {
            return "High";
        } else if (partialMatch) {
            return "Low";
        } else {
            return "None";
        }
    }

    public static void main(String[] args) {
        String excelFilePath1 = "contact_matcher\\src\\main\\resources\\Code Assessment - Find Duplicates Input (2).xlsx";
        String excelFilePath2 = "contact_matcher\\src\\main\\resources\\Code Assessment (test).xlsx";
        List<Contact> contactList = parseExcel(excelFilePath2);

        // Processing the contact data
        List<String> results = processContacts(contactList);
        // Print results
        results.forEach(System.out::println);

    }
}
