package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.expr.Expression;

import java.io.PrintStream;
import java.util.List;

public final class FunctionDefinition extends Definition {
    private final Expression term;

    public FunctionDefinition(String name, Argument[] arguments, Expression resultType, Expression term) {
        super(name, arguments, resultType);
        this.term = term;
    }

    public Expression getTerm() {
        return term;
    }

    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(getName() + " : ");
        for (Argument arg : getArguments()) {
            arg.prettyPrint(stream, names, 0);
            stream.print(' ');
        }
        getResultType().prettyPrint(stream, names, 0);
        stream.print("\n    = ");
        term.prettyPrint(stream, names, 0);
        stream.print(";");
    }

}
