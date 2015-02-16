package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeInferenceException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class NelimExpression extends Expression {
    @Override
    public int precedence() {
        return 11;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof NelimExpression;
    }

    @Override
    public String toString() {
        return "N-elim";
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        throw new TypeInferenceException(this);
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitNelim(this);
    }
}
