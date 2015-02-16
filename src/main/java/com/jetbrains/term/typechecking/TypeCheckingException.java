package main.java.com.jetbrains.term.typechecking;

import main.java.com.jetbrains.term.expr.Expression;

public class TypeCheckingException extends Exception {
    private final Expression expression;

    public TypeCheckingException(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        String message = "Type checking error";
        if (getExpression() == null) {
            return message;
        } else {
            return message + " in " + getExpression();
        }
    }
}
