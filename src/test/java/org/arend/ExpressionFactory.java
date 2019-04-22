package org.arend;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.*;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.NameLetClausePattern;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;

import java.util.ArrayList;
import java.util.Arrays;
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

  public static Expression ConCall(Constructor definition, Sort sortArgument, List<Expression> dataTypeArguments, Expression... arguments) {
    return ConCallExpression.make(definition, sortArgument, dataTypeArguments, Arrays.asList(arguments));
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

  public static LetClause let(String name, Expression expression) {
    return new LetClause(name, new NameLetClausePattern(name), expression);
  }

  public static LetExpression let(List<LetClause> clauses, Expression expression) {
    return new LetExpression(false, clauses, expression);
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

  public static DependentLink paramExpr(@SuppressWarnings("SameParameterValue") String var, Expression type) {
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
    return org.arend.core.expr.ExpressionFactory.singleParams(explicit, names, type instanceof Type ? (Type) type : new TypeExpression(type, Sort.SET0));
  }

  private static Sort getMaxSort(Expression type1, Expression type2) {
    Expression uni1 = type1.getType();
    Expression uni2 = type2.getType();
    Sort sort1 = uni1 == null ? null : uni1.toSort();
    Sort sort2 = uni2 == null ? null : uni2.toSort();
    return sort1 == null || sort2 == null ? Sort.SET0 : sort1.max(sort2);
  }

  public static PiExpression Pi(SingleDependentLink domain, Expression codomain) {
    assert domain.hasNext();
    return new PiExpression(getMaxSort(domain.getTypeExpr(), codomain.getType()), domain, codomain);
  }

  public static PiExpression Pi(Expression domain, Expression codomain) {
    return new PiExpression(getMaxSort(domain.getType(), codomain.getType()), singleParam(null, domain), codomain);
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
        SingleDependentLink parameter = singleParam(link.isExplicit(), names, link.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY));
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
