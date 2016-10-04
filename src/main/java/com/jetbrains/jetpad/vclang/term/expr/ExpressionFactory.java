package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.*;

public class ExpressionFactory {
  public static Expression Apps(Expression function, Expression... arguments) {
    return arguments.length == 0 ? function : new AppExpression(function, new ArrayList<>(Arrays.asList(arguments)));
  }

  public static Expression Apps(Expression fun, Collection<? extends Expression> arguments) {
    return arguments.isEmpty() ? fun : new AppExpression(fun, arguments);
  }

  public static FunCallExpression FunCall(FunctionDefinition definition, List<Expression> arguments) {
    return new FunCallExpression(definition, arguments);
  }

  public static FunCallExpression FunCall(FunctionDefinition definition, Expression... arguments) {
    return FunCall(definition, Arrays.asList(arguments));
  }

  public static FunCallExpression FunCall(FunctionDefinition definition, Level lp, Level lh, List<Expression> arguments) {
    FunCallExpression expr = new FunCallExpression(definition, arguments);
    expr.setPolyParamsSubst(new LevelSubstitution(definition.getPolyParams().get(0), lp, definition.getPolyParams().get(1), lh));
    return expr;
  }

  public static FunCallExpression FunCall(FunctionDefinition definition, Level lp, Level lh, Expression... arguments) {
    return FunCall(definition, lp, lh, Arrays.asList(arguments));
  }

  public static DataCallExpression DataCall(DataDefinition definition, List<Expression> arguments) {
    return new DataCallExpression(definition, arguments);
  }

  public static DataCallExpression DataCall(DataDefinition definition, Expression... arguments) {
    return DataCall(definition, Arrays.asList(arguments));
  }

  public static DataCallExpression DataCall(DataDefinition definition, Level lp, Level lh, List<Expression> arguments) {
    DataCallExpression expr = new DataCallExpression(definition, arguments);
    expr.setPolyParamsSubst(new LevelSubstitution(definition.getPolyParams().get(0), lp, definition.getPolyParams().get(1), lh));
    return expr;
  }

  public static DataCallExpression DataCall(DataDefinition definition, Level lp, Level lh, Expression... arguments) {
    return DataCall(definition, lp, lh, Arrays.asList(arguments));
  }

  public static FieldCallExpression FieldCall(ClassField definition, Expression thisExpr) {
    return new FieldCallExpression(definition, thisExpr);
  }

  public static ClassCallExpression ClassCall(ClassDefinition definition) {
    return new ClassCallExpression(definition);
  }

  public static ClassCallExpression ClassCall(ClassDefinition definition, FieldSet fieldSet) {
    return new ClassCallExpression(definition, fieldSet);
   }

  public static ConCallExpression ConCall(Constructor definition, List<Expression> parameters, List<Expression> arguments) {
    return new ConCallExpression(definition, parameters, arguments);
  }

  public static ConCallExpression ConCall(Constructor definition, List<Expression> dataTypeArguments, Expression... arguments) {
    return ConCall(definition, dataTypeArguments, Arrays.asList(arguments));
  }

  public static ConCallExpression ConCall(Constructor definition, Level lp, Level lh, List<Expression> dataTypeArguments, List<Expression> arguments) {
    ConCallExpression expr = new ConCallExpression(definition, dataTypeArguments, arguments);
    expr.setPolyParamsSubst(new LevelSubstitution(definition.getPolyParams().get(0), lp, definition.getPolyParams().get(1), lh));
    return expr;
  }

  public static ConCallExpression ConCall(Constructor definition, Level lp, Level lh, List<Expression> dataTypeArguments, Expression... arguments) {
    return ConCall(definition, lp, lh, dataTypeArguments, Arrays.asList(arguments));
  }

  public static DataCallExpression Interval() {
    return DataCall(Prelude.INTERVAL, Collections.<Expression>emptyList());
  }

  public static ConCallExpression Left() {
    return ConCall(Prelude.LEFT, Collections.<Expression>emptyList(), Collections.<Expression>emptyList());
  }

