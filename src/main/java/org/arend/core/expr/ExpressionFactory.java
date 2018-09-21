package org.arend.core.expr;

import org.arend.core.context.param.*;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;

import java.util.Collections;
import java.util.List;

public class ExpressionFactory {
  public static Expression Apps(Expression function, Expression... arguments) {
    if (arguments.length == 0) {
      return function;
    }
    Expression result = function;
    for (Expression argument : arguments) {
      result = AppExpression.make(result, argument);
    }
    return result;
  }

  public static Expression FieldCall(ClassField definition, Sort sortArgument, Expression thisExpr) {
    return FieldCallExpression.make(definition, sortArgument, thisExpr);
  }

  public static DataCallExpression Interval() {
    return new DataCallExpression(Prelude.INTERVAL, Sort.PROP, Collections.emptyList());
  }

  public static ConCallExpression Left() {
    return (ConCallExpression) ConCallExpression.make(Prelude.LEFT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Right() {
    return (ConCallExpression) ConCallExpression.make(Prelude.RIGHT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
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

  public static DataCallExpression Int() {
    return new DataCallExpression(Prelude.INT, Sort.SET0, Collections.emptyList());
  }

  public static IntegerExpression Zero() {
    return new SmallIntegerExpression(0);
  }

  public static Expression Suc(Expression expr) {
    return ConCallExpression.make(Prelude.SUC, Sort.SET0, Collections.emptyList(), Collections.singletonList(expr));
  }

  public static Expression Pos(Expression expr) {
    return ConCallExpression.make(Prelude.POS, Sort.SET0, Collections.emptyList(), Collections.singletonList(expr));
  }

  public static Expression Neg(Expression expr) {
    return ConCallExpression.make(Prelude.NEG, Sort.SET0, Collections.emptyList(), Collections.singletonList(expr));
  }
}
