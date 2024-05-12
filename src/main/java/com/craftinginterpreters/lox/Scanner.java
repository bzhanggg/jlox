package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

// import static for convenience
import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    /** raw source code input to the scanner */
    private final String source;
    /** list of tokens generated from source */
    private final List<Token> tokens = new ArrayList<>();
    /** pointer to the first character of the lexeme being scanned */
    private int start = 0;
    /** pointer to the current character of the lexeme being scanned */
    private int current = 0;
    /** pointer to the current line of the source being scanned */
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // at the beginning of the next lexeme:
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line)); // add EOF token to end of tokens
        return tokens;
    }

    /** checks to see if scanner at the end of the source line */
    private Boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * consumes next character in file and moves current pointer up one
     * @return the character at the current pointer
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * takes the text of the current lexeme and creates a new token for it
     * @param type the type of token the current lexeme is
     */
    private void addToken(TokenType type) {
        addType(type, null);
    }

    /**
     * creates and adds the token to the tokens list
     * @param type the type of the token of the current lexeme
     * @param literal the type of literal the lexeme represents, if it represents one
     */
    private void addType(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void scanToken() {
        char c = advance();
        switch(c) {
            // cases for which the lexeme is a single character long:
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            // cases for which the lexeme is an operator (one or two chars) long:
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // a comment goes until EOL
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            /* ignore whitespace */
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            default:                                                  // case of lexical error
                Lox.error(line, "Unexpected character.");
                break;
        }
    }

    /**
     * Conditional advance to match longer tokens
     * @param expected the character needed to match
     * @return true, if the expected character is next, otherwise false. the current pointer is increment iff true
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Checks the next character in source pointed to by current. Does NOT consume the character
     * @return the character at the current pointer
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
}
