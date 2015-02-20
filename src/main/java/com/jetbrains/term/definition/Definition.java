package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.PiExpression;

public abstract class Definition implements PrettyPrintable {
    private final String name;
    private final Argument[] arguments;
    private final Expression resultType;
    private final int id;
    private static int idCounter = 0;

    public Definition(String name, Argument[] arguments, Expression resultType) {
        this.name = name;
        this.arguments = arguments;
        this.resultType = resultType;
        id = idCounter++;
    }

    public String getName() {
        return name;
    }

    public Expression getResultType() {
        return resultType;
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public Argument getArgument(int i) {
        return arguments[i];
    }

    public Expression getType() {
        Expression type = resultType;
        for (int i = arguments.length - 1; i >= 0; --i) {
            type = new PiExpression(arguments[i].getName(), arguments[i].getType(), type);
        }
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Definition)) return false;
        Definition other = (Definition)o;
        return other.id == id;
    }
}
