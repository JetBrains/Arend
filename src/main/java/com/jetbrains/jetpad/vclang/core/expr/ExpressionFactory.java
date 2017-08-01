package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.Collections;
import java.util.List;

public class ExpressionFactory {
  public static Expression Apps(Expression function, Expression... arguments) {
    if (arguments.length == 0) {
      return function;
    }
    Expression result = function;
    for (Expression argument : arguments) {
      result = new AppExpression(result, argument);
    }
    return result;
  }

  public static Expression FieldCall(ClassField definition, Expression thisExpr) {
    if (thisExpr.isInstance(NewExpression.class)) {
      FieldSet.Implementation impl = thisExpr.cast(NewExpression.class).getExpression().getFieldSet().getImplementation(definition);
      assert impl != null;
      return impl.term;
    } else {
      ErrorExpression errorExpr = thisExpr.checkedCast(ErrorExpression.class);
      if (errorExpr != null && errorExpr.getExpression() != null) {
        return new FieldCallExpression(definition, new ErrorExpression(null, errorExpr.getError()));
      } else {
        return new FieldCallExpression(definition, thisExpr);
      }
    }
  }

  public static DataCallExpression Interval() {
    return new DataCallExpression(Prelude.INTERVAL, Sort.PROP, Collections.emptyList());
  }

  public static ConCallExpression Left() {
    return new ConCallExpression(Prelude.LEFT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Right() {
    return new ConCallExpression(Prelude.RIGHT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static DependentLink parameter(boolean explicit, String var, Type type) {
    return new TypedDependentLink(explicit, var, type, EmptyDependentLink.getInstance());
  }

  public static TypedDependentLink parameter(String var, Type type) {
    return new TypedDependentLink(true, var, type, EmptyDependentLink.getInstance());
  }

  public static DependentLink parameter(boolean explicit, List<String> names, Type type) {
    DependentLink link = new TypedDependentLink(explicit, names.get(names.size() - 1), type, EmptyDependentLink.getInstance());
    for (int i = names.size() - 2; i >= 0; i--) {
      link = new UntypedDependentLink(names.get(i), link);
    }
    return link;
  }

  public static SingleDependentLink singleParams(boolean explicit, List<String> names, Type type) {
    SingleDependentLink link = new TypedSingleDependentLink(explicit, names.get(names.size() - 1), type);
    for (int i = names.size() - 2; i >= 0; i--) {
      link = new UntypedSingleDependentLink(names.get(i), link);
    }
    return link;
  }

  public static DataCallExpression Nat() {
    return new DataCallExpression(Prelude.NAT, Sort.SET0, Collections.emptyList());
  }

  public static ConCallExpression Zero() {
    return new ConCallExpression(Prelude.ZERO, Sort.SET0, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Suc(Expression expr) {
    return new ConCallExpression(Prelude.SUC, Sort.SET0, Collections.emptyList(), Collections.singletonList(expr));
  }
}
