package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
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

  public static IndexExpression Index(int i) {
    return new IndexExpression(i);
  }

  public static LamExpression Lam(List<TelescopeArgument> arguments, Expression body) {
    return new LamExpression(arguments, body);
  }

  public static LamExpression Lam(String var, Expression type, Expression body) {
    List<TelescopeArgument> arguments = new ArrayList<>(1);
    arguments.add(new TelescopeArgument(true, vars(var), type));
    return Lam(arguments, body);
  }

  public static VarExpression Var(String name) {
    return new VarExpression(name);
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
    return let(name, typeArgs(), elimTree);
  }

  public static LetClause let(String name, List<TypeArgument> args, Expression expr) {
    return let(name, args, leaf(expr));
  }

  public static LetClause let(String name, List<TypeArgument> args, ElimTreeNode elimTree) {
    return let(name, args, null, elimTree);
  }

  public static LetClause let(String name, List<TypeArgument> args, Expression resultType, Abstract.Definition.Arrow arrow, Expression expr) {
    return let(name, args, resultType, leaf(arrow, expr));
  }

  public static LetClause let(String name, List<TypeArgument> args, Expression resultType, Expression expr) {
    return let(name, args, resultType, leaf(expr));
  }

  public static LetClause let(String name, List<TypeArgument> args, Expression resultType, ElimTreeNode elimTree) {
    return new LetClause(name, args, resultType, elimTree);
  }

  public static List<String> vars(String... vars) {
    return Arrays.asList(vars);
  }

  public static List<TypeArgument> typeArgs(TypeArgument... args) {
    return Arrays.asList(args);
  }

  public static List<TelescopeArgument> teleArgs(TelescopeArgument... args) {
    return Arrays.asList(args);
  }

  public static List<Argument> args(Argument... args) {
    return Arrays.asList(args);
  }

  public static NameArgument Name(boolean explicit, String name) {
    return new NameArgument(explicit, name);
  }

  public static NameArgument Name(String name) {
    return new NameArgument(true, name);
  }

  public static TypeArgument TypeArg(boolean explicit, Expression type) {
    return new TypeArgument(explicit, type);
  }

  public static TypeArgument TypeArg(Expression type) {
    return new TypeArgument(true, type);
  }

  public static TelescopeArgument Tele(boolean explicit, List<String> names, Expression type) {
    return new TelescopeArgument(explicit, names, type);
  }

  public static TelescopeArgument Tele(List<String> names, Expression type) {
    return new TelescopeArgument(true, names, type);
  }

  public static Expression Pi(List<TypeArgument> arguments, Expression codomain) {
    return arguments.isEmpty() ? codomain : new PiExpression(arguments, codomain);
  }

  public static PiExpression Pi(boolean explicit, String var, Expression domain, Expression codomain) {
    List<TypeArgument> arguments = new ArrayList<>(1);
    List<String> vars = new ArrayList<>(1);
    vars.add(var);
    arguments.add(new TelescopeArgument(explicit, vars, domain));
    return new PiExpression(arguments, codomain);
  }

  public static PiExpression Pi(String var, Expression domain, Expression codomain) {
    return Pi(true, var, domain, codomain);
  }

  public static PiExpression Pi(Expression domain, Expression codomain) {
    return new PiExpression(domain, codomain);
  }

  public static SigmaExpression Sigma(List<TypeArgument> arguments) {
    return new SigmaExpression(arguments);
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
    return new PatternArgument(new ConstructorPattern(constructor, Arrays.asList(patternArgs)), isExplicit, false);
  }

  public static PatternArgument match(Constructor constructor, PatternArgument... patterns) {
    return match(true, constructor, patterns);
  }

  public static class ConstructorClausePair {
    private final Constructor constructor;
    private final ElimTreeNode child;

    private ConstructorClausePair(Constructor constructor, ElimTreeNode child) {
      this.constructor = constructor;
      this.child = child;
    }
  }

  public static BranchElimTreeNode branch(int index, ConstructorClausePair... clauses) {
    BranchElimTreeNode result = new BranchElimTreeNode(index);
    for (ConstructorClausePair pair : clauses) {
      result.addClause(pair.constructor, pair.child);
    }
    return result;
  }

  public static LeafElimTreeNode leaf(Expression expression) {
    return new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, expression);
  }

  public static LeafElimTreeNode leaf(Abstract.Definition.Arrow arrow, Expression expression) {
    return new LeafElimTreeNode(arrow, expression);
  }

  public static ConstructorClausePair clause(Constructor constructor, BranchElimTreeNode node) {
    return new ConstructorClausePair(constructor, node);
  }

  public static ConstructorClausePair clause(Constructor constructor, Abstract.Definition.Arrow arrow, Expression expr) {
    return new ConstructorClausePair(constructor, leaf(arrow, expr));
  }

  public static ConstructorClausePair clause(Constructor constructor, Expression expr) {
    return new ConstructorClausePair(constructor, new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, expr));
  }

  public static PatternArgument match(boolean isExplicit, String name) {
    return new PatternArgument(new NamePattern(name), isExplicit, false);
  }

  public static PatternArgument match(String name) {
    return match(true, name);
  }
}
