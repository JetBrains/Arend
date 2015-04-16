package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintArgument;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names) {
    myBuilder = builder;
    myNames = names;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Byte prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
    expr.getFunction().accept(this, Abstract.AppExpression.PREC);
    myBuilder.append(' ');
    if (expr.isExplicit()) {
      expr.getArgument().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    } else {
      myBuilder.append('{');
      expr.getArgument().accept(this, (byte) 0);
      myBuilder.append('}');
    }
    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Byte prec) {
    myBuilder.append(expr.getDefinition().getName());
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Byte prec) {
    if (expr.getIndex() < myNames.size()) {
      myBuilder.append(myNames.get(myNames.size() - 1 - expr.getIndex()));
    } else {
      myBuilder.append('<').append(expr.getIndex()).append('>');
    }
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Byte prec) {
    if (prec > Abstract.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");
    for (Abstract.Argument arg : expr.getArguments()) {
      prettyPrintArgument(arg, myBuilder, myNames, (byte) 0);
      myBuilder.append(" ");
    }
    myBuilder.append("=> ");
    expr.getBody().accept(this, Abstract.LamExpression.PREC);
    for (Abstract.Argument arg : expr.getArguments()) {
      Utils.removeFromList(myNames, arg);
    }
    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitNat(Abstract.NatExpression expr, Byte prec) {
    myBuilder.append("N");
    return null;
  }

  @Override
  public Void visitNelim(Abstract.NelimExpression expr, Byte prec) {
    myBuilder.append("N-elim");
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Byte prec) {
    if (prec > Abstract.PiExpression.PREC) myBuilder.append('(');
    byte domPrec = (byte) (expr.getArguments().size() > 1 ? Abstract.AppExpression.PREC + 1 : Abstract.PiExpression.PREC + 1);
    if (expr.getArguments().size() == 1 && !(expr.getArgument(0) instanceof Abstract.TelescopeArgument)) {
      expr.getArgument(0).getType().accept(this, (byte) (Abstract.PiExpression.PREC + 1));
      myBuilder.append(' ');
      myNames.add(null);
    } else {
      myBuilder.append("\\Pi ");
      for (Abstract.Argument argument : expr.getArguments()) {
        prettyPrintArgument(argument, myBuilder, myNames, domPrec);
        myBuilder.append(' ');
      }
    }
    myBuilder.append("-> ");
    expr.getCodomain().accept(this, Abstract.PiExpression.PREC);
    for (Abstract.Argument arg : expr.getArguments()) {
      Utils.removeFromList(myNames, arg);
    }
    if (prec > Abstract.PiExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSuc(Abstract.SucExpression expr, Byte prec) {
    myBuilder.append("S");
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Byte prec) {
    myBuilder.append(expr.getUniverse());
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Byte prec) {
    myBuilder.append(expr.getName());
    return null;
  }

  @Override
  public Void visitZero(Abstract.ZeroExpression expr, Byte prec) {
    myBuilder.append("0");
    return null;
  }

  @Override
  public Void visitHole(Abstract.HoleExpression expr, Byte prec) {
    myBuilder.append('?');
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Byte prec) {
    myBuilder.append('(');
    for (int i = 0; i < expr.getFields().size(); ++i) {
      expr.getField(i).accept(this, (byte) 0);
      if (i < expr.getFields().size() - 1) {
        myBuilder.append(", ");
      }
    }
    myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Byte prec) {
    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\Sigma");
    for (Abstract.Argument argument : expr.getArguments()) {
      myBuilder.append(' ');
      prettyPrintArgument(argument, myBuilder, myNames, (byte) (Abstract.AppExpression.PREC + 1));
    }
    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Byte prec) {
    if (prec > expr.getBinOp().getPrecedence().priority) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) (expr.getBinOp().getPrecedence().priority + (expr.getBinOp().getPrecedence().associativity == Definition.Associativity.LEFT_ASSOC ? 0 : 1)));
    myBuilder.append(' ').append(expr.getBinOp().getName()).append(' ');
    expr.getRight().accept(this, (byte) (expr.getBinOp().getPrecedence().priority + (expr.getBinOp().getPrecedence().associativity == Definition.Associativity.RIGHT_ASSOC ? 0 : 1)));
    if (prec > expr.getBinOp().getPrecedence().priority) myBuilder.append(')');
    return null;
  }
}
