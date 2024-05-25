package com.craftinginterpreters.lox;

import java.util.List;

import static  com.craftinginterpreters.lox.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        }
        catch (ParseError error) {
            return null;
        }
    }

    /* RULE EVALUATIONS */

    /**
     * The expression rule expands to the equality rule
     */
    private Expr expression() {
        return equality();
    }

    /**
     * The equality expression is a left-expansion into comparisons
     * equality --> comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        Expr expr = comparison();   // left side comparison call

        // match as many "!=" or "==" tokens as needed
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * the comparison expression is a left-expansion into terms
     * comparison --> term ( ( ">" | ">=" | "<" | "<=" ) term )*;
     */
    private Expr comparison() {
        Expr expr = term();     // left side term call

        // match as many ">, >=, <, <=" tokens as needed
        while (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * the term expression is a left-expansion into factors
     * term --> factor ( ( "-" | "+" ) factor )*;
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * the factor expression is a left-expansion into unarys
     * factor --> unary ( ( "/" | "*" ) unary )*;
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * The unary expression is a right-expression into another unary or a primary
     * unary --> ( "!" | "-" ) unary | primary;
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    /**
     * The primary expression evaluates to a terminal or a grouping
     * primary --> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            // MUST find a closing ')' token after seeing a '(', otherwise throw an error
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    /* PARSER HELPER METHODS */

    /**
     * Checks to see if the current token has any of the given types.
     * @param types the types of tokens to check the current token against
     * @return true iff the current token type is any of types
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes and ends an expression parse. If there is a syntax error, throw an error
     * @param type the type of token to check the current pointer against
     * @param message the error message to throw, if needed
     * @return true iff the current token is of the given type.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * Checks to see if the current token is of the given type. Does not consume the token
     * @param type the type of token to check the current token type against
     * @return true iff the current token is of the given type.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consumes the current token and returns it
     * @return the token at the current pointer
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * checks to see if the current pointer is at the end of the token list
     * @return true iff the current pointer is at EOF
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * @return the token at the current pointer
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * @return the token before the current pointer
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * reports an error
     * @return a ParseError to allow the calling method to decide whether to unwind
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Discard tokens until likely at a statement boundary. Discards tokens that would cause cascading errors.
     * We are (probably) finished with a statement after a semicolon
     * Most statements start with a keyword (for, if, return, var, etc.)
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            // if previous token is a semicolon, probably finished a statement
            if (previous().type == SEMICOLON) return;
            // if next token is a keyword, probably starting a statement
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}


