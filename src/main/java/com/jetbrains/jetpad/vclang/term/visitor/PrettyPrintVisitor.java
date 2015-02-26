package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

import java.io.PrintStream;
import java.util.List;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Void> {
  private final PrintStream stream;
  private final List<String> names;
  private final int prec;

  public PrettyPrintVisitor(PrintStream stream, List<String> names, int prec) {
    this.stream = stream;
    this.names = names;
    this.prec = prec;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr) {
    if (prec > Abstract.AppExpression.PREC) stream.print("(");
    expr.getFunction().accept(new PrettyPrintVisitor(stream, names, Abstract.AppExpression.PREC));
    stream.print(" ");
    expr.getArgument().accept(new PrettyPrintVisitor(stream, names, Abstract.AppExpression.PREC + 1));
    if (prec > Abstract.AppExpression.PREC) stream.print(")");
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr) {
    stream.print(expr.getDefinition().getName());
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr) {
    assert expr.getIndex() < names.size();
    stream.print(names.get(names.size() - 1 - expr.getIndex()));
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr) {
    if (prec > Abstract.LamExpression.PREC) stream.print("(");
    String var;
    for (var = expr.getVariable(); names.contains(var); var += "'");
    stream.print("\\" + var + " -> ");
    names.add(var);
    expr.getBody().accept(new PrettyPrintVisitor(stream, names, Abstract.LamExpression.PREC));
    names.remove(names.size() - 1);
    if (prec > Abstract.LamExpression.PREC) stream.print(")");
    return null;
  }

  @Override
  public Void visitNat(Abstract.NatExpression expr) {
    stream.print("N");
    return null;
  }

  @Override
  public Void visitNelim(Abstract.NelimExpression expr) {
    stream.print("N-elim");
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr) {
    if (prec > Abstract.PiExpression.PREC) stream.print("(");
    if (expr.getVariable() == null) {
      expr.getLeft().accept(new PrettyPrintVisitor(stream, names, Abstract.PiExpression.PREC + 1));
    } else {
      stream.print("(" + expr.getVariable() + " : ");
      expr.getLeft().accept(new PrettyPrintVisitor(stream, names, 0));
      stream.print(")");
    }
    stream.print(" -> ");
    names.add(expr.getVariable());
    expr.getRight().accept(new PrettyPrintVisitor(stream, names, Abstract.PiExpression.PREC));
    names.remove(names.size() - 1);
    if (prec > Abstract.PiExpression.PREC) stream.print(")");
    return null;
  }

  @Override
  public Void visitSuc(Abstract.SucExpression expr) {
    stream.print("S");
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr) {
    stream.print("Type" + (expr.getLevel() < 0 ? "" : expr.getLevel()));
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr) {
    stream.print(expr.getName());
    return null;
  }

  @Override
  public Void visitZero(Abstract.ZeroExpression expr) {
    stream.print("0");
    return null;
  }
}
