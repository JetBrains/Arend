package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.expr.Expression;

public abstract class Definition {
    private final String name;
    private final int id;
    private static int idCounter = 0;

    public Definition(String name) {
        this.name = name;
        id = idCounter++;
    }

    public String getName() {
        return name;
    }

    public abstract Expression getTerm();

    public abstract Expression getType();

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Definition)) return false;
        Definition other = (Definition)o;
        return other.id == id;
    }
}
