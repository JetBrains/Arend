package com.jetbrains.jetpad.vclang.term;

public class NotInScopeException extends RuntimeException {
    private final String name;

    public NotInScopeException(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Not in scope: " + name;
    }
}
