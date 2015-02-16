package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;

import java.io.PrintStream;
import java.util.List;

public final class FunctionDefinition extends Definition implements PrettyPrintable {
    private final Expression type;
    private final Expression term;

    public FunctionDefinition(String name, Expression type, Expression term) {
        super(name);
        this.type = type;
        this.term = term;
    }

    @Override
    public Expression getType() {
        return type;
    }

    @Override
    public Expression getTerm() {
        return term;
    }

    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(getName() + " : ");
        type.prettyPrint(stream, names, 0);
        stream.print("\n    = ");
        term.prettyPrint(stream, names, 0);
        stream.print(";");
    }
}
