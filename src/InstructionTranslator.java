import java.util.*;

public class InstructionTranslator {
    private final Map<String, Integer> labelMap;
    private int currentInstructionAddress;

    public InstructionTranslator(Map<String, Integer> labelMap, int startAddress) {
        this.labelMap = new HashMap<>(labelMap);
        this.currentInstructionAddress = startAddress;
    }

    public void setCurrentAddress(int newAddress) {
        this.currentInstructionAddress = newAddress;
    }

    public void updateLabelAddressMap(String label, int address) {
        labelMap.put(label, address);
    }

    public String translateLDR(String[] params) {
        return translateMemoryInstruction("000001", params);
    }

    public String translateLDX(String[] params) {
        return translateIndexRegisterInstruction("100001", params);
    }

    public String translateSTR(String[] params) {
        return translateMemoryInstruction("000010", params);
    }

    public String translateLDA(String[] params) {
        return translateMemoryInstruction("000011", params);
    }

    public String translateSTX(String[] params) {
        return translateIndexRegisterInstruction("010010", params);
    }

    public String translateJZ(String[] params) {
        return translateMemoryInstruction("001000", params);
    }

    public String translateHLT() {
        return "000000";
    }

    private String translateMemoryInstruction(String opcode, String[] params) {
        if (params.length < 3) {
            logError(opcode, "Insufficient parameters");
            return "Invalid Instruction";
        }

        try {
            int r = parseRegister(params[0]);
            int x = parseRegister(params[1]);
            String i = params.length == 4 ? "1" : "0";
            String address = encodeAddress(params[2]);

            String binaryInstruction = opcode + encodeBits(r, 2) + encodeBits(x, 2) + i + address;
            return convertToOctal(binaryInstruction);
        } catch (Exception e) {
            logError(opcode, e.getMessage());
            return "Invalid Instruction";
        }
    }

    private String translateIndexRegisterInstruction(String opcode, String[] params) {
        if (params.length < 2) {
            logError(opcode, "Insufficient parameters");
            return "Invalid Instruction";
        }

        try {
            int x = parseIndexRegister(params[0]);
            String i = params.length == 3 ? "1" : "0";
            String address = encodeAddress(params[1]);

            String binaryInstruction = opcode + "00" + encodeBits(x, 2) + i + address;
            return convertToOctal(binaryInstruction);
        } catch (Exception e) {
            logError(opcode, e.getMessage());
            return "Invalid Instruction";
        }
    }

    private int parseRegister(String reg) {
        int value = Integer.parseInt(reg.trim());
        if (value < 0 || value > 3) {
            throw new IllegalArgumentException("Invalid register value");
        }
        return value;
    }

    private int parseIndexRegister(String reg) {
        int value = Integer.parseInt(reg.trim());
        if (value < 1 || value > 3) {
            throw new IllegalArgumentException("Invalid index register value");
        }
        return value;
    }

    private String encodeAddress(String addr) {
        return encodeBits(parseAddress(addr.trim()), 5);
    }

    private int parseAddress(String token) {
        if (token.matches("-?\\d+")) {
            return Integer.parseInt(token);
        } else if (token.toLowerCase().startsWith("0x")) {
            return Integer.parseInt(token.substring(2), 16);
        } else if (token.startsWith("0")) {
            return Integer.parseInt(token, 8);
        } else if (labelMap.containsKey(token)) {
            return labelMap.get(token);
        }
        throw new IllegalArgumentException("Invalid address: " + token);
    }

    private String encodeBits(int number, int bits) {
        return String.format("%" + bits + "s", Integer.toBinaryString(number)).replace(' ', '0');
    }

    private String convertToOctal(String binary) {
        return String.format("%06o", Integer.parseInt(binary, 2));
    }

    private void logError(String instruction, String message) {
        System.out.println(instruction + " " + currentInstructionAddress + ": " + message);
    }

    public int parseData(String token) {
        try {
            return parseAddress(token);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid data value: " + token);
            return -1;
        }
    }
}