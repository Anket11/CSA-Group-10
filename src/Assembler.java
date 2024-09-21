import java.io.*;
import java.util.*;

public class Assembler {
    // Our trusty variables
    private HashMap<String, Integer> labelAddressMap = new HashMap<>();
    private ArrayList<String> sourceLines = new ArrayList<>();
    private int currentAddress = 0;
    private TreeMap<Integer, String> loadFile = new TreeMap<>();
    private ArrayList<String> listingFile = new ArrayList<>();
    private int lastAddress = 0;

    private InstructionTranslator translator;

    // Fire it up
    public Assembler() {
        this.translator = new InstructionTranslator(labelAddressMap, currentAddress);
    }

    // Grab the source code
    public void readInstructionFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceLines.add(line.trim());
            }
        } catch (IOException oops) {
            System.out.println("Yikes! Couldn't read the file: " + oops.getMessage());
        }
    }

    // First sweep: map out the labels
    public void firstPass() {
        currentAddress = 0;
        for (String line : sourceLines) {
            line = line.split(";")[0].trim(); // Ditch comments
            if (line.isEmpty()) continue;

            String[] bits = line.split("\\s+");
            if (bits.length == 0) continue;

            if (bits[0].equals("LOC")) {
                currentAddress = Integer.parseInt(bits[1]);
                continue;
            }

            if (bits[0].endsWith(":")) {
                String label = bits[0].substring(0, bits[0].length() - 1);
                labelAddressMap.put(label, currentAddress);
                translator.updateLabelAddressMap(label, currentAddress);
                continue;
            }

            currentAddress++;
        }
    }

    // Second sweep: translate to machine code
    public void secondPass() {
        currentAddress = 0;
        for (String line : sourceLines) {
            translator.setCurrentAddress(currentAddress);
            String originalLine = line;
            line = line.split(";")[0].trim(); // Ditch comments
            if (line.isEmpty()) {
                listingFile.add(originalLine);
                continue;
            }

            String[] bits = line.split("\\s+", 2); // Split into 2 parts
            if (bits.length == 0) {
                listingFile.add(originalLine);
                continue;
            }

            // Handle different instruction types
            if (bits[0].equals("LOC")) {
                handleLOC(bits, originalLine);
            } else if (bits[0].equals("Data")) {
                handleData(bits, originalLine);
            } else if (bits[0].endsWith(":")) {
                handleLabel(bits, originalLine);
            } else {
                handleRegularInstruction(bits, originalLine);
            }
        }
    }

    private void handleLOC(String[] bits, String originalLine) {
        currentAddress = Integer.parseInt(bits[1]);
        listingFile.add(String.format("              %s", originalLine));
    }

    private void handleData(String[] bits, String originalLine) {
        if (bits.length < 2) {
            System.out.println("Oops, data's missing at " + currentAddress);
            listingFile.add(String.format("%06o       %s ; Where's the value?", currentAddress, originalLine));
            currentAddress++;
            return;
        }
        int dataValue = translator.parseData(bits[1]);
        if (dataValue != -1) {
            String octalValue = String.format("%06o", dataValue);
            loadFile.put(currentAddress, octalValue);
            listingFile.add(String.format("%06o %s %s", currentAddress, octalValue, originalLine));
            lastAddress = Math.max(lastAddress, currentAddress);
        } else {
            System.out.println("That value doesn't look right at " + currentAddress);
            listingFile.add(String.format("%06o       %s ; Undefined label or wonky value", currentAddress, originalLine));
        }
        currentAddress++;
    }

    private void handleLabel(String[] bits, String originalLine) {
        if (bits.length > 1) {
            String instruction = bits[1].trim();
            if (instruction.equals("HLT")) {
                String hltCode = "000000";
                loadFile.put(currentAddress, hltCode);
                listingFile.add(String.format("%06o %s %s", currentAddress, hltCode, originalLine));
                currentAddress++;
            }
        } else {
            listingFile.add(String.format("      %s", originalLine));
        }
    }

    private void handleRegularInstruction(String[] bits, String originalLine) {
        String translatedInstruction = translateInstruction(bits);
        if (!translatedInstruction.equals("Invalid Instruction")) {
            loadFile.put(currentAddress, translatedInstruction);
            listingFile.add(String.format("%06o %s %s", currentAddress, translatedInstruction, originalLine));
            lastAddress = Math.max(lastAddress, currentAddress);
        } else {
            System.out.println("Huh? Invalid instruction at " + currentAddress);
            listingFile.add(String.format("%06o       %s ; ERROR: What's this instruction?", currentAddress, originalLine));
        }
        currentAddress++;
    }

    // Figure out what we're dealing with
    public String translateInstruction(String[] bits) {
        if (bits.length < 2) return "Invalid Instruction";

        String opcode = bits[0];
        String[] params = bits[1].split(",");

        return switch (opcode) {
            case "LDR" -> translator.translateLDR(params);
            case "STR" -> translator.translateSTR(params);
            case "LDA" -> translator.translateLDA(params);
            case "LDX" -> translator.translateLDX(params);
            case "STX" -> translator.translateSTX(params);
            case "HLT" -> translator.translateHLT();
            case "JZ" -> translator.translateJZ(params);
            default -> "Invalid Instruction";
        };
    }

    public void writeFiles(String listingFilename, String loadFilename) {
        writeListingFile(listingFilename);
        writeLoadFile(loadFilename);
    }

    private void writeListingFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String line : listingFile) {
                writer.println(line);
            }
        } catch (IOException oops) {
            System.out.println("Uh-oh, trouble writing listing file: " + oops.getMessage());
        }
    }

    private void writeLoadFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Map.Entry<Integer, String> entry : loadFile.entrySet()) {
                writer.printf("%06o %s\n", entry.getKey(), entry.getValue());
            }
        } catch (IOException oops) {
            System.out.println("Rats, can't write load file: " + oops.getMessage());
        }
    }
}