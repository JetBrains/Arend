package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.List;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Expression myType;

  public OfTypeExpression(Expression expression, Expression type) {
    myExpression = expression;
    myType = type;
  }

  public static Expression make(Expression expression, Expression actualType, Expression expectedType) {
    if ((expectedType instanceof PiExpression || expectedType instanceof SigmaExpression || expectedType instanceof ClassCallExpression) &&
        !(actualType instanceof PiExpression || actualType instanceof SigmaExpression || actualType instanceof ClassCallExpression)) {
      while (expression.toOfType() != null) {
        expression = expression.toOfType().myExpression;
      }
      return new OfTypeExpression(expression, expectedType);
    } else {
      return expression;
    }
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public Expression getFunction() {
    return myExpression.getFunction();
  }

  @Override
  public List<? extends Expression> getArguments() {
    return myExpression.getArguments();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitOfType(this, params);
  }

  public Expression getTypeOf() {
    return myType;
  }

  @Override
  public Expression addArgument(Expression argument) {
    return myExpression.addArgument(argument);
  }

  @Override
  public AppExpression toApp() {
    return myExpression.toApp();
  }

  @Override
  public ClassCallExpression toClassCall() {
    return myExpression.toClassCall();
  }

  @Override
  public ConCallExpression toConCall() {
    return myExpression.toConCall();
  }

  @Override
  public DataCallExpression toDataCall() {
    return myExpression.toDataCall();
  }

  @Override
  public DefCallExpression toDefCall() {
    return myExpression.toDefCall();
  }

  @Override
  public ErrorExpression toError() {
    return myExpression.toError();
  }

  @Override
  public FieldCallExpression toFieldCall() {
    return myExpression.toFieldCall();
  }

  @Override
  public FunCallExpression toFunCall() {
    return myExpression.toFunCall();
  }

  @Override
  public LetClauseCallExpression toLetClauseCall() {
    return myExpression.toLetClauseCall();
  }

  @Override
  public LamExpression toLam() {
    return myExpression.toLam();
  }

  @Override
  public LetExpression toLet() {
    return myExpression.toLet();
  }

  @Override
  public NewExpression toNew() {
    return myExpression.toNew();
  }

  @Override
  public OfTypeExpression toOfType() {
    return this;
  }

  @Override
  public PiExpression toPi() {
    return myExpression.toPi();
  }

  @Override
  public ProjExpression toProj() {
    return myExpression.toProj();
  }

  @Override
  public ReferenceExpression toReference() {
    return myExpression.toReference();
  }

  @Override
  public SigmaExpression toSigma() {
    return myExpression.toSigma();
  }

  @Override
  public TupleExpression toTuple() {
    return myExpression.toTuple();
  }

  @Override
  public UniverseExpression toUniverse() {
    return myExpression.toUniverse();
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}
