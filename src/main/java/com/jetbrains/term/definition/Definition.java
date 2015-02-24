package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.UniverseExpression;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.util.ArrayList;

public abstract class Definition implements PrettyPrintable {
    private final String name;
    private final Signature signature;
    private final int id;
    private static int idCounter = 0;

    public Definition(String name, Signature signature) {
        this.name = name;
        this.signature = signature;
        id = idCounter++;
    }

    public String getName() {
        return name;
    }

    public Signature getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Definition)) return false;
        Definition other = (Definition)o;
        return other.id == id;
    }

    public Definition checkTypes() {
        Expression typeOfType = signature.getType().inferType(new ArrayList<Definition>());
        if (typeOfType instanceof UniverseExpression) {
            return this;
        } else {
            throw new TypeMismatchException(new UniverseExpression(), typeOfType, signature.getType());
        }
    }
}
