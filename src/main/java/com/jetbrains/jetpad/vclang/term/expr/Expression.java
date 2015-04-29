package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    List<CompareVisitor.Equation> result = compare(this, (Expression) obj, CompareVisitor.CMP.EQ);
    return result != null && result.size() == 0;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    return accept(new SubstVisitor(substExpr, from));
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(mode));
  }

  public final CheckTypeVisitor.OKResult checkType(Map<String, Definition> globalContext, List<Binding> localContext, Expression expectedType, List<TypeCheckingError> errors) {
    return new CheckTypeVisitor(globalContext, localContext, errors).checkType(this, expectedType);
  }

  public static List<CompareVisitor.Equation> compare(Abstract.Expression expr1, Expression expr2, CompareVisitor.CMP cmp) {
    CompareVisitor visitor = new CompareVisitor(cmp, new ArrayList<CompareVisitor.Equation>());
    Boolean result = expr1.accept(visitor, expr2);
    return result ? visitor.equations() : null;
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
          if (piType.getArgument(i) instanceof TelescopeArgument) {
            TelescopeArgument teleArg = (TelescopeArgument) piType.getArgument(i);
            int j;
            for (j = 0; j < teleArg.getNames().size() && arguments.size() < index; ++j) {
              arguments.add(Tele(piType.getArgument(i).getExplicit(), vars(teleArg.getName(j)), piType.getArgument(i).getType()));
            }
            if (j < teleArg.getNames().size()) {
              List<String> names = new ArrayList<>(teleArg.getNames().size() - j);
              for (; j < teleArg.getNames().size(); ++j) {
                names.add(teleArg.getName(j));
              }
              additionalArgument = Tele(teleArg.getExplicit(), names, teleArg.getType());
            }
          } else {
            arguments.add(piType.getArgument(i));
          }
        }
        type = piType.getCodomain();
        if (i < piType.getArguments().size() || additionalArgument != null) {
          List<TypeArgument> arguments1 = new ArrayList<>(piType.getArguments().size() - i + (additionalArgument == null ? 0 : 1));
          if (additionalArgument != null) {
            arguments1.add(additionalArgument);
          }
          for (; i < piType.getArguments().size(); ++i) {
            arguments1.add(piType.getArgument(i));
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