  public static ConCallExpression Right() {
    return ConCall(Prelude.RIGHT, Collections.<Expression>emptyList(), Collections.<Expression>emptyList());
  }

  public static NewExpression New(ClassCallExpression expression) {
    return new NewExpression(expression);
  }

  public static ReferenceExpression Reference(Binding binding) {
    return new ReferenceExpression(binding);
  }

  public static LamExpression Lam(DependentLink link, Expression body) {
    return new LamExpression(link, body);
  }

  public static LetExpression Let(List<LetClause> clauses, Expression expr) {
    return new LetExpression(clauses, expr);
  }

  public static List<LetClause> lets(LetClause... letClauses) {
    return Arrays.asList(letClauses);
  }

  public static LetClause let(String name, Expression expr) {
    return let(name, leaf(expr));
  }

  public static LetClause let(String name, ElimTreeNode elimTree) {
    return let(name, EmptyDependentLink.getInstance(), elimTree);
  }

  public static LetClause let(String name, DependentLink params, Expression expr) {
    return let(name, params, leaf(expr));
  }

  public static LetClause let(String name, DependentLink params, ElimTreeNode elimTree) {
    return let(name, params, null, elimTree);
  }

  public static LetClause let(String name, DependentLink params, Expression resultType, Abstract.Definition.Arrow arrow, Expression expr) {
    return let(name, params, resultType, leaf(arrow, expr));
  }

  public static LetClause let(String name, DependentLink params, Expression resultType, Expression expr) {
    return let(name, params, resultType, leaf(expr));
  }

  public static LetClause let(String name, DependentLink params, Expression resultType, ElimTreeNode elimTree) {
    return new LetClause(name, params, resultType, elimTree);
  }

  public static DependentLink params(DependentLink... links) {
    for (int i = 0; i < links.length - 1; i++) {
      links[i].setNext(links[i + 1]);
    }
    return links[0];
  }

  public static DependentLink param(boolean explicit, String var, Expression type) {
    return new TypedDependentLink(explicit, var, type, EmptyDependentLink.getInstance());
  }

  public static DependentLink param(String var, Expression type) {
    return new TypedDependentLink(true, var, type, EmptyDependentLink.getInstance());
  }

