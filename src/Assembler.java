import java.io.*;
import java.util.*;


public class Assembler {

    // Variable needed
    private HashMap<String, Integer> labelAddressMap = new HashMap<>();
    private ArrayList<String> sourceLines = new ArrayList<>();
    private int currentAddress = 0;
    private TreeMap<Integer, String> loadFileContent = new TreeMap<>();
    private ArrayList<String> listingFileContent = new ArrayList<>();
    private int lastAddress = 0;

    private InstructionTranslator translator;

    // Call constructor
    public Assembler() {
        this.translator = new InstructionTranslator(labelAddressMap, currentAddress);
    }

    // Store instruction Lines in sourceLines Map
    public void readSourceFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String instructionLine;
            while ((instructionLine = reader.readLine()) != null) {
                sourceLines.add(instructionLine.trim());
            }
        } catch (IOException e) {
            System.out.println("Error in reading file " + e.getMessage());
        }
    }

    // First Pass to store addresses corresponding to labels
    public void firstPass() {
        currentAddress = 0;
        for (String instructionLine : sourceLines) {
            instructionLine = instructionLine.split(";")[0].trim(); // Remove comments
            if (instructionLine.isEmpty()) continue;

            String[] parameters = instructionLine.split("\\s+");
            if (parameters.length == 0) continue;

            if (parameters[0].equals("LOC")) {
                currentAddress = Integer.parseInt(parameters[1]);
                continue;
            }

            if (parameters[0].endsWith(":")) {
                String label = parameters[0].substring(0, parameters[0].length() - 1);
                labelAddressMap.put(label, currentAddress);
                translator.updateLabelAddressMap(label, currentAddress);
                continue;
            }

            currentAddress++;
        }

    }

    // Second Pass to translate instructions to Binary then Octal
    public void secondPass() {
        currentAddress = 0;
        for (String instructionLine : sourceLines) {
            translator.setCurrentAddress(currentAddress);
            String originalLine = instructionLine;
            instructionLine = instructionLine.split(";")[0].trim(); // Remove comments
            if (instructionLine.isEmpty()) {
                listingFileContent.add(originalLine);
                continue;
            }

            String[] parameters = instructionLine.split("\\s+", 2); // Split into 2 parts
            if (parameters.length == 0) {
                listingFileContent.add(originalLine);
                continue;
            }
            // Starting setting location
            if (parameters[0].equals("LOC")) {
                currentAddress = Integer.parseInt(parameters[1]);
                listingFileContent.add(String.format("              %s", originalLine));
                continue;
            }
            // Data instructions
            if (parameters[0].equals("Data")) {
                if (parameters.length < 2) {
                    System.out.println("Data missing " + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Missing value", currentAddress, originalLine));
                    currentAddress++;
                    continue;
                }
                int dataValue = translator.parseData(parameters[1]);
                if (dataValue != -1) {
                    String octalValue = String.format("%06o", dataValue);
//                    System.out.printf("%06o %s\n", currentAddress, octalValue);
                    loadFileContent.put(currentAddress, octalValue);
                    listingFileContent.add(String.format("%06o %s %s", currentAddress, octalValue, originalLine));
                    lastAddress = Math.max(lastAddress, currentAddress);
                } else {
                    System.out.println("Invalid value " + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Undefined label or invalid value", currentAddress, originalLine));
                }
                currentAddress++;
            } else if (parameters[0].endsWith(":")) {
                String label = parameters[0].substring(0, parameters[0].length() - 1);

                // Check if there's an instruction after the label
                if (parameters.length > 1) {
                    String instruction = parameters[1].trim();
                    if (instruction.equals("HLT")) {
                        String hltCode = "000000";
                        loadFileContent.put(currentAddress, hltCode);
                        listingFileContent.add(String.format("%06o %s %s", currentAddress, hltCode, originalLine));
                        currentAddress++;
                    }
                } else {
                    // If  no instruction, add the label
                    listingFileContent.add(String.format("      %s", originalLine));
                }
            }
            // Rest instructions
            else {
                String translatedInstruction = translateInstruction(parameters);
                if (!translatedInstruction.equals("Invalid Instruction")) {
//                    System.out.printf("%06o %s\n", currentAddress, translatedInstruction);
                    loadFileContent.put(currentAddress, translatedInstruction);
                    listingFileContent.add(String.format("%06o %s %s", currentAddress, translatedInstruction, originalLine));
                    lastAddress = Math.max(lastAddress, currentAddress);
                } else {
                    System.out.println("Invalid instruction" + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Invalid instruction", currentAddress, originalLine));
                }
                currentAddress++;
            }
        }
    }

    // Switch case for translate functions
    public String translateInstruction(String[] parameters) {
        if (parameters.length < 2) return "Invalid Instruction";

        String opcode = parameters[0];
        String[] params = parameters[1].split(",");

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
            for (String line : listingFileContent) {
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("writing listing file: " + e.getMessage());
        }
    }

    private void writeLoadFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Map.Entry<Integer, String> entry : loadFileContent.entrySet()) {
                writer.printf("%06o %s\n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("writing load file: " + e.getMessage());
        }
    }
}