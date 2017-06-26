package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExpressionFactory {
  public static FunCallExpression FunCall(FunctionDefinition definition, Sort sortArgument, Expression... arguments) {
    return new FunCallExpression(definition, sortArgument, Arrays.asList(arguments));
  }

  public static DataCallExpression DataCall(DataDefinition definition, Sort sortArgument, List<Expression> arguments) {
    return new DataCallExpression(definition, sortArgument, arguments);
  }

  public static DataCallExpression DataCall(DataDefinition definition, Sort sortArgument, Expression... arguments) {
    return new DataCallExpression(definition, sortArgument, Arrays.asList(arguments));
  }

  public static ClassCallExpression ClassCall(ClassDefinition definition) {
    return new ClassCallExpression(definition, Sort.STD);
  }

  public static ConCallExpression ConCall(Constructor definition, Sort sortArgument, List<Expression> dataTypeArguments, Expression... arguments) {
    return new ConCallExpression(definition, sortArgument, dataTypeArguments, Arrays.asList(arguments));
  }

  public static ReferenceExpression Ref(Binding binding) {
    return new ReferenceExpression(binding);
  }

  public static LamExpression Lam(SingleDependentLink link, Expression body) {
    return new LamExpression(Sort.SET0, link, body);
  }

  public static List<LetClause> lets(LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static LetClause let(String name, Expression expr) {
    return let(name, Collections.emptyList(), null, new LeafElimTreeNode(expr));
  }

  public static LetClause let(String name, SingleDependentLink param, Expression expr) {
    return let(name, Collections.singletonList(param), null, new LeafElimTreeNode(expr));
  }

  public static LetClause let(String name, SingleDependentLink param, Expression resultType, Expression expr) {
    return let(name, Collections.singletonList(param), resultType, new LeafElimTreeNode(expr));
  }

  public static LetClause let(String name, List<SingleDependentLink> params, Expression resultType, Expression expr) {
    return let(name, params, resultType, new LeafElimTreeNode(expr));
  }

  public static LetClause let(String name, List<SingleDependentLink> params, Expression resultType, ElimTreeNode elimTree) {
    return new LetClause(name, Collections.nCopies(params.size(), Sort.SET0), params, resultType == null ? null : new TypeExpression(resultType, Sort.SET0), elimTree);
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static DependentLink params(DependentLink... links) {
    for (int i = 0; i < links.length - 1; i++) {
      DependentLink.Helper.getLast(links[i]).setNext(links[i + 1]);
    }
    return links[0];
  }

  public static DependentLink param(String var, Expression type) {
    return new TypedDependentLink(true, var, new TypeExpression(type, Sort.SET0), EmptyDependentLink.getInstance());
  }

  public static DependentLink paramExpr(String var, Expression type) {
    return new TypedDependentLink(true, var, new TypeExpression(type, Sort.SET0), EmptyDependentLink.getInstance());
  }

  public static DependentLink paramExpr(Expression type) {
    return new TypedDependentLink(true, null, new TypeExpression(type, Sort.SET0), EmptyDependentLink.getInstance());
  }

  public static TypedSingleDependentLink singleParam(boolean explicit, String name, Expression type) {
    return new TypedSingleDependentLink(explicit, name, type instanceof Type ? (Type) type : new TypeExpression(type, Sort.SET0));
  }

  public static TypedSingleDependentLink singleParam(String name, Expression type) {
    return singleParam(true, name, type);
  }

  public static SingleDependentLink singleParam(boolean explicit, List<String> names, Expression type) {
    return com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.singleParams(explicit, names, type instanceof Type ? (Type) type : new TypeExpression(type, Sort.SET0));
  }

  public static PiExpression Pi(SingleDependentLink domain, Expression codomain) {
    assert domain.hasNext();
    return new PiExpression(Sort.SET0, domain, codomain);
  }

  public static PiExpression Pi(Expression domain, Expression codomain) {
    return new PiExpression(Sort.SET0, singleParam(null, domain), codomain);
  }

  public static UniverseExpression Universe(int pLevel) {
    return new UniverseExpression(Sort.TypeOfLevel(pLevel));
  }

  public static UniverseExpression Universe(int pLevel, int hLevel) {
    return new UniverseExpression(hLevel == -1 ? Sort.PROP : new Sort(pLevel, hLevel));
  }

  public static UniverseExpression Universe(Level pLevel, Level hLevel) {
    return new UniverseExpression(new Sort(pLevel, hLevel));
  }

  public static UniverseExpression Universe(Sort universe) {
    return new UniverseExpression(universe);
  }

  public static TupleExpression Tuple(SigmaExpression type, Expression... fields) {
    return new TupleExpression(Arrays.asList(fields), type);
  }

  public static Expression fromPiParameters(Expression expr, List<DependentLink> params) {
    List<SingleDependentLink> parameters = new ArrayList<>();
    ExprSubstitution substitution = new ExprSubstitution();
    List<String> names = new ArrayList<>();
    DependentLink link0 = null;
    for (DependentLink link : params) {
      if (link0 == null) {
        link0 = link;
      }

      names.add(link.getName());
      if (link instanceof TypedDependentLink) {
        SingleDependentLink parameter = singleParam(link.isExplicit(), names, link.getType().getExpr().subst(substitution, LevelSubstitution.EMPTY));
        parameters.add(parameter);
        names.clear();

        for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
          substitution.add(link0, new ReferenceExpression(parameter));
        }

        link0 = null;
      }
    }

    Expression type = expr.subst(substitution, LevelSubstitution.EMPTY);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      type = ExpressionFactory.Pi(parameters.get(i), type);
    }
    return type;
  }
}
