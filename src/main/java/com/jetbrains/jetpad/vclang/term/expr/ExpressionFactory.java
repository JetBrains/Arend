package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.NonDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.*;

public class ExpressionFactory {
  public static Expression Apps(Expression expr, Expression... exprs) {
    for (Expression expr1 : exprs) {
      expr = new AppExpression(expr, new ArgumentExpression(expr1, true, false));
    }
    return expr;
  }

  public static Expression Apps(Expression expr, ArgumentExpression... exprs) {
    for (ArgumentExpression expr1 : exprs) {
      expr = new AppExpression(expr, expr1);
    }
    return expr;
  }

  public static Expression Apps(Expression expr, Expression arg, boolean explicit, boolean hidden) {
    return new AppExpression(expr, new ArgumentExpression(arg, explicit, hidden));
  }

  public static FunCallExpression FunCall(FunctionDefinition definition) {
    return new FunCallExpression(definition);
  }

  public static DataCallExpression DataCall(DataDefinition definition) {
    return new DataCallExpression(definition);
  }

  public static FieldCallExpression FieldCall(ClassField definition) {
    return new FieldCallExpression(definition);
  }

  public static ClassCallExpression ClassCall(ClassDefinition definition) {
    return new ClassCallExpression(definition);
  }

  public static ClassCallExpression ClassCall(ClassDefinition definition, Map<ClassField, ClassCallExpression.ImplementStatement> statements) {
    return new ClassCallExpression(definition, statements);
  }

  public static ConCallExpression ConCall(Constructor definition, List<Expression> parameters) {
    return new ConCallExpression(definition, parameters);
  }

  public static ConCallExpression ConCall(Constructor definition) {
    return new ConCallExpression(definition, definition.getDataType().getNumberOfAllParameters() == 0 ? Collections.<Expression>emptyList() : new ArrayList<Expression>(definition.getDataType().getNumberOfAllParameters()));
  }

  public static ConCallExpression ConCall(Constructor definition, Expression... parameters) {
    return ConCall(definition, Arrays.asList(parameters));
  }

  public static Expression BinOp(Expression left, Definition binOp, Expression right) {
    return Apps(binOp.getDefCall(), left, right);
  }

  public static NewExpression New(Expression expression) {
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
    return let(name, null, elimTree);
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
      assert links[i].getNext() == null;
      links[i].setNext(links[i + 1]);
    }
    return links[0];
  }

  public static DependentLink param(boolean explicit, String var, Expression type) {
    return new TypedDependentLink(explicit, var, type, null);
  }

  public static DependentLink param(String var, Expression type) {
    return new TypedDependentLink(true, var, type, null);
  }

  public static DependentLink param(Expression type) {
    return new NonDependentLink(type, null);
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static DependentLink param(boolean explicit, List<String> names, Expression type) {
    DependentLink link = new TypedDependentLink(explicit, names.get(names.size() - 1), type, null);
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
    return new PiExpression(domain, codomain);
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
    return DataCall(Prelude.NAT);
  }

  public static ConCallExpression Zero() {
    return ConCall(Prelude.ZERO);
  }

  public static ConCallExpression Suc() {
    return ConCall(Prelude.SUC);
  }

  public static Expression Suc(Expression expr) {
    return Apps(Suc(), expr);
  }

  public static UniverseExpression Universe() {
    return new UniverseExpression(new Universe.Type());
  }

  public static UniverseExpression Universe(int level) {
    return new UniverseExpression(new Universe.Type(level));
  }

  public static UniverseExpression Universe(int level, int truncated) {
    return new UniverseExpression(new Universe.Type(level, truncated));
  }

  public static ErrorExpression Error(Expression expr, TypeCheckingError error) {
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

  public static BranchElimTreeNode branch(Binding reference, List<Binding> tail, ConstructorClausePair... clauses) {
    BranchElimTreeNode result = new BranchElimTreeNode(reference, tail);
    for (ConstructorClausePair pair : clauses) {
      ConstructorClause clause = result.addClause(pair.constructor);
      Substitution subst = clause.getSubst();
      subst.getDomain().remove(reference);
      for (DependentLink linkFake = pair.parameters, linkTrue = clause.getParameters();
           linkFake != null; linkFake = linkFake.getNext(), linkTrue = linkTrue.getNext()) {
        subst.addMapping(linkFake, Reference(linkTrue));
      }
      clause.setChild(pair.child.subst(subst));
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