  public static DependentLink param(Expression type) {
    return new TypedDependentLink(true, null, type, EmptyDependentLink.getInstance());
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static DependentLink param(boolean explicit, List<String> names, Expression type) {
    DependentLink link = new TypedDependentLink(explicit, names.get(names.size() - 1), type, EmptyDependentLink.getInstance());
    for (int i = names.size() - 2; i >= 0; i--) {
      link = new UntypedDependentLink(names.get(i), link);
    }
    return link;
  }

  public static DependentLink param(Abstract.Argument argument, Expression type) {
    if (argument instanceof Abstract.TelescopeArgument) {
      return param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), type);
    } else {
      return param(type);
    }
  }

  public static PiExpression Pi(DependentLink domain, Expression codomain) {
    assert domain.hasNext();
    return new PiExpression(domain, codomain);
  }

  public static PiExpression Pi(Expression domain, Expression codomain) {
    return new PiExpression(param(domain), codomain);
  }

  public static SigmaExpression Sigma(DependentLink domain) {
    return new SigmaExpression(domain);
  }

  public static TupleExpression Tuple(List<Expression> fields, SigmaExpression type) {
    return new TupleExpression(fields, type);
  }

  public static TupleExpression Tuple(SigmaExpression type, Expression... fields) {
    return new TupleExpression(Arrays.asList(fields), type);
  }

  public static ProjExpression Proj(Expression expr, int field) {
    return new ProjExpression(expr, field);
  }

  public static DataCallExpression Nat() {
    return DataCall(Prelude.NAT, Collections.<Expression>emptyList());
  }

  public static ConCallExpression Zero() {
    return ConCall(Prelude.ZERO, Collections.<Expression>emptyList(), Collections.<Expression>emptyList());
  }

  public static ConCallExpression Suc(Expression expr) {
    return ConCall(Prelude.SUC, Collections.<Expression>emptyList(), Collections.singletonList(expr));
  }

  public static DataCallExpression Lvl() {
    return DataCall(Prelude.LVL, Collections.<Expression>emptyList());
  }

  public static DataCallExpression CNat() {
    return DataCall(Prelude.CNAT, Collections.<Expression>emptyList());
  }

  public static UniverseExpression Universe() {
    return new UniverseExpression(new Sort(Sort.ANY_LEVEL, Sort.ANY_LEVEL));
  }

  public static UniverseExpression Universe(int plevel) {
    return new UniverseExpression(new Sort(plevel, Sort.NOT_TRUNCATED));
  }

  public static UniverseExpression Universe(int plevel, int hlevel) {
    return new UniverseExpression(new Sort(plevel, hlevel));
  }

  public static UniverseExpression Universe(Level plevel, Level hlevel) {
    return new UniverseExpression(new Sort(plevel, hlevel));
  }

  public static UniverseExpression Universe(Sort universe) {
    return new UniverseExpression(universe);
  }

  public static ErrorExpression Error(Expression expr, LocalTypeCheckingError error) {
    return new ErrorExpression(expr, error);
  }

  public static PatternArgument match(boolean isExplicit, Constructor constructor, PatternArgument... patternArgs) {
    return new PatternArgument(new ConstructorPattern(constructor, new Patterns(Arrays.asList(patternArgs))), isExplicit, false);
  }

  public static PatternArgument match(Constructor constructor, PatternArgument... patterns) {
    return match(true, constructor, patterns);
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
    tree.updateLeavesMatched(toContext(parameters));
    return tree;
  }

  public static BranchElimTreeNode branch(Binding reference, List<Binding> tail, ConstructorClausePair... clauses) {
    BranchElimTreeNode result = new BranchElimTreeNode(reference, tail);
    for (ConstructorClausePair pair : clauses) {
      if (pair.constructor != null) {
        ConstructorClause clause = result.addClause(pair.constructor, toNames(pair.parameters));
        ExprSubstitution subst = clause.getSubst();
        assert size(pair.constructor.getParameters()) == size(pair.parameters);
        for (DependentLink linkFake = pair.parameters, linkTrue = clause.getParameters();
             linkFake.hasNext(); linkFake = linkFake.getNext(), linkTrue = linkTrue.getNext()) {
          subst.add(linkFake, Reference(linkTrue));
        }
        clause.setChild(pair.child.subst(subst));
      } else {
        OtherwiseClause clause = result.addOtherwiseClause();
        clause.setChild(pair.child);
      }
    }
    return result;
  }

  public static LeafElimTreeNode leaf(Expression expression) {
    return new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, expression);
  }

  public static LeafElimTreeNode leaf(Abstract.Definition.Arrow arrow, Expression expression) {
    return new LeafElimTreeNode(arrow, expression);
  }

  public static ConstructorClausePair clause(Constructor constructor, DependentLink parameters, BranchElimTreeNode node) {
    return new ConstructorClausePair(constructor, parameters, node);
  }

  public static ConstructorClausePair clause(ElimTreeNode node) {
    return new ConstructorClausePair(null, null, node);
  }

  public static ConstructorClausePair clause(Expression expr) {
    return clause(leaf(expr));
  }

  public static ConstructorClausePair clause(Constructor constructor, DependentLink parameters, Abstract.Definition.Arrow arrow, Expression expr) {
    return new ConstructorClausePair(constructor, parameters, leaf(arrow, expr));
  }

  public static ConstructorClausePair clause(Constructor constructor, DependentLink parameters, Expression expr) {
    return new ConstructorClausePair(constructor, parameters, leaf(Abstract.Definition.Arrow.RIGHT, expr));
  }

  public static PatternArgument match(boolean isExplicit, DependentLink link) {
    return new PatternArgument(new NamePattern(link), isExplicit, false);
  }

  public static PatternArgument match(DependentLink link) {
    return match(true, link);
  }
}
