package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.PiExpression;

import java.io.PrintStream;
import java.util.List;

public final class FunctionDefinition extends Definition implements PrettyPrintable {
    private final Argument[] arguments;
    private final Expression resultType;
    private final Expression term;

    public FunctionDefinition(String name, Argument[] arguments, Expression resultType, Expression term) {
        super(name);
        this.resultType = resultType;
        this.term = term;
        this.arguments = arguments;
    }

    public Expression getResultType() {
        return resultType;
    }

    @Override
    public Expression getTerm() {
        return term;
    }

    @Override
    public Expression getType() {
        Expression type = resultType;
        for (int i = arguments.length - 1; i >= 0; --i) {
            type = new PiExpression(arguments[i].getName(), arguments[i].getType(), type);
        }
        return type;
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(getName() + " : ");
        for (Argument arg : arguments) {
            arg.prettyPrint(stream, names, 0);
            stream.print(' ');
        }
        resultType.prettyPrint(stream, names, 0);
        stream.print("\n    = ");
        term.prettyPrint(stream, names, 0);
        stream.print(";");
    }

}
