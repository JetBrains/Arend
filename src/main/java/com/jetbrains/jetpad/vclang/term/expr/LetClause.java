package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintLetClause;

public class LetClause implements Abstract.LetClause {
    private final String myName;
    private final List<Argument> myArguments;
    private final Expression myType;
    private final Expression myTerm;

    public LetClause(String name, List<Argument> arguments, Expression type, Expression term) {
        this.myName = name;
        this.myArguments = arguments;
        this.myType = type;
        this.myTerm = term;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public Expression getTerm() {
        return myTerm;
    }

    @Override
    public List<? extends Argument> getArguments() {
        return myArguments;
    }

    @Override
    public Expression getType() {
        return myType;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        prettyPrintLetClause(this, builder, names, 0);
    }
}
