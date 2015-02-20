package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.PiExpression;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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

    public FunctionDefinition fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        Argument[] newArguments = new Argument[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            Expression argType = arguments[i].getType().fixVariables(names, signature);
            newArguments[i] = new Argument(arguments[i].isExplicit(), arguments[i].getName(), argType);
            names.add(arguments[i].getName());
        }
        Expression newResultType = resultType.fixVariables(names, signature);
        for (Argument ignored : arguments) {
            names.remove(names.size() - 1);
        }
        return new FunctionDefinition(getName(), newArguments, newResultType, term.fixVariables(names, signature));
    }

}
