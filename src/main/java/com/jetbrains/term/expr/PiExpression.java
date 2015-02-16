package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class PiExpression extends Expression {
    private final String variable;
    private final Expression left;
    private final Expression right;

    public PiExpression(Expression left, Expression right) {
        this(null, left, right.liftIndex(0, 1));
    }

    public PiExpression(String variable, Expression left, Expression right) {
        this.variable = variable;
        this.left = left;
        this.right = right;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public int precedence() {
        return 6;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (prec > precedence()) stream.print("(");
        if (variable == null) {
            left.prettyPrint(stream, names, precedence() + 1);
        } else {
            stream.print("(" + variable + " : ");
            names.add(variable);
            left.prettyPrint(stream, names, 0);
            names.remove(names.size() - 1);
            stream.print(")");
        }
        stream.print(" -> ");
        right.prettyPrint(stream, names, precedence());
        if (prec > precedence()) stream.print(")");
    }

    @Override
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        if (variable != null) names.add(variable);
        Expression right1 = right.fixVariables(names, signature);
        if (variable != null) names.remove(names.size() - 1);
        return new PiExpression(left.fixVariables(names, signature), right1);
    }

    @Override
    public Expression normalize() {
        return new PiExpression(variable, left.normalize(), right.normalize());
    }

    @Override
    public Expression subst(Expression expr, int from) {
        return new PiExpression(variable, left.subst(expr, from), right.subst(expr, variable == null ? from : from + 1));
    }

    @Override
    public Expression liftIndex(int from, int on) {
        return new PiExpression(variable, left.liftIndex(from, on), right.liftIndex(variable == null ? from : from + 1, on));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PiExpression)) return false;
        PiExpression other = (PiExpression)o;
        return left.equals(other.left) && right.equals(other.right);
    }

    @Override
    public String toString() {
        return "(" + (variable == null ? "" : variable + " : ") + left.toString() + ") -> " + right.toString();
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        Expression typeType = new UniverseExpression();
        Expression leftType = left.inferType(context).normalize();
        context.add(new FunctionDefinition(variable, leftType, new VarExpression(variable)));
        Expression rightType = right.inferType(context).normalize();
        context.remove(context.size() - 1);
        boolean leftOK = leftType.equals(typeType);
        boolean rightOK = rightType.equals(typeType);
        if (leftOK && rightOK) return typeType;
        else throw new TypeMismatchException(typeType, leftOK ? rightType : leftType, leftOK ? right : left);
    }
}
