package main.java.com.jetbrains.term.typechecking;

import main.java.com.jetbrains.term.expr.Expression;

public class TypeMismatchException extends TypeCheckingException {
    private final Expression expected;
    private final Expression actual;

    public TypeMismatchException(Expression expected, Expression actual, Expression expression) {
        super(expression);
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String toString() {
        String message = "Type mismatch:\n" +
                "Expected type: " + expected + "\n" +
                "Actual type: " + actual;
        if (getExpression() != null) {
            message += "\n" +
                    "In expression: " + getExpression();
        }
        return message;
    }
}
