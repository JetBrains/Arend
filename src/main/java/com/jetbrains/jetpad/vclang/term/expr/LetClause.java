package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintLetClause;

public class LetClause extends FunctionDefinition implements Abstract.LetClause {

    public LetClause(String name, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term) {
        super(name, null, null, Fixity.PREFIX, arguments, resultType, arrow, term);
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        prettyPrintLetClause(this, builder, names, 0);
    }
}
