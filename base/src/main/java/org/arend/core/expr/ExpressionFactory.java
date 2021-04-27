package org.arend.core.expr;

import org.arend.core.context.param.*;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.type.Type;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.prelude.Prelude;
import org.arend.util.SingletonList;

import java.util.Collections;
import java.util.List;

public class ExpressionFactory {
  public static Expression FieldCall(ClassField definition, LevelPair levels, Expression thisExpr) {
    return FieldCallExpression.make(definition, levels, thisExpr);
  }

  public static DataCallExpression Interval() {
    return new DataCallExpression(Prelude.INTERVAL, LevelPair.PROP, Collections.emptyList());
  }

  public static ConCallExpression Left() {
    return (ConCallExpression) ConCallExpression.make(Prelude.LEFT, LevelPair.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Right() {
    return (ConCallExpression) ConCallExpression.make(Prelude.RIGHT, LevelPair.PROP, Collections.emptyList(), Collections.emptyList());
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
    return new DataCallExpression(Prelude.NAT, LevelPair.PROP, Collections.emptyList());
  }

  public static DataCallExpression Fin(int cardinality) {
    return Fin(new SmallIntegerExpression(cardinality));
  }

  public static DataCallExpression Fin(Expression cardinality) {
    return new DataCallExpression(Prelude.FIN, LevelPair.PROP, new SingletonList<>(cardinality));
  }

  public static SigmaExpression divModType(Type type) {
    return new SigmaExpression(Sort.SET0, new TypedDependentLink(true, null, Nat(), new TypedDependentLink(true, null, type, EmptyDependentLink.getInstance())));
  }

  public static SigmaExpression finDivModType(Expression expr) {
    return divModType(Fin(expr));
  }

  public static DataCallExpression Int() {
    return new DataCallExpression(Prelude.INT, LevelPair.PROP, Collections.emptyList());
  }

  public static IntegerExpression Zero() {
    return new SmallIntegerExpression(0);
  }

  public static Expression Suc(Expression expr) {
    return ConCallExpression.make(Prelude.SUC, LevelPair.PROP, Collections.emptyList(), new SingletonList<>(expr));
  }

  public static Expression add(Expression expr, int n) {
    for (int i = 0; i < n; i++) {
      expr = Suc(expr);
    }
    return expr;
  }

  public static ConCallExpression Pos(Expression expr) {
    return ConCallExpression.makeConCall(Prelude.POS, LevelPair.PROP, Collections.emptyList(), new SingletonList<>(expr));
  }

  public static ConCallExpression Neg(Expression expr) {
    return ConCallExpression.makeConCall(Prelude.NEG, LevelPair.PROP, Collections.emptyList(), new SingletonList<>(expr));
  }
}
