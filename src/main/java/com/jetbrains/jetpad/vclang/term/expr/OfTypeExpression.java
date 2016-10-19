package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class OfTypeExpression extends Expression {
  private final Expression myExpression;
  private final Type myType;

  public OfTypeExpression(Expression expression, Type type) {
    myExpression = expression;
    myType = type;
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

  @Override
  public Type getType() {
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
}
