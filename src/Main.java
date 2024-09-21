import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Assembler assembler = new Assembler();

        // Specify the source file and the output files
        String sourceFile = "input.txt";
        String loadFile = "load.txt";
        String listingFile = "listing.txt";
        // Assemble the source code and generate the output files
        assembler.readInstructionFile(sourceFile);
        assembler.firstPass();
        assembler.secondPass();
        assembler.writeFiles(listingFile, loadFile);

        System.out.println("Assembly completed. Listing and load files generated.");
    }
}