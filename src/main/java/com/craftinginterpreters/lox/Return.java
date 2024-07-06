package com.craftinginterpreters.lox;

class Return extends RuntimeException{
    final Object value;

    Return(Object value) {
        super(null, null, false, false); // disable JVM overhead
        this.value = value;
    }
}
