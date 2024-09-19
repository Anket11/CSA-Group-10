import java.io.*;
import java.util.*;

public class Assembler {
    private HashMap<String, Integer> labelAddressMap = new HashMap<>();
    private ArrayList<String> sourceLines = new ArrayList<>();
    private int currentAddress = 0;
    private TreeMap<Integer, String> loadFileContent = new TreeMap<>();
    private ArrayList<String> listingFileContent = new ArrayList<>();
    private int lastAddress = 0;

    public void readSourceFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceLines.add(line.trim());
            }
        } catch (IOException e) {
            System.out.println("Error reading source file: " + e.getMessage());
        }
    }

    public void firstPass() {
        currentAddress = 0;
        for (String line : sourceLines) {
            line = line.split(";")[0].trim(); // Remove comments
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            if (tokens.length == 0) continue;

            if (tokens[0].equals("LOC")) {
                currentAddress = Integer.parseInt(tokens[1]);
                continue;
            }

            if (tokens[0].endsWith(":")) {
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                labelAddressMap.put(label, currentAddress);
                continue;
            }

            currentAddress++;
        }
    }

    public void secondPass() {
        currentAddress = 0;
        for (String line : sourceLines) {
            String originalLine = line;
            line = line.split(";")[0].trim(); // Remove comments
            if (line.isEmpty()) {
                listingFileContent.add(originalLine);
                continue;
            }

            String[] tokens = line.split("\\s+", 2); // Split into at most 2 parts
            if (tokens.length == 0) {
                listingFileContent.add(originalLine);
                continue;
            }

            if (tokens[0].equals("LOC")) {
                currentAddress = Integer.parseInt(tokens[1]);
                listingFileContent.add(String.format("              %s", originalLine));
                continue;
            }

            if (tokens[0].equals("Data")) {
                if (tokens.length < 2) {
                    System.out.println("Error: Data instruction is missing value at address " + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Missing value", currentAddress, originalLine));
                    currentAddress++;
                    continue;
                }
                int dataValue = parseData(tokens[1]);
                if (dataValue != -1) {
                    String octalValue = String.format("%06o", dataValue);
                    System.out.printf("%06o %s\n", currentAddress, octalValue);
                    loadFileContent.put(currentAddress, octalValue);
                    listingFileContent.add(String.format("%06o %s %s", currentAddress, octalValue, originalLine));
                    lastAddress = Math.max(lastAddress, currentAddress);
                } else {
                    System.out.println("Error: Undefined label or invalid data value at address " + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Undefined label or invalid value", currentAddress, originalLine));
                }
                currentAddress++;
            } else if (tokens[0].endsWith(":")) {
                // This is a label
                String label = tokens[0].substring(0, tokens[0].length() - 1);

                // Check if there's an instruction after the label
                if (tokens.length > 1) {
                    String instruction = tokens[1].trim();
                    if (instruction.equals("HLT")) {
                        String hltCode = "000000";
                        loadFileContent.put(currentAddress, hltCode);
                        listingFileContent.add(String.format("%06o %s %s", currentAddress, hltCode, originalLine));
                        currentAddress++;
                    }
                } else {
                    // If there's no instruction after the label, just add the label line
                    listingFileContent.add(String.format("      %s", originalLine));
                }
            } else {
                String translatedInstruction = translateInstruction(tokens);
                if (!translatedInstruction.equals("Invalid Instruction")) {
                    System.out.printf("%06o %s\n", currentAddress, translatedInstruction);
                    loadFileContent.put(currentAddress, translatedInstruction);
                    listingFileContent.add(String.format("%06o %s %s", currentAddress, translatedInstruction, originalLine));
                    lastAddress = Math.max(lastAddress, currentAddress);
                } else {
                    System.out.println("Error: Invalid instruction at address " + currentAddress);
                    listingFileContent.add(String.format("%06o       %s ; ERROR: Invalid instruction", currentAddress, originalLine));
                }
                currentAddress++;
            }
        }
    }
    public String translateInstruction(String[] tokens) {
        if (tokens.length < 2) return "Invalid Instruction";

        String opcode = tokens[0];
        String[] params = tokens[1].split(",");

        return switch (opcode) {
            case "LDR" -> translateLDR(params);
            case "STR" -> translateSTR(params);
            case "LDA" -> translateLDA(params);
            case "LDX" -> translateLDX(params);
            case "STX" -> translateSTX(params);
            case "HLT" -> translateHLT();
            case "JZ" -> translateJZ(params);
            default -> "Invalid Instruction";
        };
    }

    public String translateLDR(String[] params) {
        if (params.length < 3) {
            System.out.println("Error: LDR instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "000001";
            int r = Integer.parseInt(params[0].trim());
            int x = Integer.parseInt(params[1].trim());
            if (r < 0 || r > 3 || x < 0 || x > 3) {
                throw new IllegalArgumentException("Invalid register or index value");
            }
            String rBinary = toBinary(r, 2);
            String ix = toBinary(x, 2);
            String i = params.length == 4 ? "1" : "0";
            String address = toBinary(parseAddress(params[2].trim()), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (Exception e) {
            System.out.println("Error: Invalid parameters in LDR instruction at address " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateLDX(String[] params) {
        if (params.length < 2) {
            System.out.println("Error: LDX instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "100001";
            int x = Integer.parseInt(params[0].trim());
            if (x < 1 || x > 3) {
                throw new IllegalArgumentException("Invalid index register value");
            }
            String ix = toBinary(x, 2);
            String i = params.length == 3 ? "1" : "0";
            String address = toBinary(parseAddress(params[1].trim()), 5);
            String binaryInstruction = opcode + "00" + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (Exception e) {
            System.out.println("Error: Invalid parameters in LDX instruction at address " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateSTR(String[] tokens) {
        System.out.println("Processing STR at address " + currentAddress + ": " + Arrays.toString(tokens));
        if (tokens.length < 3) {
            System.out.println("Error: STR instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "000010";
            String[] rx = tokens[1].split(",");
            if (rx.length < 1) {
                System.out.println("Error: Missing register in STR instruction at address " + currentAddress);
                return "Invalid Instruction";
            }
            int r = Integer.parseInt(rx[0]);
            if (r < 0 || r > 3) {
                System.out.println("Error: Invalid register value in STR instruction at address " + currentAddress);
                return "Invalid Instruction";
            }
            String rBinary = toBinary(r, 2);
            String ix = rx.length > 1 ? toBinary(Integer.parseInt(rx[1]), 2) : "00";
            String i = tokens.length == 4 ? "1" : "0";
            String address = toBinary(parseAddress(tokens[2]), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid parameters in STR instruction at address " + currentAddress + ": " + Arrays.toString(tokens));
            return "Invalid Instruction";
        }
    }

    public String translateLDA(String[] params) {
        if (params.length < 3) {
            System.out.println("Error: LDA instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "000011";
            int r = Integer.parseInt(params[0].trim());
            int x = Integer.parseInt(params[1].trim());
            if (r < 0 || r > 3 || x < 0 || x > 3) {
                throw new IllegalArgumentException("Invalid register or index value");
            }
            String rBinary = toBinary(r, 2);
            String ix = toBinary(x, 2);
            String i = params.length == 4 ? "1" : "0";
            String address = toBinary(parseAddress(params[2].trim()), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (Exception e) {
            System.out.println("Error: Invalid parameters in LDA instruction at address " + currentAddress + ": " + Arrays.toString(params));
            return "Invalid Instruction";
        }
    }

    public String translateSTX(String[] tokens) {
        System.out.println("Processing STX at address " + currentAddress + ": " + Arrays.toString(tokens));

        if (tokens.length < 3) {
            System.out.println("Error: STX instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "010010";
            String[] rx = tokens[1].split(",");
            if (rx.length != 2) {
                System.out.println("Error: Incorrect format for STX instruction at address " + currentAddress);
                return "Invalid Instruction";
            }

            int x = Integer.parseInt(rx[0]);
            if (x < 1 || x > 3) {
                System.out.println("Error: Invalid index register value in STX instruction at address " + currentAddress);
                return "Invalid Instruction";
            }
            String ix = toBinary(x, 2);

            int addressValue = parseAddress(rx[1]);
            if (addressValue == -1) {
                System.out.println("Error: Invalid address in STX instruction at address " + currentAddress);
                return "Invalid Instruction";
            }
            String address = toBinary(addressValue, 5);

            String i = tokens.length == 4 ? "1" : "0";
            String binaryInstruction = opcode + "00" + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid parameters in STX instruction at address " + currentAddress + ": " + Arrays.toString(tokens));
            return "Invalid Instruction";
        }
    }

    public String translateJZ(String[] params) {
        if (params.length < 3) {
            System.out.println("Error: JZ instruction is missing parameters at address " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "001000";
            int r = Integer.parseInt(params[0].trim());
            int x = Integer.parseInt(params[1].trim());
            if (r < 0 || r > 3 || x < 0 || x > 3) {
                throw new IllegalArgumentException("Invalid register or index value");
            }
            String rBinary = toBinary(r, 2);
            String ix = toBinary(x, 2);
            String i = params.length == 4 ? "1" : "0";
            String address = toBinary(parseAddress(params[2].trim()), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (Exception e) {
            System.out.println("Error: Invalid parameters in JZ instruction at address " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateHLT() {
        return "000000";
    }

    private int parseData(String token) {
        try {
            if (token.matches("-?\\d+")) {
                return Integer.parseInt(token);
            } else if (token.toLowerCase().startsWith("0x")) {
                return Integer.parseInt(token.substring(2), 16);
            } else if (token.startsWith("0")) {
                return Integer.parseInt(token, 8);
            } else if (labelAddressMap.containsKey(token)) {
                System.out.println("Data" + labelAddressMap.get(token));
                return labelAddressMap.get(token);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid data value: " + token);
        }
        return -1;
    }

    private int parseAddress(String token) {
        try {
            if (token.matches("-?\\d+")) {
                return Integer.parseInt(token);
            } else if (token.toLowerCase().startsWith("0x")) {
                return Integer.parseInt(token.substring(2), 16);
            } else if (token.startsWith("0")) {
                return Integer.parseInt(token, 8);
            } else if (labelAddressMap.containsKey(token)) {
                System.out.println("Address" + labelAddressMap.get(token));
                return labelAddressMap.get(token);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid address value: " + token);
        }
        return -1;
    }

    public String binaryToOctal(String binaryString) {
        int decimal = Integer.parseInt(binaryString, 2);
        return String.format("%06o", decimal);
    }

    public String toBinary(int number, int bits) {
        return String.format("%" + bits + "s", Integer.toBinaryString(number)).replace(' ', '0');
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
            System.out.println("Error writing listing file: " + e.getMessage());
        }
    }

    private void writeLoadFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Map.Entry<Integer, String> entry : loadFileContent.entrySet()) {
                writer.printf("%06o %s\n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error writing load file: " + e.getMessage());
        }
    }
}