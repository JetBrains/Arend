package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

import java.io.PrintStream;
import java.util.List;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Integer, Void> {
  private final PrintStream myStream;
  private final List<String> myNames;

  public PrettyPrintVisitor(PrintStream stream, List<String> names) {
    myStream = stream;
    myNames = names;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Integer prec) {
    if (prec > Abstract.AppExpression.PREC) myStream.print("(");
    expr.getFunction().accept(this, Abstract.AppExpression.PREC);
    myStream.print(" ");
    if (expr.isExplicit()) {
      expr.getArgument().accept(this, Abstract.AppExpression.PREC + 1);
    } else {
      myStream.print("{");
      expr.getArgument().accept(this, 0);
      myStream.print("}");
    }
    if (prec > Abstract.AppExpression.PREC) myStream.print(")");
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Integer prec) {
    myStream.print(expr.getDefinition().getName());
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Integer prec) {
    assert expr.getIndex() < myNames.size();
    myStream.print(myNames.get(myNames.size() - 1 - expr.getIndex()));
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Integer prec) {
    if (prec > Abstract.LamExpression.PREC) myStream.print("(");
    String var;
    for (var = expr.getVariable(); myNames.contains(var); var += "'");
    myStream.print("\\" + var + " => ");
    myNames.add(var);
    expr.getBody().accept(this, Abstract.LamExpression.PREC);
    myNames.remove(myNames.size() - 1);
    if (prec > Abstract.LamExpression.PREC) myStream.print(")");
    return null;
  }

  @Override
  public Void visitNat(Abstract.NatExpression expr, Integer prec) {
    myStream.print("N");
    return null;
  }

  @Override
  public Void visitNelim(Abstract.NelimExpression expr, Integer prec) {
    myStream.print("N-elim");
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Integer prec) {
    if (prec > Abstract.PiExpression.PREC) myStream.print("(");
    if (expr.getVariable() == null) {
      expr.getDomain().accept(this, Abstract.PiExpression.PREC + 1);
    } else {
      myStream.print("(" + expr.getVariable() + " : ");
      expr.getDomain().accept(this, 0);
      myStream.print(")");
    }
    myStream.print(" -> ");
    myNames.add(expr.getVariable());
    expr.getCodomain().accept(this, Abstract.PiExpression.PREC);
    myNames.remove(myNames.size() - 1);
    if (prec > Abstract.PiExpression.PREC) myStream.print(")");
    return null;
  }

  @Override
  public Void visitSuc(Abstract.SucExpression expr, Integer prec) {
    myStream.print("S");
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Integer prec) {
    myStream.print("Type" + (expr.getLevel() < 0 ? "" : expr.getLevel()));
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Integer prec) {
    myStream.print(expr.getName());
    return null;
  }

  @Override
  public Void visitZero(Abstract.ZeroExpression expr, Integer prec) {
    myStream.print("0");
    return null;
  }

  @Override
  public Void visitHole(Abstract.HoleExpression expr, Integer params) {
    myStream.print("?");
    return null;
  }
}
