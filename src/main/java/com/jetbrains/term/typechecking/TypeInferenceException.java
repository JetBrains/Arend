package main.java.com.jetbrains.term.typechecking;

import main.java.com.jetbrains.term.expr.Expression;

public class TypeInferenceException extends TypeCheckingException {
    public TypeInferenceException(Expression expression) {
        super(expression);
    }

    @Override
    public String toString() {
        String message = "Cannot infer type";
        if (getExpression() == null) {
            return message;
        } else {
            return message + " of " + getExpression();
        }
    }
}
