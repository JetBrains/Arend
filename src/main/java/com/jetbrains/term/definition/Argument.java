package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.PiExpression;

import java.io.PrintStream;
import java.util.List;

public class Argument implements PrettyPrintable {
    private final boolean explicit;
    private final String name;
    private final Expression type;

    public Argument(boolean explicit, String name, Expression type) {
        this.explicit = explicit;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public Expression getType() {
        return type;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (explicit) {
            if (name == null) {
                type.prettyPrint(stream, names, PiExpression.PREC);
            } else {
                stream.print('(');
                stream.print(name + " : ");
                type.prettyPrint(stream, names, 0);
                stream.print(')');
            }
        } else {
            stream.print('{');
            if (name != null) {
                stream.print(name + " : ");
            }
            assert name != null;
            type.prettyPrint(stream, names, 0);
            stream.print('}');
        }
    }
}
