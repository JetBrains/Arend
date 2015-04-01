package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintArgument;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeNames;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Integer, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names) {
    myBuilder = builder;
    myNames = names;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Integer prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
    expr.getFunction().accept(this, Abstract.AppExpression.PREC);
    myBuilder.append(' ');
    if (expr.isExplicit()) {
      expr.getArgument().accept(this, Abstract.AppExpression.PREC + 1);
    } else {
      myBuilder.append('{');
      expr.getArgument().accept(this, 0);
      myBuilder.append('}');
    }
    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Integer prec) {
    myBuilder.append(expr.getDefinition().getName());
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Integer prec) {
    if (expr.getIndex() < myNames.size()) {
      myBuilder.append(myNames.get(myNames.size() - 1 - expr.getIndex()));
    } else {
      myBuilder.append('<').append(expr.getIndex()).append('>');
    }
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Integer prec) {
    if (prec > Abstract.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\");
    for (Abstract.Argument arg : expr.getArguments()) {
      prettyPrintArgument(arg, myBuilder, myNames, 0);
      myBuilder.append(" ");
    }
    myBuilder.append("=> ");
    expr.getBody().accept(this, Abstract.LamExpression.PREC);
    for (Abstract.Argument arg : expr.getArguments()) {
      removeNames(myNames, arg);
    }
    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitNat(Abstract.NatExpression expr, Integer prec) {
    myBuilder.append("N");
    return null;
  }

  @Override
  public Void visitNelim(Abstract.NelimExpression expr, Integer prec) {
    myBuilder.append("N-elim");
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Integer prec) {
    if (prec > Abstract.PiExpression.PREC) myBuilder.append('(');
    int domPrec = expr.getArguments().size() > 1 ? Abstract.AppExpression.PREC + 1 : Abstract.PiExpression.PREC + 1;
    for (Abstract.Argument argument : expr.getArguments()) {
      prettyPrintArgument(argument, myBuilder, myNames, domPrec);
      myBuilder.append(' ');
    }
    myBuilder.append("-> ");
    expr.getCodomain().accept(this, Abstract.PiExpression.PREC);
    for (Abstract.Argument arg : expr.getArguments()) {
      removeNames(myNames, arg);
    }
    if (prec > Abstract.PiExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSuc(Abstract.SucExpression expr, Integer prec) {
    myBuilder.append("S");
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Integer prec) {
    myBuilder.append("Type").append(expr.getLevel() < 0 ? "" : expr.getLevel());
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Integer prec) {
    myBuilder.append(expr.getName());
    return null;
  }

  @Override
  public Void visitZero(Abstract.ZeroExpression expr, Integer prec) {
    myBuilder.append("0");
    return null;
  }

  @Override
  public Void visitHole(Abstract.HoleExpression expr, Integer params) {
    myBuilder.append('?');
    return null;
  }
}
