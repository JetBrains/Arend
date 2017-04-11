package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.Arrays;
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
    if (thisExpr.toNew() != null) {
      FieldSet.Implementation impl = thisExpr.toNew().getExpression().getFieldSet().getImplementation(definition);
      assert impl != null;
      return impl.term;
    } else {
      return new FieldCallExpression(definition, thisExpr);
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

  public static class ConstructorClausePair {
    private final Constructor constructor;
    private final DependentLink parameters;
    private final ElimTreeNode child;

    private ConstructorClausePair(Constructor constructor, DependentLink parameters, ElimTreeNode child) {
      this.constructor = constructor;
      this.parameters = parameters;
      this.child = child;
    }
  }

  public static List<Binding> tail(Binding... bindings) {
    return Arrays.asList(bindings);
  }

  public static ElimTreeNode top(DependentLink parameters, ElimTreeNode tree) {
    tree.updateLeavesMatched(DependentLink.Helper.toContext(parameters));
    return tree;
  }

  public static ElimTreeNode top(List<SingleDependentLink> parameters, ElimTreeNode tree) {
    List<Binding> context = new ArrayList<>();
    for (SingleDependentLink link : parameters) {
      context.addAll(DependentLink.Helper.toContext(link));
    }
    tree.updateLeavesMatched(context);
    return tree;
  }

  public static BranchElimTreeNode branch(Binding reference, List<Binding> tail, ConstructorClausePair... clauses) {
    BranchElimTreeNode result = new BranchElimTreeNode(reference, tail);
    for (ConstructorClausePair pair : clauses) {
      if (pair.constructor != null) {
        ConstructorClause clause = result.addClause(pair.constructor, DependentLink.Helper.toNames(pair.parameters));
        ExprSubstitution subst = clause.getSubst();
        assert DependentLink.Helper.size(pair.constructor.getParameters()) == DependentLink.Helper.size(pair.parameters);
        for (DependentLink linkFake = pair.parameters, linkTrue = clause.getParameters();
             linkFake.hasNext(); linkFake = linkFake.getNext(), linkTrue = linkTrue.getNext()) {
          subst.add(linkFake, new ReferenceExpression(linkTrue));
        }
        clause.setChild(pair.child.subst(subst));
      } else {
        OtherwiseClause clause = result.addOtherwiseClause();
        clause.setChild(pair.child);
      }
    }
    return result;
  }

  public static ConstructorClausePair clause(ElimTreeNode node) {
    return new ConstructorClausePair(null, null, node);
  }

  public static ConstructorClausePair clause(Expression expr) {
    return clause(new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, expr));
  }

  public static ConstructorClausePair clause(Constructor constructor, DependentLink parameters, Abstract.Definition.Arrow arrow, Expression expr) {
    return new ConstructorClausePair(constructor, parameters, new LeafElimTreeNode(arrow, expr));
  }

  public static ConstructorClausePair clause(Constructor constructor, DependentLink parameters, Expression expr) {
    return new ConstructorClausePair(constructor, parameters, new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, expr));
  }
}
