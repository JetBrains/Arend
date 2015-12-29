package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.pushArgument;

public abstract class Expression implements PrettyPrintable {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract Expression getType(List<Binding> context);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), (byte) 0);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new ToAbstractVisitor(new ConcreteExpressionFactory(), names), null).accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public String prettyPrint(List<String> names) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, names, Abstract.Expression.PREC);
    return sb.toString();
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(on), from);
  }

  public final Expression subst(Expression substExpr, int from) {
    List<Expression> substExprs = new ArrayList<>(1);
    substExprs.add(substExpr);
    return accept(new SubstVisitor(substExprs, from), null);
  }

  public final Expression subst(List<Expression> substExprs, int from) {
    return substExprs.isEmpty() ? this : accept(new SubstVisitor(substExprs, from), null);
  }

  public final Expression normalize(NormalizeVisitor.Mode mode, List<Binding> context) {
    return context == null ? this : accept(new NormalizeVisitor(context), mode);
  }

  public static CompareVisitor.Result oldCompare(Abstract.Expression expr1, Expression expr2, List<CompareVisitor.Equation> equations) {
    return expr1.accept(new CompareVisitor(equations), expr2);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return NewCompareVisitor.compare(DummyEquations.getInstance(), cmp, new ArrayList<Binding>(), expr1, expr2);
  }

  public static boolean compare(Expression expr1, Expression expr2) {
    return compare(expr1, expr2, Equations.CMP.EQ);
  }

  public Expression lamSplitAt(int index, List<Argument> arguments) {
    assert arguments.size() == 0;
    Expression result = this;

    while (arguments.size() < index) {
      if (result instanceof LamExpression) {
        LamExpression lamExpr = (LamExpression) result;
        TelescopeArgument additionalArgument = null;
        int i;
        for (i = 0; i < lamExpr.getArguments().size() && arguments.size() < index; ++i) {
          TelescopeArgument teleArg = lamExpr.getArguments().get(i);
          int j;
          for (j = 0; j < teleArg.getNames().size() && arguments.size() < index; ++j) {
            arguments.add(Tele(lamExpr.getArguments().get(i).getExplicit(), vars(teleArg.getNames().get(j)), teleArg.getType()));
          }
          if (j < teleArg.getNames().size()) {
            List<String> names = new ArrayList<>(teleArg.getNames().size() - j);
            for (; j < teleArg.getNames().size(); ++j) {
              names.add(teleArg.getNames().get(j));
            }
            additionalArgument = Tele(teleArg.getExplicit(), names, teleArg.getType());
          }
        }

        result = lamExpr.getBody();
        if (i < lamExpr.getArguments().size() || additionalArgument != null) {
          List<TelescopeArgument> arguments1 = new ArrayList<>(lamExpr.getArguments().size() - i + (additionalArgument == null ? 0 : 1));
          if (additionalArgument != null) {
            arguments1.add(additionalArgument);
          }
          for (; i < lamExpr.getArguments().size(); ++i) {
            arguments1.add(lamExpr.getArguments().get(i));
          }
          return Lam(arguments1, result);
        }
      } else {
        break;
      }
    }

    return result;
  }

  public Expression splitAt(int index, List<TypeArgument> arguments, List<Binding> context) {
    try (Utils.ContextSaver saver = context == null ? null : new Utils.ContextSaver(context)) {
      int count = 0;
      Expression type = this;
      while (count < index) {
        if (context != null) {
          type = type.normalize(NormalizeVisitor.Mode.WHNF, context);
        }
        if (type instanceof PiExpression) {
          PiExpression piType = (PiExpression) type;
          TelescopeArgument additionalArgument = null;
          int i;
          for (i = 0; i < piType.getArguments().size() && count < index; ++i) {
            if (piType.getArguments().get(i) instanceof TelescopeArgument) {
              TelescopeArgument teleArg = (TelescopeArgument) piType.getArguments().get(i);
              int j;
              for (j = 0; j < teleArg.getNames().size() && count < index; ++j) {
                if (arguments != null) {
                  arguments.add(Tele(piType.getArguments().get(i).getExplicit(), vars(teleArg.getNames().get(j)), teleArg.getType().liftIndex(0, j)));
                }
                ++count;
              }
              if (j < teleArg.getNames().size()) {
                List<String> names = new ArrayList<>(teleArg.getNames().size() - j);
                for (; j < teleArg.getNames().size(); ++j) {
                  names.add(teleArg.getNames().get(j));
                }
                additionalArgument = Tele(teleArg.getExplicit(), names, teleArg.getType().liftIndex(0, teleArg.getNames().size() - names.size()));
              }
            } else {
              if (arguments != null) {
                arguments.add(piType.getArguments().get(i));
              }
              ++count;
            }
            if (context != null)
              pushArgument(context, piType.getArguments().get(i));
          }

          type = piType.getCodomain();
          if (i < piType.getArguments().size() || additionalArgument != null) {
            List<TypeArgument> arguments1 = new ArrayList<>(piType.getArguments().size() - i + (additionalArgument == null ? 0 : 1));
            if (additionalArgument != null) {
              arguments1.add(additionalArgument);
            }
            for (; i < piType.getArguments().size(); ++i) {
              arguments1.add(piType.getArguments().get(i));
            }
            return Pi(arguments1, type);
          }
        } else {
          break;
        }
      }
      return type;
    }
  }

  public Expression getFunction(List<Expression> arguments) {
    Expression expr = this;
    while (expr instanceof AppExpression) {
      arguments.add(((AppExpression) expr).getArgument().getExpression());
      expr = ((AppExpression) expr).getFunction();
    }
    return expr;
  }

  public Expression getFunctionArgs(List<ArgumentExpression> arguments) {
    Expression expr = this;
    while (expr instanceof AppExpression) {
      arguments.add(((AppExpression) expr).getArgument());
      expr = ((AppExpression) expr).getFunction();
    }
    return expr;
  }
}
