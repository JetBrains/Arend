package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public abstract class Expression implements PrettyPrintable, Abstract.Expression {
  public abstract <T> T accept(ExpressionVisitor<? extends T> visitor);

  @Override
  public void setWellTyped(Expression wellTyped) {}

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new PrettyPrintVisitor(builder, new ArrayList<String>(), 0), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Expression)) return false;
    List<CompareVisitor.Equation> equations = new ArrayList<>(0);
    CompareVisitor.Result result = compare(this, (Expression) obj, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    List<Expression> substExprs = new ArrayList<>(1);
    substExprs.add(substExpr);
    return accept(new SubstVisitor(substExprs, from));
  }

  public final Expression subst(List<Expression> substExprs, int from) {
    return substExprs.isEmpty() ? this : accept(new SubstVisitor(substExprs, from));
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(mode));
  }

  public final CheckTypeVisitor.OKResult checkType(List<Binding> localContext, Expression expectedType, List<TypeCheckingError> errors) {
    return new CheckTypeVisitor(null, localContext, new ArrayList<Definition>(0), errors, CheckTypeVisitor.Side.LHS).checkType(this, expectedType);
  }

  public static CompareVisitor.Result compare(Abstract.Expression expr1, Expression expr2, List<CompareVisitor.Equation> equations) {
    return expr1.accept(new CompareVisitor(equations), expr2);
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
          if (lamExpr.getArguments().get(i) instanceof TelescopeArgument) {
            TelescopeArgument teleArg = (TelescopeArgument) lamExpr.getArguments().get(i);
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
          } else {
            arguments.add(lamExpr.getArguments().get(i));
          }
        }

        result = lamExpr.getBody();
        if (i < lamExpr.getArguments().size() || additionalArgument != null) {
          List<Argument> arguments1 = new ArrayList<>(lamExpr.getArguments().size() - i + (additionalArgument == null ? 0 : 1));
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

  public Expression splitAt(int index, List<TypeArgument> arguments) {
    assert arguments.size() == 0;
    Expression type = this;
    while (arguments.size() < index) {
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
      if (type instanceof PiExpression) {
        PiExpression piType = (PiExpression) type;
        TelescopeArgument additionalArgument = null;
        int i;
        for (i = 0; i < piType.getArguments().size() && arguments.size() < index; ++i) {
          if (piType.getArguments().get(i) instanceof TelescopeArgument) {
            TelescopeArgument teleArg = (TelescopeArgument) piType.getArguments().get(i);
            int j;
            for (j = 0; j < teleArg.getNames().size() && arguments.size() < index; ++j) {
              arguments.add(Tele(piType.getArguments().get(i).getExplicit(), vars(teleArg.getNames().get(j)), teleArg.getType().liftIndex(0, j)));
            }
            if (j < teleArg.getNames().size()) {
              List<String> names = new ArrayList<>(teleArg.getNames().size() - j);
              for (; j < teleArg.getNames().size(); ++j) {
                names.add(teleArg.getNames().get(j));
              }
              additionalArgument = Tele(teleArg.getExplicit(), names, teleArg.getType());
            }
          } else {
            arguments.add(piType.getArguments().get(i));
          }
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
