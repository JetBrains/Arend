package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintArgument;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;
  private int myIndent;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names, int indent) {
    myBuilder = builder;
    myNames = names;
    myIndent = indent;
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
      expr.getArgument().accept(this, Abstract.Expression.PREC);
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
      String var = myNames.get(myNames.size() - 1 - expr.getIndex());
      if (var != null) {
        myBuilder.append(var);
        return null;
      }
    }
    myBuilder.append('<').append(expr.getIndex()).append('>');
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Byte prec) {
    if (prec > Abstract.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");
    for (Abstract.Argument arg : expr.getArguments()) {
      prettyPrintArgument(arg, myBuilder, myNames, Abstract.Expression.PREC, myIndent);
      myBuilder.append(" ");
    }
    myBuilder.append("=> ");
    expr.getBody().accept(this, Abstract.LamExpression.PREC);
    for (Abstract.Argument arg : expr.getArguments()) {
      removeFromList(myNames, arg);
    }
    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
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
        prettyPrintArgument(argument, myBuilder, myNames, domPrec, myIndent);
        myBuilder.append(' ');
      }
    }
    myBuilder.append("-> ");
    expr.getCodomain().accept(this, Abstract.PiExpression.PREC);
    removeFromList(myNames, expr.getArguments());
    if (prec > Abstract.PiExpression.PREC) myBuilder.append(')');
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
  public Void visitInferHole(Abstract.InferHoleExpression expr, Byte prec) {
    myBuilder.append('_');
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Byte prec) {
    myBuilder.append("{!");
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this, Abstract.Expression.PREC);
    }
    myBuilder.append('}');
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Byte prec) {
    myBuilder.append('(');
    for (int i = 0; i < expr.getFields().size(); ++i) {
      expr.getField(i).accept(this, Abstract.Expression.PREC);
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
      prettyPrintArgument(argument, myBuilder, myNames, (byte) (Abstract.AppExpression.PREC + 1), myIndent);
    }
    removeFromList(myNames, expr.getArguments());
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

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Byte prec) {
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append('(');
    myBuilder.append(expr.getElimType() == Abstract.ElimExpression.ElimType.ELIM ? "\\elim " : "\\case ");
    expr.getExpression().accept(this, Abstract.Expression.PREC);
    myBuilder.append('\n');
    ++myIndent;
    for (Abstract.Clause clause : expr.getClauses()) {
      printIndent();
      myBuilder.append("| ").append(clause.getName());
      int startIndex = myNames.size();
      for (Abstract.Argument argument : clause.getArguments()){
        myBuilder.append(' ');
        prettyPrintArgument(argument, myBuilder, myNames, (byte) (Abstract.AppExpression.PREC + 1), myIndent);
      }

      List<String> names = myNames;
      if (expr.getExpression() instanceof Abstract.IndexExpression) {
        int varIndex = ((Abstract.IndexExpression) expr.getExpression()).getIndex();
        names = new ArrayList<>(myNames.subList(0, startIndex - varIndex - 1 > 0 ? startIndex - varIndex - 1 : 0));
        names.addAll(myNames.subList(startIndex, myNames.size()));
        if (startIndex >= varIndex) {
          names.addAll(myNames.subList(startIndex - varIndex, startIndex));
        } else {
          for (int i = 0; i < varIndex; ++i) {
            names.add(null);
          }
        }
      }

      myBuilder.append(clause.getArrow() == Abstract.Definition.Arrow.LEFT ? " <= " : " => ");
      clause.getExpression().accept(new PrettyPrintVisitor(myBuilder, names, myIndent), Abstract.Expression.PREC);
      myBuilder.append('\n');
      removeFromList(myNames, clause.getArguments());
    }
    printIndent();
    myBuilder.append(';');
    --myIndent;
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append(')');
    return null;
  }

  private void printIndent() {
    for (int i = 0; i < myIndent; ++i) {
      myBuilder.append('\t');
    }
  }
}
