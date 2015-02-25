package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class FunctionDefinition extends Definition {
    private final Expression term;

    public FunctionDefinition(String name, Signature signature, Expression term) {
        super(name, signature);
        this.term = term;
    }

    public Expression getTerm() {
        return term;
    }

    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(getName() + " : ");
        getSignature().getType().prettyPrint(stream, names, 0);
        stream.print("\n    = ");
        term.prettyPrint(stream, names, 0);
        stream.print(";");
    }

    @Override
    public String toString() {
        return getName() + " : " + getSignature().toString() + " = " + term.toString() + ";";
    }

    @Override
    public FunctionDefinition checkTypes() {
        super.checkTypes();
        Expression type = getSignature().getType();
        term.checkType(new ArrayList<Definition>(), type);
        return new FunctionDefinition(getName(), new Signature(type), term);
    }
}
