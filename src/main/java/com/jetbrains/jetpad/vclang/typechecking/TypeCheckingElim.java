package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ConditionViolationsCollector;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.CoverageChecker;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ConstructorNotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.*;
import static com.jetbrains.jetpad.vclang.core.pattern.Utils.processImplicit;

public class TypeCheckingElim {
  private final CheckTypeVisitor myVisitor;

  public TypeCheckingElim(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  public enum PatternExpansionMode {
    FUNCTION, DATATYPE, CONDITION
  }

  public static LocalTypeCheckingError checkConditions(final Abstract.ReferableSourceNode def, DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditions(def.getName(), def, eliminatingArgs, elimTree);
  }

  public static LocalTypeCheckingError checkConditions(final String name, final Abstract.SourceNode source, final DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditionsContext(name, source, toList(eliminatingArgs), elimTree);
  }

  public static LocalTypeCheckingError checkConditions(String name, Abstract.SourceNode source, List<SingleDependentLink> eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditionsContext(name, source, toList(eliminatingArgs), elimTree);
  }

  public static LocalTypeCheckingError checkConditions(Abstract.ReferableSourceNode def, List<SingleDependentLink> eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditionsContext(def.getName(), def, toList(eliminatingArgs), elimTree);
  }

  private static LocalTypeCheckingError checkConditionsContext(final String name, final Abstract.SourceNode source, List<? extends DependentLink> context, ElimTreeNode elimTree) {
    final StringBuilder errorMsg = new StringBuilder();

    ConditionViolationsCollector.check(elimTree, ExprSubstitution.getIdentity(context), (expr1, argsSubst1, expr2, argsSubst2) -> {
      expr1 = expr1.normalize(NormalizeVisitor.Mode.NF);
      expr2 = expr2.normalize(NormalizeVisitor.Mode.NF);

      if (!expr1.equals(expr2)){
        errorMsg.append("\n").append(name);
        printArgs(context, argsSubst1, errorMsg);
        errorMsg.append(" = ").append(expr1).append(" =/= ").append(expr2).append(" = ").append(name);
        printArgs(context, argsSubst2, errorMsg);
     }
    });

    if (errorMsg.length() != 0) {
      return new LocalTypeCheckingError("Condition check failed: " + errorMsg.toString(), source);
    }
    return null;
  }

  public static LocalTypeCheckingError checkCoverage(final Abstract.ReferableSourceNode def, final DependentLink eliminatingArgs, ElimTreeNode elimTree, Expression resultType) {
    return checkCoverageContext(def.getName(), def, toList(eliminatingArgs), elimTree, resultType);
  }

  public static LocalTypeCheckingError checkCoverage(String name, Abstract.SourceNode sourceNode, List<SingleDependentLink> eliminatingArgs, ElimTreeNode elimTree, Expression resultType) {
    return checkCoverageContext(name, sourceNode, toList(eliminatingArgs), elimTree, resultType);
  }

  private static LocalTypeCheckingError checkCoverageContext(String name, Abstract.SourceNode sourceNode, List<? extends DependentLink> context, ElimTreeNode elimTree, Expression resultType) {
    final StringBuilder incompleteCoverageMessage = new StringBuilder();

    CoverageChecker.check(elimTree, ExprSubstitution.getIdentity(context), argsSubst -> {
      for (Map.Entry<Variable, Expression> entry : argsSubst.getEntries()) {
        Expression expr = entry.getValue();
        if (!expr.normalize(NormalizeVisitor.Mode.NF).equals(expr)) {
          return;
        }
      }

      incompleteCoverageMessage.append("\n ").append(name);
      printArgs(context, argsSubst, incompleteCoverageMessage);
    }, resultType);

    if (incompleteCoverageMessage.length() != 0) {
      return new LocalTypeCheckingError("Coverage check failed for: " + incompleteCoverageMessage.toString(), sourceNode);
    } else {
      return null;
    }
  }

  public Patterns visitPatternArgs(List<Abstract.PatternArgument> patternArgs, DependentLink eliminatingArgs, List<Expression> substIn, PatternExpansionMode mode) {
    List<PatternArgument> typedPatterns = new ArrayList<>();
    LinkList links = new LinkList();
    Set<Binding> bounds = new HashSet<>(myVisitor.getContext());
    for (Abstract.PatternArgument patternArg : patternArgs) {
      ExpandPatternResult result = expandPattern(patternArg.getPattern(), eliminatingArgs, mode, links);
      if (result == null || result instanceof ExpandPatternErrorResult)
        return null;

      typedPatterns.add(new PatternArgument(((ExpandPatternOKResult) result).pattern, patternArg.isExplicit(), patternArg.isHidden()));

      for (DependentLink link = ((ExpandPatternOKResult) result).pattern.getParameters(); link.hasNext(); link = link.getNext()) {
        bounds.add(link);
      }

      for (int j = 0; j < substIn.size(); j++) {
        substIn.set(j, substIn.get(j).subst(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
      }

      eliminatingArgs = DependentLink.Helper.subst(eliminatingArgs.getNext(), new ExprSubstitution(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
    }

    return new Patterns(typedPatterns);
  }

  public ElimTreeNode typeCheckElim(final Abstract.ElimCaseExpression expr, DependentLink eliminatingArgs, Expression expectedType, boolean isCase, boolean isTopLevel) {
    LocalTypeCheckingError error = null;
    if (expectedType == null) {
      error = new LocalTypeCheckingError("Cannot infer type of the expression", expr);
    }
    if (eliminatingArgs == null && error == null) {
      error = new LocalTypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    }

    if (error != null) {
      myVisitor.getErrorReporter().report(error);
      expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
      return null;
    }

    final List<ReferenceExpression> elimExprs;
    if (!isCase) {
      elimExprs = typecheckElimIndices(expr, eliminatingArgs);
    } else {
      elimExprs = new ArrayList<>();
      for (DependentLink link = eliminatingArgs; link.hasNext(); link = link.getNext()) {
        elimExprs.add(new ReferenceExpression(link));
      }
    }
    if (elimExprs == null) return null;

    boolean wasError = false;

    for (ReferenceExpression elimExpr : elimExprs) {
      DataCallExpression dataCall = elimExpr.getBinding().getType().getExpr().toDataCall();
      if (dataCall != null && dataCall.getDefinition().isTruncated()) {
        if (!expectedType.getType().isLessOrEquals(dataCall.getType(), myVisitor.getEquations(), expr)) {
          error = new LocalTypeCheckingError("Data " + dataCall.getDefinition().getName() + " is truncated to the universe "
            + dataCall.getType() + " which must be not less than the universe of " +
            expectedType + " - the type of eliminator", expr);
          expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
          myVisitor.getErrorReporter().report(error);
          wasError = true;
        }
      }
    }

    DependentLink origEliminatingArgs = eliminatingArgs;
    List<Pattern> dummyPatterns = new ArrayList<>();
    for (;eliminatingArgs.hasNext() && eliminatingArgs != elimExprs.get(0).getBinding(); eliminatingArgs = eliminatingArgs.getNext()) {
      dummyPatterns.add(new NamePattern(eliminatingArgs));
      myVisitor.getContext().add(eliminatingArgs);
    }

    List<Binding> argsBindings = toContext(eliminatingArgs);

    final List<List<Pattern>> patterns = new ArrayList<>();
    final List<Expression> expressions = new ArrayList<>();
    final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
    clause_loop:
    for (Abstract.Clause clause : expr.getClauses()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(myVisitor.getContext())) {
        List<Pattern> clausePatterns = new ArrayList<>(dummyPatterns);
        Expression clauseExpectedType = expectedType;

        DependentLink tailArgs = eliminatingArgs;
        LinkList links = new LinkList();
        for (int i = 0, j = 0; tailArgs.hasNext() && i < argsBindings.size(); i++) {
          ExpandPatternResult patternResult;
          if (j < elimExprs.size() && elimExprs.get(j).getBinding() == argsBindings.get(i)) {
            patternResult = expandPattern(clause.getPatterns().get(j), tailArgs, PatternExpansionMode.FUNCTION, links);
            j++;
          } else {
            patternResult = expandPattern(null, tailArgs, PatternExpansionMode.FUNCTION, links);
          }

          if (patternResult instanceof ExpandPatternErrorResult) {
            assert j <= elimExprs.size();
            expr.getExpressions().get(j - 1).setWellTyped(myVisitor.getContext(), new ErrorExpression(null, ((ExpandPatternErrorResult) patternResult).error));
            wasError = true;
            continue clause_loop;
          }

          ExpandPatternOKResult okResult = (ExpandPatternOKResult) patternResult;
          clausePatterns.add(okResult.pattern);
          ExprSubstitution subst = new ExprSubstitution(tailArgs, okResult.expression);
          tailArgs = DependentLink.Helper.subst(tailArgs.getNext(), subst);
          clauseExpectedType = clauseExpectedType.subst(subst, LevelSubstitution.EMPTY);
        }

        if (clause.getExpression() != null) {
          CheckTypeVisitor.Result clauseResult = isTopLevel ? myVisitor.finalCheckExpr(clause.getExpression(), clauseExpectedType) : myVisitor.checkExpr(clause.getExpression(), clauseExpectedType);
          if (clauseResult == null) {
            wasError = true;
            continue;
          }
          expressions.add(clauseResult.expression);
        } else {
          expressions.add(null);
        }
        patterns.add(clausePatterns);
        arrows.add(clause.getArrow());
      }
    }

    if (wasError) {
      return null;
    }

    PatternsToElimTreeConversion.Result elimTreeResult = PatternsToElimTreeConversion.convert(origEliminatingArgs, patterns, expressions, arrows);

    if (elimTreeResult instanceof PatternsToElimTreeConversion.OKResult) {
      ElimTreeNode result = ((PatternsToElimTreeConversion.OKResult) elimTreeResult).elimTree;
      if (isTopLevel) {
        LevelSubstitution substitution = myVisitor.getEquations().solve(expr);
        if (!substitution.isEmpty()) {
          result = result.subst(new ExprSubstitution(), substitution);
        }
        Set<Binding> bounds = new HashSet<>(myVisitor.getContext());
        bounds.addAll(argsBindings);
        result = result.accept(new StripVisitor(bounds, myVisitor.getErrorReporter()), null);
      }
      return result;
    } else if (elimTreeResult instanceof PatternsToElimTreeConversion.EmptyReachableResult) {
      for (int i : ((PatternsToElimTreeConversion.EmptyReachableResult) elimTreeResult).reachable) {
        error = new LocalTypeCheckingError("Empty clause is reachable", expr.getClauses().get(i));
        expr.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
        myVisitor.getErrorReporter().report(error);
      }
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  private ReferenceExpression lookupLocalVar(Abstract.Expression expression) {
    if (expression instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expression).getExpression() == null && ((Abstract.DefCallExpression) expression).getReferent() == null) {
      CheckTypeVisitor.TResult exprResult = myVisitor.getLocalVar((Abstract.DefCallExpression) expression);
      if (exprResult == null) {
        return null;
      }
      if (exprResult instanceof CheckTypeVisitor.Result) {
        ReferenceExpression result = ((CheckTypeVisitor.Result) exprResult).expression.toReference();
        if (result != null) {
          return result;
        }
      }
    }

    LocalTypeCheckingError error = new LocalTypeCheckingError("\\elim can be applied only to a local variable", expression);
    myVisitor.getErrorReporter().report(error);
    expression.setWellTyped(myVisitor.getContext(), new ErrorExpression(null, error));
    return null;
  }

  private List<ReferenceExpression> typecheckElimIndices(Abstract.ElimCaseExpression expr, DependentLink eliminatingArgs) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myVisitor.getContext())) {
      List<Binding> argsBindings = toContext(eliminatingArgs);
      myVisitor.getContext().addAll(argsBindings);

      List<Integer> eliminatingIndices = new ArrayList<>();

      LocalTypeCheckingError error;
      final List<ReferenceExpression> elimExprs = new ArrayList<>(expr.getExpressions().size());
      for (Abstract.Expression var : expr.getExpressions()) {
        ReferenceExpression refExpr = lookupLocalVar(var);

        if (refExpr == null) {
          return null;
        }

        if (!argsBindings.contains(refExpr.getBinding())) {
          error = new LocalTypeCheckingError("\\elim can be applied only to arguments of the innermost definition", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, new ErrorExpression(null, error));
          return null;
        }
        eliminatingIndices.add(argsBindings.indexOf(refExpr.getBinding()));

        if (eliminatingIndices.size() >= 2 && eliminatingIndices.get(eliminatingIndices.size() - 2) >= eliminatingIndices.get(eliminatingIndices.size() - 1)) {
          error = new LocalTypeCheckingError("Variable elimination must be in the order of variable introduction", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, new ErrorExpression(null, error));
          return null;
        }

        if (refExpr.getType().normalize(NormalizeVisitor.Mode.WHNF).toDataCall() == null) {
          error = new LocalTypeCheckingError("Elimination is allowed only for a data type variable.", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, new ErrorExpression(null, error));
          return null;
        }
        elimExprs.add(refExpr);
      }
      return elimExprs;
    }
  }

  private static void printArgs(List<? extends DependentLink> context, ExprSubstitution argsSubst, StringBuilder errorMsg) {
    for (DependentLink link : context) {
      errorMsg.append(" ").append(link.isExplicit() ? "(" : "{");
      errorMsg.append(argsSubst.get(link));
      errorMsg.append(link.isExplicit() ? ")" : "}");
    }
  }

  private abstract static class ExpandPatternResult {}

  private static class ExpandPatternOKResult extends ExpandPatternResult {
    private final Expression expression;
    private final Pattern pattern;

    private ExpandPatternOKResult(Expression expression, Pattern pattern) {
      this.expression = expression;
      this.pattern = pattern;
    }
  }

  private static class ExpandPatternErrorResult extends  ExpandPatternResult {
    private final LocalTypeCheckingError error;

    private ExpandPatternErrorResult(LocalTypeCheckingError error) {
      this.error = error;
    }
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, DependentLink binding, PatternExpansionMode mode, LinkList links) {
    if (pattern == null) {
      links.append(new TypedDependentLink(true, binding.getName(), binding.getType(), EmptyDependentLink.getInstance()));
      myVisitor.getContext().add(links.getLast());
      return new ExpandPatternOKResult(new ReferenceExpression(links.getLast()), new NamePattern(links.getLast()));
    } else if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName() == null ? binding.getName() : ((Abstract.NamePattern) pattern).getName();
      links.append(new TypedDependentLink(true, name, binding.getType(), EmptyDependentLink.getInstance()));
      NamePattern namePattern = new NamePattern(links.getLast());
      myVisitor.getContext().add(links.getLast());
      pattern.setWellTyped(namePattern);
      return new ExpandPatternOKResult(new ReferenceExpression(links.getLast()), namePattern);
    } else if (pattern instanceof Abstract.AnyConstructorPattern || pattern instanceof Abstract.ConstructorPattern) {
      LocalTypeCheckingError error = null;

      Type type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF);
      if (!(type instanceof DataCallExpression)) {
        error = new LocalTypeCheckingError("Pattern expected a data type, got: " + type, pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      DataCallExpression dataCall = (DataCallExpression) type;
      DataDefinition dataType = dataCall.getDefinition();
      List<? extends Expression> parameters = dataCall.getDefCallArguments();

      if (mode == PatternExpansionMode.DATATYPE) {
        boolean ok = pattern instanceof Abstract.ConstructorPattern;
        if (ok) {
          for (Constructor constructor : dataType.getConstructors()) {
            if (constructor.getCondition() != null && constructor.getName().equals(((Abstract.ConstructorPattern) pattern).getConstructorName())) {
              ok = false;
              break;
            }
          }
        }
        if (!ok) {
          error = new LocalTypeCheckingError("Pattern matching on a data type with conditions is not allowed here: " + type, pattern);
          myVisitor.getErrorReporter().report(error);
          return new ExpandPatternErrorResult(error);
        }
      }

      if ((mode == PatternExpansionMode.FUNCTION || mode == PatternExpansionMode.DATATYPE) && dataType == Prelude.INTERVAL) {
        error = new LocalTypeCheckingError("Pattern matching on an interval is not allowed here", pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (dataType.getMatchedConstructors(dataCall) == null) {
        error = new LocalTypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (pattern instanceof Abstract.AnyConstructorPattern) {
        TypedDependentLink param = new TypedDependentLink(true, null, type, EmptyDependentLink.getInstance());
        links.append(param);
        AnyConstructorPattern newPattern = new AnyConstructorPattern(param);
        pattern.setWellTyped(newPattern);
        myVisitor.getContext().add(param);
        return new ExpandPatternOKResult(new ReferenceExpression(param), newPattern);
      }

      Abstract.ConstructorPattern constructorPattern = (Abstract.ConstructorPattern) pattern;

      Constructor constructor = null;
      for (int index = 0; index < dataType.getConstructors().size(); ++index) {
        if (dataType.getConstructors().get(index).getName().equals(constructorPattern.getConstructorName())) {
          constructor = dataType.getConstructors().get(index);
          break;
        }
      }

      if (constructor == null) {
        error = new ConstructorNotInScopeError(constructorPattern.getConstructorName(), pattern);  // TODO: refer by reference
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (!constructor.status().headerIsOK()) {
        error = new HasErrors(constructor.getAbstractDefinition(), pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Pattern.MatchResult matchResult = constructor.getPatterns().match(parameters);
        if (matchResult instanceof Pattern.MatchMaybeResult) {
          throw new IllegalStateException();
        } else if (matchResult instanceof Pattern.MatchFailedResult) {
          error = new LocalTypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
                                             "Expected " + ((Pattern.MatchFailedResult) matchResult).failedPattern + ", got " + ((Pattern.MatchFailedResult) matchResult).actualExpression, pattern);
        } else if (matchResult instanceof Pattern.MatchOKResult) {
          matchedParameters = ((Pattern.MatchOKResult) matchResult).expressions;
        } else {
          throw new IllegalStateException();
        }

        if (error != null) {
          myVisitor.getErrorReporter().report(error);
          return new ExpandPatternErrorResult(error);
        }
      } else {
        matchedParameters = new ArrayList<>(parameters);
      }

      ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getArguments(), constructor.getParameters());
      if (implicitResult.patterns == null) {
        if (implicitResult.numExcessive != 0) {
          error = new LocalTypeCheckingError("Too many arguments: " + implicitResult.numExcessive + " excessive", pattern);
        } else if (implicitResult.wrongImplicitPosition < constructorPattern.getArguments().size()) {
          error = new LocalTypeCheckingError("Unexpected implicit argument", constructorPattern.getArguments().get(implicitResult.wrongImplicitPosition));
        } else {
          error = new LocalTypeCheckingError("Too few explicit arguments, expected: " + implicitResult.numExplicit, pattern);
        }
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }
      List<Abstract.PatternArgument> patterns = implicitResult.patterns;

      DependentLink constructorArgs = DependentLink.Helper.subst(constructor.getParameters(),
        toSubstitution(constructor.getDataTypeParameters(), matchedParameters),
        dataCall.getSortArgument().toLevelSubstitution());

      List<PatternArgument> resultPatterns = new ArrayList<>();
      DependentLink tailArgs = constructorArgs;
      List<Expression> arguments = new ArrayList<>(patterns.size());
      for (Abstract.PatternArgument subPattern : patterns) {
        ExpandPatternResult result = expandPattern(subPattern.getPattern(), tailArgs, mode, links);
        if (result instanceof ExpandPatternErrorResult)
          return result;
        ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
        resultPatterns.add(new PatternArgument(okResult.pattern, subPattern.isExplicit(), subPattern.isHidden()));
        tailArgs = DependentLink.Helper.subst(tailArgs.getNext(), new ExprSubstitution(tailArgs, okResult.expression));
        arguments.add(okResult.expression);
      }

      ConstructorPattern result = new ConstructorPattern(constructor, new Patterns(resultPatterns));
      pattern.setWellTyped(result);
      return new ExpandPatternOKResult(new ConCallExpression(constructor, dataCall.getSortArgument(), matchedParameters, arguments), result);
    } else {
      throw new IllegalStateException();
    }
  }
}
