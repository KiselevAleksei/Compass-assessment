package com.kiselev;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelParser {

    private final static String EXCEL_FILE_PATH = "contact_matcher\\src\\main\\resources\\Code Assessment - Find Duplicates Input (2).xlsx";
    private final static String RESULT_FILE_PATH = "contact_matcher\\src\\main\\resources\\results.txt";

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

    public static List<String> processContactsByHashMap(List<Contact> contacts) {
        List<String> results = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>(); // To track processed pairs

        // Create HashMaps to group contacts by fields
        Map<String, List<Contact>> firstNameIndex = new HashMap<>();
        Map<String, List<Contact>> lastNameIndex = new HashMap<>();
        Map<String, List<Contact>> emailIndex = new HashMap<>();
        Map<Integer, List<Contact>> postalZipIndex = new HashMap<>();
        Map<String, List<Contact>> addressIndex = new HashMap<>();

        // Populate the indices
        for (Contact contact : contacts) {
            firstNameIndex.computeIfAbsent(contact.getFirstName(), k -> new ArrayList<>()).add(contact);
            lastNameIndex.computeIfAbsent(contact.getLastName(), k -> new ArrayList<>()).add(contact);
            emailIndex.computeIfAbsent(contact.getEmail(), k -> new ArrayList<>()).add(contact);
            postalZipIndex.computeIfAbsent(contact.getPostalZip(), k -> new ArrayList<>()).add(contact);
            addressIndex.computeIfAbsent(contact.getAddress(), k -> new ArrayList<>()).add(contact);
        }

        // Process each contact
        for (Contact source : contacts) {
            Set<Contact> potentialMatches = new HashSet<>();

            // Add potential matches by looking up in each index
            potentialMatches.addAll(firstNameIndex.getOrDefault(source.getFirstName(), Collections.emptyList()));
            potentialMatches.addAll(lastNameIndex.getOrDefault(source.getLastName(), Collections.emptyList()));
            potentialMatches.addAll(emailIndex.getOrDefault(source.getEmail(), Collections.emptyList()));
            potentialMatches.addAll(postalZipIndex.getOrDefault(source.getPostalZip(), Collections.emptyList()));
            potentialMatches.addAll(addressIndex.getOrDefault(source.getAddress(), Collections.emptyList()));

            for (Contact match : potentialMatches) {
                if (source.getContactID() != match.getContactID()) {
                    // Ensure consistent order (source < match)
                    int id1 = Math.min(source.getContactID(), match.getContactID());
                    int id2 = Math.max(source.getContactID(), match.getContactID());

                    String pairKey = id1 + "-" + id2;

                    // Only process if pair has not been processed
                    if (!processedPairs.contains(pairKey)) {
                        processedPairs.add(pairKey); // Mark pair as processed

                        String accuracy = determineAccuracy(source, match);
                        if (!accuracy.equals("None")) {
                            results.add(String.format("%d; %d; %s", id1, id2, accuracy));
                        }
                    }
                }
            }
        }

        return results;
    }

    public static List<String> processContactsByHashMapConcurrent(List<Contact> contacts) {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        Set<String> processedPairs = Collections.synchronizedSet(new HashSet<>()); // To track processed pairs

        // Create HashMaps to group contacts by fields
        Map<String, List<Contact>> firstNameIndex = new HashMap<>();
        Map<String, List<Contact>> lastNameIndex = new HashMap<>();
        Map<String, List<Contact>> emailIndex = new HashMap<>();
        Map<Integer, List<Contact>> postalZipIndex = new HashMap<>();
        Map<String, List<Contact>> addressIndex = new HashMap<>();

        // Populate the indices
        for (Contact contact : contacts) {
            firstNameIndex.computeIfAbsent(contact.getFirstName(), k -> new ArrayList<>()).add(contact);
            lastNameIndex.computeIfAbsent(contact.getLastName(), k -> new ArrayList<>()).add(contact);
            emailIndex.computeIfAbsent(contact.getEmail(), k -> new ArrayList<>()).add(contact);
            postalZipIndex.computeIfAbsent(contact.getPostalZip(), k -> new ArrayList<>()).add(contact);
            addressIndex.computeIfAbsent(contact.getAddress(), k -> new ArrayList<>()).add(contact);
        }

        // Create a thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        // Process each contact in parallel
        for (Contact source : contacts) {
            futures.add(executor.submit(() -> {
                Set<Contact> potentialMatches = new HashSet<>();

                // Add potential matches by looking up in each index
                potentialMatches.addAll(firstNameIndex.getOrDefault(source.getFirstName(), Collections.emptyList()));
                potentialMatches.addAll(lastNameIndex.getOrDefault(source.getLastName(), Collections.emptyList()));
                potentialMatches.addAll(emailIndex.getOrDefault(source.getEmail(), Collections.emptyList()));
                potentialMatches.addAll(postalZipIndex.getOrDefault(source.getPostalZip(), Collections.emptyList()));
                potentialMatches.addAll(addressIndex.getOrDefault(source.getAddress(), Collections.emptyList()));

                // Check for matches
                for (Contact match : potentialMatches) {
                    if (source.getContactID() != match.getContactID()) {
                        // Ensure consistent order (source < match)
                        int id1 = Math.min(source.getContactID(), match.getContactID());
                        int id2 = Math.max(source.getContactID(), match.getContactID());

                        String pairKey = id1 + "-" + id2;

                        // Only process if pair has not been processed
                        if (!processedPairs.contains(pairKey)) {
                            processedPairs.add(pairKey); // Mark pair as processed

                            String accuracy = determineAccuracy(source, match);
                            if (!accuracy.equals("None")) {
                                results.add(String.format("%d; %d; %s", id1, id2, accuracy));
                            }
                        }
                    }
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Shutdown the executor
        executor.shutdown();

        return results;
    }

    public static String determineAccuracy(Contact source, Contact match) {
        /*
         * Using string similarity algorithm (Jaro-Winkler)
         * can be used to measure how similar two strings are, even if they don't match
         * exactly.
         */
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

        // Define weights for each field (can be tuned based on importance)
        double firstNameWeight = 0.2;
        double lastNameWeight = 0.2;
        double emailWeight = 0.3;
        double postalZipWeight = 0.1;
        double addressWeight = 0.2;

        // Compute string similarities for each field
        double firstNameSim = similarity.apply(source.getFirstName(), match.getFirstName());
        double lastNameSim = similarity.apply(source.getLastName(), match.getLastName());
        double emailSim = similarity.apply(source.getEmail(), match.getEmail());

        // Numeric match for postal zip
        boolean postalZipMatch = source.getPostalZip() == (match.getPostalZip());

        // String similarity for address
        double addressSim = similarity.apply(source.getAddress(), match.getAddress());

        // Calculate weighted score
        double similarityScore = (firstNameSim * firstNameWeight) +
                (lastNameSim * lastNameWeight) +
                (emailSim * emailWeight) +
                (postalZipMatch ? postalZipWeight : 0) +
                (addressSim * addressWeight);

        // Determine accuracy based on score thresholds
        if (similarityScore >= 0.7) {
            return "High";
        } else if (similarityScore >= 0.2) {
            return "Low";
        } else {
            return "None";
        }
    }

    public static void writeResultsToFile(List<String> results) {
        // Write results to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESULT_FILE_PATH))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
            }
        }

        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Results written to: " + RESULT_FILE_PATH);
    }

    public static void main(String[] args) {
        List<Contact> contactList = parseExcel(EXCEL_FILE_PATH);

        // Processing the contact data
        long startTime = System.currentTimeMillis();
        // List<String> results = processContactsByListLoop(contactList);
        List<String> results = processContactsByHashMap(contactList);
        long stopTime = System.currentTimeMillis();
        System.out.println(">>> Elapsed time: " + (stopTime - startTime) + " millseconds");

        startTime = System.currentTimeMillis();
        List<String> resultsConcurent = processContactsByHashMapConcurrent(contactList);
        stopTime = System.currentTimeMillis();

        // print algorithm running time
        System.out.println(">>> Elapsed time concurrent: " + (stopTime - startTime) + " millseconds");

        // Print results
        writeResultsToFile(results);

    }
}
