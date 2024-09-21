import java.util.Arrays;
import java.util.HashMap;

public class InstructionTranslator {
    private final HashMap<String, Integer> labelAddressMap;
    private int currentAddress;

    public InstructionTranslator(HashMap<String, Integer> labelAddressMap, int currentAddress) {
        this.labelAddressMap = labelAddressMap;
        this.currentAddress = currentAddress;
    }
    //Update currentAddress for the instruction
    public void setCurrentAddress(int address) {
        this.currentAddress = address;
    }
    //Update Label Address Map for the instruction
    public void updateLabelAddressMap(String label, int address) {
        labelAddressMap.put(label, address);
    }
    public String translateLDR( String[] params) {
        if (params.length < 3) {
            System.out.println("LDR " + currentAddress);
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
            System.out.println("LDR " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateLDX(String[] params) {
        if (params.length < 2) {
            System.out.println("LDX " + currentAddress);
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
            System.out.println("LDX " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateSTR(String[] parameters) {
        if (parameters.length < 3) {
            System.out.println("STR 1" + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "000010";
            String[] rx = parameters[1].split(",");
            if (rx.length < 1) {
                System.out.println("STR 2" + currentAddress);
                return "Invalid Instruction";
            }
            int r = Integer.parseInt(rx[0]);
            if (r < 0 || r > 3) {
                System.out.println("STR 3 " + currentAddress);
                return "Invalid Instruction";
            }
            String rBinary = toBinary(r, 2);
            String ix = rx.length > 1 ? toBinary(Integer.parseInt(rx[1]), 2) : "00";
            String i = parameters.length == 4 ? "1" : "0";
            String address = toBinary(parseAddress(parameters[2]), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (NumberFormatException e) {
            System.out.println("STR 4 " + currentAddress + ": " + Arrays.toString(parameters));
            return "Invalid Instruction";
        }
    }

    public String translateLDA(String[] params) {
        if (params.length < 3) {
            System.out.println("LDA " + currentAddress);
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
            System.out.println("LDA " + currentAddress + ": " + Arrays.toString(params));
            return "Invalid Instruction";
        }
    }

    public String translateSTX(String[] parameters) {
        System.out.println("STX " + currentAddress + ": " + Arrays.toString(parameters));

        if (parameters.length < 3) {
            System.out.println("STX 2 " + currentAddress);
            return "Invalid Instruction";
        }

        try {
            String opcode = "010010";
            String[] rx = parameters[1].split(",");
            if (rx.length != 2) {
                System.out.println("STX 3 " + currentAddress);
                return "Invalid Instruction";
            }

            int x = Integer.parseInt(rx[0]);
            if (x < 1 || x > 3) {
                System.out.println("STX 4 " + currentAddress);
                return "Invalid Instruction";
            }
            String ix = toBinary(x, 2);

            int addressValue = parseAddress(rx[1]);
            if (addressValue == -1) {
                System.out.println("STX 5 " + currentAddress);
                return "Invalid Instruction";
            }
            String address = toBinary(addressValue, 5);

            String i = parameters.length == 4 ? "1" : "0";
            String binaryInstruction = opcode + "00" + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (NumberFormatException e) {
            System.out.println("STX 6 " + currentAddress + ": " + Arrays.toString(parameters));
            return "Invalid Instruction";
        }
    }

    public String translateJZ(String[] params) {
        if (params.length < 3) {
            System.out.println("JZ " + currentAddress);
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
            String address = toBinary(parseAddress( params[2].trim()), 5);
            String binaryInstruction = opcode + rBinary + ix + i + address;
            return binaryToOctal(binaryInstruction);
        } catch (Exception e) {
            System.out.println("JZ " + currentAddress);
            return "Invalid Instruction";
        }
    }

    public String translateHLT() {
        return "000000";
    }
    public int parseData(String token) {
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
            System.out.println("Invalid data value: " + token);
        }
        return -1;
    }

    public int parseAddress(String token) {
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
            System.out.println("Invalid address value: " + token);
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
}
