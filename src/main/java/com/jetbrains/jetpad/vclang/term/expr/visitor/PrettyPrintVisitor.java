package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;
  private int myIndent;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names, int indent) {
    myBuilder = builder;
    myNames = names;
    myIndent = indent;
  }

  private void visitApps(Abstract.Expression expr, List<Abstract.ArgumentExpression> args, byte prec) {
    if (expr instanceof Abstract.DefCallExpression) {
      if (((Abstract.DefCallExpression) expr).getDefinition().getFixity() == Abstract.Definition.Fixity.INFIX) {
        int numberOfVisibleArgs = 0;
        List<Abstract.Expression> visibleArgs = new ArrayList<>(2);
        for (Abstract.ArgumentExpression arg : args) {
          if (arg.isExplicit() || !arg.isHidden()) {
            if (++numberOfVisibleArgs > 2) break;
            visibleArgs.add(arg.getExpression());
          }
        }

        if (numberOfVisibleArgs == 2) {
          Abstract.Definition.Precedence defPrecedence = ((Abstract.DefCallExpression) expr).getDefinition().getPrecedence();
          if (prec > defPrecedence.priority) myBuilder.append('(');
          visibleArgs.get(0).accept(this, (byte) (defPrecedence.priority + (defPrecedence.associativity == Definition.Associativity.LEFT_ASSOC ? 0 : 1)));
          myBuilder.append(' ').append(((Abstract.DefCallExpression) expr).getDefinition().getName()).append(' ');
          visibleArgs.get(1).accept(this, (byte) (defPrecedence.priority + (defPrecedence.associativity == Definition.Associativity.RIGHT_ASSOC ? 0 : 1)));
          if (prec > defPrecedence.priority) myBuilder.append(')');
          return;
        }
      }

      if (((Abstract.DefCallExpression) expr).getDefinition().equals(Prelude.PATH) && args.size() == 3 && args.get(0).getExpression() instanceof Expression && Apps(((Expression) args.get(0).getExpression()).liftIndex(0, 1), Index(0)).normalize(NormalizeVisitor.Mode.NF).liftIndex(0, -1) != null) {
        if (prec > Prelude.PATH_INFIX.getPrecedence().priority) myBuilder.append('(');
        args.get(1).getExpression().accept(this, (byte) (Prelude.PATH_INFIX.getPrecedence().priority + 1));
        myBuilder.append(" = ");
        args.get(2).getExpression().accept(this, (byte) (Prelude.PATH_INFIX.getPrecedence().priority + 1));
        if (prec > Prelude.PATH_INFIX.getPrecedence().priority) myBuilder.append(')');
        return;
      }
    }

    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
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
    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr).getDefinition() == Prelude.ZERO) {
      return 0;
    }
    if (expr instanceof Abstract.AppExpression && ((Abstract.AppExpression) expr).getFunction() instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) ((Abstract.AppExpression) expr).getFunction()).getDefinition() == Prelude.SUC) {
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
    if (expr.getDefinition().getFixity() == Abstract.Definition.Fixity.INFIX) myBuilder.append('(');
    myBuilder.append(expr.getDefinition() == Prelude.ZERO ? "0" : expr.getDefinition().getName());
    if (expr.getDefinition().getFixity() == Abstract.Definition.Fixity.INFIX) myBuilder.append(')');
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
    if (prec > expr.getBinOp().getPrecedence().priority) myBuilder.append('(');

    if (expr.getLeft().isHidden()) {
      myBuilder.append('_');
    } else {
      expr.getLeft().getExpression().accept(this, (byte) (expr.getBinOp().getPrecedence().priority + (expr.getBinOp().getPrecedence().associativity == Definition.Associativity.LEFT_ASSOC ? 0 : 1)));
    }

    myBuilder.append(' ').append(expr.getBinOp().getName()).append(' ');

    if (expr.getRight().isHidden()) {
      myBuilder.append('_');
    } else {
      expr.getRight().getExpression().accept(this, (byte) (expr.getBinOp().getPrecedence().priority + (expr.getBinOp().getPrecedence().associativity == Definition.Associativity.RIGHT_ASSOC ? 0 : 1)));
    }

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
      prettyPrintClause(expr, clause, myBuilder, myNames, myIndent);
    }

    if (expr.getOtherwise() != null) {
      printIndent(myBuilder, myIndent);
      myBuilder.append("| _ ").append(expr.getOtherwise().getArrow() == Abstract.Definition.Arrow.LEFT ? "<= " : "=> ");
      expr.getOtherwise().getExpression().accept(this, Abstract.Expression.PREC);
      myBuilder.append('\n');
    }

    printIndent(myBuilder, myIndent);
    myBuilder.append(';');
    --myIndent;
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitFieldAcc(Abstract.FieldAccExpression expr, Byte prec) {
    if (prec > Abstract.FieldAccExpression.PREC) myBuilder.append('(');
    expr.getExpression().accept(this, Abstract.FieldAccExpression.PREC);
    myBuilder.append('.');
    if (expr.getFixity() == Abstract.Definition.Fixity.INFIX) {
      myBuilder.append('(');
    }
    myBuilder.append(expr.getName());
    if (expr.getFixity() == Abstract.Definition.Fixity.INFIX) {
      myBuilder.append(')');
    }
    if (prec > Abstract.FieldAccExpression.PREC) myBuilder.append(')');
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
    myBuilder.append(expr.getBaseClass().getName()).append(" {\n");
    ++myIndent;
    DefinitionPrettyPrintVisitor visitor = new DefinitionPrettyPrintVisitor(myBuilder, myNames, myIndent);
    for (Abstract.FunctionDefinition definition : expr.getDefinitions()) {
      visitor.visitFunction(definition, null);
      myBuilder.append("\n");
    }
    --myIndent;
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

  public static void printIndent(StringBuilder builder, int indent) {
    for (int i = 0; i < indent; ++i) {
      builder.append("    ");
    }
  }
}
