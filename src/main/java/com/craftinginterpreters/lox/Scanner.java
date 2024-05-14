package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /** map to check if an identifier lexeme is one of the reserved words */
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

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
        addToken(type, null);
    }

    /**
     * creates and adds the token to the tokens list
     * @param type the type of the token of the current lexeme
     * @param literal the type of literal the lexeme represents, if it represents one
     */
    private void addToken(TokenType type, Object literal) {
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
            /* string literals */
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    /* number literals */
                    number();
                } else if (isAlpha(c)) {
                    /* reserved words */
                    identifier();
                } else {
                    /* lexical error */
                    Lox.error(line, "Unexpected character.");
                }
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

    /**
     * Second character lookahead
     * @return true, if there exists a character at `current+1`, otherwise the null terminator
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Checks if the current character pointed to by current is a digit, for the case of a number lexeme
     * @param c the character to check
     * @return true iff c is a digit
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if the current character pointed to by current is an alphabet character (including the underscore char)
     * @param c the current character pointed to by current
     * @return true, iff c is alpha
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * Checks if the current character pointed to by current is an alphanumeric character
     * @param c the current character pointed to by current
     * @return true, iff c is alphanumeric
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Consumes characters until find the " that ends the string. Also handles case of unterminated string
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // consume closing "
        advance();

        // trim surrounding quotes
        String value = source.substring(start+1, current-1);
        addToken(STRING, value);
    }

    /**
     * Captures a number type, including decimals
     */
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the '.'
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Captures a generic identifier, to be checked against the map of reserved words
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        // check to see if lexeme matches with a predefined reserved type
        TokenType type = keywords.get(text);
        // otherwise, the lexeme is a generic identifier
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }
}
