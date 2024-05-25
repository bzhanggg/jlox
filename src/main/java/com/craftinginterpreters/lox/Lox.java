package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runPrompt();
        }
    }

    /**
     * Run the Lox script from a file
     * @param path the filepath to execute
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // indicate an error in exit code
        if (hadError) System.exit(65);
    }

    /**
     * Run jlox interactively from the command line
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null) break;        // only happens when Control+D is inputted
            run(line);
            // reset error flag to prevent early kill session from user error
            hadError = false;
        }
    }

    /**
     * The jlox scanner. For now, just tokenize the input and return it token-by-token.
     * @param source
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Stop if there was a syntax error
        if (hadError) return;

        System.out.println(new AstPrinter().print(expression));
    }

    /* IMPLEMENTING BASIC ERROR HANDLING FOR THE INTERPRETER */

    /**
     * sends an error report to the reporter
     * @param line the line the error occurs on
     * @param message the relevant error message to display
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * reports an error with location and error message
     * @param line the line the error occurs on
     * @param where the type of error that occurred
     * @param message the relevant error message to display
     */
    static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        }
        else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}
