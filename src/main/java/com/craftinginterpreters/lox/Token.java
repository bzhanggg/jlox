package com.craftinginterpreters.lox;

public class Token {
    /** the type of token that this token represents */
    final TokenType type;
    /** substring of the source code that is seen as one whole token */
    final String lexeme;
    /** the type of literal the token represents (if it represents a literal) */
    final Object literal;
    /** the line the token occurs on */
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
