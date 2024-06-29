package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static  com.craftinginterpreters.lox.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /* RULE EVALUATIONS */

    /**
     * The expression rule expands to the equality rule
     */
    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        // if there is an increment, extend the body with an expression statement that evalautes the increment
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        // Take the condition and the body and build the loop using while
        // If condition is omitted, use true to make infinite loop
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // if there is an initializer, run it once before the entire loop`
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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


