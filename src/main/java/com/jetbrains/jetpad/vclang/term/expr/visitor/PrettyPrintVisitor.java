package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.LamExpression;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;
  private int myIndent;
  private static final int INDENT = 4;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names, int indent) {
    myBuilder = builder;
    myNames = names;
    myIndent = indent;
  }

  private void visitApps(Abstract.Expression expr, List<Abstract.ArgumentExpression> args, byte prec) {
    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr).getResolvedName() != null) {
      if (((Abstract.DefCallExpression) expr).getName().fixity == Abstract.Definition.Fixity.INFIX) {
        int numberOfVisibleArgs = 0;
        List<Abstract.Expression> visibleArgs = new ArrayList<>(2);
        for (Abstract.ArgumentExpression arg : args) {
          if (arg.isExplicit() || !arg.isHidden()) {
            if (++numberOfVisibleArgs > 2) break;
            visibleArgs.add(arg.getExpression());
          }
        }

        if (numberOfVisibleArgs == 2) {
          Abstract.Definition.Precedence defPrecedence = ((Abstract.DefCallExpression) expr).getResolvedName().toPrecedence() == null
              ? Abstract.Definition.DEFAULT_PRECEDENCE : ((Abstract.DefCallExpression) expr).getResolvedName().toPrecedence();
          if (prec > defPrecedence.priority) myBuilder.append('(');
          if (((Abstract.DefCallExpression) expr).getExpression() != null) {
            ((Abstract.DefCallExpression) expr).getExpression().accept(this, Abstract.DefCallExpression.PREC);
            myBuilder.append('.');
          }
          visibleArgs.get(0).accept(this, (byte) (defPrecedence.priority + (defPrecedence.associativity == Definition.Associativity.LEFT_ASSOC ? 0 : 1)));
          myBuilder.append(' ').append(((Abstract.DefCallExpression) expr).getName().name).append(' ');
          visibleArgs.get(1).accept(this, (byte) (defPrecedence.priority + (defPrecedence.associativity == Definition.Associativity.RIGHT_ASSOC ? 0 : 1)));
          if (prec > defPrecedence.priority) myBuilder.append(')');
          return;
        }
      }

      if (((Abstract.DefCallExpression) expr).getResolvedName().toDefinition() == Prelude.PATH && args.size() == 3 && args.get(0).getExpression() instanceof LamExpression && ((LamExpression) args.get(0).getExpression()).getBody().liftIndex(0, -1) != null) {
        if (prec > Prelude.PATH_INFIX.getPrecedence().priority) myBuilder.append('(');
        args.get(1).getExpression().accept(this, (byte) (Prelude.PATH_INFIX.getPrecedence().priority + 1));
        myBuilder.append(" = ");
        args.get(2).getExpression().accept(this, (byte) (Prelude.PATH_INFIX.getPrecedence().priority + 1));
        if (prec > Prelude.PATH_INFIX.getPrecedence().priority) myBuilder.append(')');
        return;
      }
    }

    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr).getExpression() != null) {
      ((Abstract.DefCallExpression) expr).getExpression().accept(this, Abstract.DefCallExpression.PREC);
      myBuilder.append('.');
    }
    expr.accept(this, Abstract.AppExpression.PREC);

    for (Abstract.ArgumentExpression arg : args) {
      if (arg.isExplicit()) {
        myBuilder.append(' ');
        if (arg.isHidden()) {
          myBuilder.append('_');
        } else {
          arg.getExpression().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
        }
      } else {
        if (!arg.isHidden()) {
          myBuilder.append(" {");
          arg.getExpression().accept(this, Abstract.Expression.PREC);
          myBuilder.append('}');
        }
      }
    }

    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
  }

  private Integer getNumber(Abstract.Expression expr) {
    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr).getResolvedName() != null
        && ((Abstract.DefCallExpression) expr).getResolvedName().toDefinition() == Prelude.ZERO) {
      return 0;
    }
    if (expr instanceof Abstract.AppExpression && ((Abstract.AppExpression) expr).getFunction() instanceof Abstract.DefCallExpression
        && ((Abstract.DefCallExpression) ((Abstract.AppExpression) expr).getFunction()).getResolvedName() != null
        && ((Abstract.DefCallExpression) ((Abstract.AppExpression) expr).getFunction()).getResolvedName().toDefinition() == Prelude.SUC) {
      Integer result = getNumber(((Abstract.AppExpression) expr).getArgument().getExpression());
      if (result == null) return null;
      return result + 1;
    }
    return null;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Byte prec) {
    Integer number = getNumber(expr);
    if (number != null) {
      myBuilder.append(number);
      return null;
    }

    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    visitApps(Abstract.getFunction(expr, args), args, prec);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Byte prec) {
    if (expr.getResolvedName() != null && expr.getResolvedName().toDefinition() == Prelude.ZERO) {
      myBuilder.append("0");
    } else {
      myBuilder.append(expr.getName());
    }
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Byte prec) {
    if (expr.getIndex() < myNames.size()) {
      String var = myNames.get(myNames.size() - 1 - expr.getIndex());
      myBuilder.append(var != null ? var : "_");
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
    if (expr.getArguments().size() == 1 && !(expr.getArguments().get(0) instanceof Abstract.TelescopeArgument)) {
      expr.getArguments().get(0).getType().accept(this, (byte) (Abstract.PiExpression.PREC + 1));
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
  public Void visitInferHole(Abstract.InferHoleExpression expr, Byte prec) {
    myBuilder.append('_');
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Byte prec) {
    myBuilder.append("{?");
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
      expr.getFields().get(i).accept(this, Abstract.Expression.PREC);
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
    if (prec > expr.getResolvedBinOpName().toPrecedence().priority) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) (expr.getResolvedBinOpName().toPrecedence().priority + (expr.getResolvedBinOpName().toPrecedence().associativity == Definition.Associativity.LEFT_ASSOC ? 0 : 1)));
    myBuilder.append(' ').append(expr.getResolvedBinOpName().toNamespace().getName().getInfixName()).append(' ');
    expr.getRight().accept(this, (byte) (expr.getResolvedBinOpName().toPrecedence().priority + (expr.getResolvedBinOpName().toPrecedence().associativity == Definition.Associativity.RIGHT_ASSOC ? 0 : 1)));
    if (prec > expr.getResolvedBinOpName().toPrecedence().priority) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Byte prec) {
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) 10);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      myBuilder.append(' ').append(elem.binOp.getName().getInfixName()).append(' ');
      elem.argument.accept(this, (byte) 10);
    }
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append(')');
    return null;
  }

  private void visitElimCaseExpression(Abstract.ElimCaseExpression expr, Byte prec) {
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append('(');
    myBuilder.append(expr instanceof Abstract.ElimExpression ? "\\elim" : "\\case");
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      myBuilder.append(" ");
      expr.getExpressions().get(i).accept(this, Abstract.Expression.PREC);
      if (i != expr.getExpressions().size() - 1)
        myBuilder.append(",");
    }
    myBuilder.append('\n');
    myIndent += INDENT;
    for (Abstract.Clause clause : expr.getClauses()) {
      prettyPrintClause(expr, clause, myBuilder, myNames, myIndent);
    }

    printIndent(myBuilder, myIndent);
    myBuilder.append(';');
    myIndent -= INDENT;
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append(')');
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Byte prec) {
    visitElimCaseExpression(expr, prec);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Byte params) {
    visitElimCaseExpression(expr, params);
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Byte prec) {
    if (prec > Abstract.ProjExpression.PREC) myBuilder.append('(');
    expr.getExpression().accept(this, Abstract.ProjExpression.PREC);
    myBuilder.append('.').append(expr.getField() + 1);
    if (prec > Abstract.ProjExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Byte prec) {
    if (prec > Abstract.ClassExtExpression.PREC) myBuilder.append('(');
    expr.getBaseClassExpression().accept(this, (byte) -Abstract.ClassExtExpression.PREC);
    myBuilder.append(" {\n");
    myIndent += INDENT;
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      printIndent(myBuilder, myIndent);
      myBuilder.append("| ").append(statement.getName().getPrefixName()).append(" => ");
      statement.getExpression().accept(this, Abstract.Expression.PREC);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    if (prec > Abstract.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Byte prec) {
    if (prec > Abstract.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, Abstract.NewExpression.PREC);
    if (prec > Abstract.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Byte prec) {
    final int oldNamesSize = myNames.size();
    if (prec > Abstract.LetExpression.PREC) myBuilder.append('(');
    myBuilder.append("\n");
    myIndent += INDENT;
    printIndent(myBuilder, myIndent);
    String let = "\\let ";
    myBuilder.append(let);

    final int INDENT0 = let.length();
    myIndent += INDENT0;
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      prettyPrintLetClause(expr.getClauses().get(i), myBuilder, myNames, myIndent);
      myBuilder.append("\n");
      if (i == expr.getClauses().size() - 1) {
        myIndent -= INDENT0;
      }
      printIndent(myBuilder, myIndent);
      myNames.add(expr.getClauses().get(i).getName() == null ? null : expr.getClauses().get(i).getName().name);
    }

    String in = "\\in ";
    myBuilder.append(in);
    final int INDENT1 = in.length();
    myIndent += INDENT1;
    expr.getExpression().accept(this, Abstract.LetExpression.PREC);
    myIndent -= INDENT1;
    myIndent -= INDENT;

    trimToSize(myNames, oldNamesSize);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Byte params) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  public static void printIndent(StringBuilder builder, int indent) {
    for (int i = 0; i < indent; ++i) {
      builder.append(' ');
    }
  }
}
