package main.java.com.jetbrains.term;

public class NotInScopeException extends Exception {
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
