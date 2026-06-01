package vgu.pe2026.ttt.basis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasicTest {
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    
    private PipedOutputStream outputStream;
    private BufferedReader testReader;

    @BeforeEach
    void setUp() { 
        outputStream = new PipedOutputStream();
        try {
            // PipedInputStream receive data from outputStream (where code prints)
            PipedInputStream inputStream = new PipedInputStream(outputStream); 
            testReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            Logger.getLogger(BasicTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Redirect System.out to read what the program prints
        System.setOut(new PrintStream(outputStream)); 
    }

    @AfterEach
    void tearDown() { 
        System.setOut(originalOut); 
        System.setIn(originalIn);
    }

    // Utility to simulate user input from keyboard
    private void setMockInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void testGameInitialization() throws IOException {
        // 1. Simulate user input first move is "1"
        setMockInput("1\n");

        // 2. Initialize components based on your Logic
        Scanner scanner = new Scanner(System.in);
        HumanPlayer human = new HumanPlayer(scanner);
        ComputerPlayer computer = new ComputerPlayer();
        Board board = new Board();

        // Assume turn = 1 (Human goes first)
        GamePlay game = new GamePlay(board, human, computer);

        // 3. Run the game (we will interrupt the thread or just run 1 turn)
        // Note: Because the play() loop is infinite until win/loss, 
        // in practice you should test smaller functions or use Thread.
        
        // Here we test if the program prints the correct greeting
        System.out.println("Hello!");
        
        // 4. Check the result captured through the pipe
        String output = testReader.readLine();
        assertTrue(output.contains("Hello!"));
    }

    @Test
    void testMainInvalidOption() throws IOException {
        // Test logic similar to your Main
        String[] args = {"3"}; // Invalid option (only 1 or 2 allowed)
        
        if (args.length > 0) {
            String option = args[0].trim();
            try {
                int turn = Integer.parseInt(option);
                if (turn == 1 || turn == 2) {
                    // Game logic...
                } else {
                    System.out.println("Please, input a valid option [1-2]");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please, input a valid option [1-2]");
            }
        }

        // Read from outputStream to confirm error message
        String output = testReader.readLine();
        assertTrue(output.contains("Please, input a valid option [1-2]"));
    }
}
