package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ConditionViolationsCollector;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.CoverageChecker;
import com.jetbrains.jetpad.vclang.typechecking.error.HasErrors;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;

public class TypeCheckingElim {
  private final CheckTypeVisitor myVisitor;

  public TypeCheckingElim(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  public enum PatternExpansionMode {
    FUNCTION, DATATYPE, CONDITION
  }

  public static TypeCheckingError checkConditions(final Abstract.Function def, DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditions(def.getName(), def, eliminatingArgs, elimTree);
  }

  public static TypeCheckingError checkConditions(final String name, final Abstract.SourceNode source, final DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    final StringBuilder errorMsg = new StringBuilder();

    ConditionViolationsCollector.check(elimTree, Substitution.getIdentity(toContext(eliminatingArgs)), new ConditionViolationsCollector.ConditionViolationChecker() {
      @Override
      public void check(Expression expr1, Substitution argsSubst1, Expression expr2, Substitution argsSubst2) {
        expr1 = expr1.normalize(NormalizeVisitor.Mode.NF);
        expr2 = expr2.normalize(NormalizeVisitor.Mode.NF);

        if (!expr1.equals(expr2)){
          errorMsg.append("\n").append(name);
          printArgs(eliminatingArgs, argsSubst1, errorMsg);
          errorMsg.append(" = ").append(expr1).append(" =/= ").append(expr2).append(" = ").append(name);
          printArgs(eliminatingArgs, argsSubst2, errorMsg);
       }
      }
    });

    if (errorMsg.length() != 0) {
      return new TypeCheckingError("Condition check failed: " + errorMsg.toString(), source);
    }
    return null;
  }

  public static TypeCheckingError checkCoverage(final Abstract.Function def, final DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    final StringBuilder incompleteCoverageMessage = new StringBuilder();

    CoverageChecker.check(elimTree, Substitution.getIdentity(toContext(eliminatingArgs)), new CoverageChecker.CoverageCheckerMissingProcessor() {
      @Override
      public void process(Substitution argsSubst) {
        for (Binding binding : argsSubst.getDomain()) {
          if (!argsSubst.get(binding).normalize(NormalizeVisitor.Mode.NF).equals(argsSubst.get(binding)))
            return;
        }

        incompleteCoverageMessage.append("\n ").append(def.getName());
        printArgs(eliminatingArgs, argsSubst, incompleteCoverageMessage);
      }
    });

    if (incompleteCoverageMessage.length() != 0) {
      return new TypeCheckingError("Coverage check failed for: " + incompleteCoverageMessage.toString(), def);
    } else {
      return null;
    }
  }

  public Patterns visitPatternArgs(List<Abstract.PatternArgument> patternArgs, DependentLink eliminatingArgs, List<Expression> substIn, PatternExpansionMode mode) {
    List<PatternArgument> typedPatterns = new ArrayList<>();
    LinkList links = new LinkList();
    for (Abstract.PatternArgument patternArg : patternArgs) {
      ExpandPatternResult result = expandPattern(patternArg.getPattern(), eliminatingArgs, mode, links);
      if (result == null || result instanceof ExpandPatternErrorResult)
        return null;

      typedPatterns.add(new PatternArgument(((ExpandPatternOKResult) result).pattern, patternArg.isExplicit(), patternArg.isHidden()));

      for (int j = 0; j < substIn.size(); j++) {
        substIn.set(j, substIn.get(j).subst(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
      }

      eliminatingArgs = DependentLink.Helper.subst(eliminatingArgs.getNext(), new Substitution(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
    }
    return new Patterns(typedPatterns);
  }

  public static class Result extends TypeCheckingResult {
    public ElimTreeNode elimTree;

    public Result(ElimTreeNode elimTree) {
      this.elimTree = elimTree;
    }

    @Override
    public void subst(Substitution substitution) {
      elimTree = elimTree.subst(substitution);
    }
  }

  public Result typeCheckElim(final Abstract.ElimCaseExpression expr, DependentLink eliminatingArgs, Expression expectedType, boolean isCase) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError("Cannot infer type of the expression", expr);
    }
    if (eliminatingArgs == null && error == null) {
      error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    }

    if (error != null) {
      myVisitor.getErrorReporter().report(error);
      expr.setWellTyped(myVisitor.getContext(), Error(null, error));
      return null;
    }

    final List<ReferenceExpression> elimExprs;
    if (!isCase) {
      elimExprs = typecheckElimIndices(expr, eliminatingArgs);
    } else {
      elimExprs = new ArrayList<>();
      for (DependentLink link = eliminatingArgs; link.hasNext(); link = link.getNext()) {
        elimExprs.add(Reference(link));
      }
    }
    if (elimExprs == null) return null;

    boolean wasError = false;

    DependentLink origEliminatingArgs = eliminatingArgs;
    List<Pattern> dummyPatterns = new ArrayList<>();
    for (;eliminatingArgs.hasNext() && eliminatingArgs != elimExprs.get(0).getBinding(); eliminatingArgs = eliminatingArgs.getNext()) {
      dummyPatterns.add(new NamePattern(eliminatingArgs));
      myVisitor.getContext().add(eliminatingArgs);
    }

    List<Binding> argsBindings = toContext(eliminatingArgs);

    Result result = new Result(null);
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
            expr.getExpressions().get(j - 1).setWellTyped(myVisitor.getContext(), Error(null, ((ExpandPatternErrorResult) patternResult).error));
            wasError = true;
            continue clause_loop;
          }

          ExpandPatternOKResult okResult = (ExpandPatternOKResult) patternResult;
          clausePatterns.add(okResult.pattern);
          Substitution subst = new Substitution(tailArgs, okResult.expression);
          tailArgs = DependentLink.Helper.subst(tailArgs.getNext(), subst);
          clauseExpectedType = clauseExpectedType.subst(subst);
        }

        if (clause.getExpression() != null) {
          CheckTypeVisitor.Result clauseResult = myVisitor.typeCheck(clause.getExpression(), clauseExpectedType);
          if (clauseResult == null) {
            wasError = true;
            continue;
          }
          if (!clauseResult.getEquations().isEmpty()) {
            for (DependentLink link = links.getFirst(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
              clauseResult.getEquations().abstractBinding(link);
            }
            result.add(clauseResult);
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
      result.elimTree = ((PatternsToElimTreeConversion.OKResult) elimTreeResult).elimTree;
      result.update();
      return result;
    } else if (elimTreeResult instanceof PatternsToElimTreeConversion.EmptyReachableResult) {
      for (int i : ((PatternsToElimTreeConversion.EmptyReachableResult) elimTreeResult).reachable) {
        error = new TypeCheckingError("Empty clause is reachable", expr.getClauses().get(i));
        expr.setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
      }
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  private ReferenceExpression lookupLocalVar(Abstract.Expression expression) {
    if (expression instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expression).getExpression() == null && ((Abstract.DefCallExpression) expression).getResolvedName() == null) {
      CheckTypeVisitor.Result exprResult = myVisitor.getTypeCheckingDefCall().getLocalVar(((Abstract.DefCallExpression) expression).getName(), expression);
      if (exprResult == null)
        return null;
      return (ReferenceExpression) exprResult.expression;
    } else {
      TypeCheckingError error = new TypeCheckingError("\\elim can be applied only to a local variable", expression);
      myVisitor.getErrorReporter().report(error);
      expression.setWellTyped(myVisitor.getContext(), Error(null, error));
      return null;
    }
  }

  private List<ReferenceExpression> typecheckElimIndices(Abstract.ElimCaseExpression expr, DependentLink eliminatingArgs) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myVisitor.getContext())) {
      List<Binding> argsBindings = toContext(eliminatingArgs);
      myVisitor.getContext().addAll(argsBindings);

      List<Integer> eliminatingIndicies = new ArrayList<>();

      TypeCheckingError error;
      final List<ReferenceExpression> elimExprs = new ArrayList<>(expr.getExpressions().size());
      for (Abstract.Expression var : expr.getExpressions()) {
        ReferenceExpression refExpr = lookupLocalVar(var);

        if (refExpr == null) {
          return null;
        }

        if (!argsBindings.contains(refExpr.getBinding())) {
          error = new TypeCheckingError("\\elim can be applied only to arguments of the innermost definition", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, Error(null, error));
          return null;
        }
        eliminatingIndicies.add(argsBindings.indexOf(refExpr.getBinding()));

        if (eliminatingIndicies.size() >= 2 && eliminatingIndicies.get(eliminatingIndicies.size() - 2) >= eliminatingIndicies.get(eliminatingIndicies.size() - 1)) {
          error = new TypeCheckingError("Variable elimination must be in the order of variable introduction", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, Error(null, error));
          return null;
        }

        Expression ftype = refExpr.getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(new ArrayList<Expression>());
        if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
          error = new TypeCheckingError("Elimination is allowed only for a data type variable.", var);
          myVisitor.getErrorReporter().report(error);
          var.setWellTyped(argsBindings, Error(null, error));
          return null;
        }
        elimExprs.add(refExpr);
      }
      return elimExprs;
    }
  }

  private static void printArgs(DependentLink eliminatingArgs, Substitution argsSubst, StringBuilder errorMsg) {
    for (DependentLink link = eliminatingArgs; link.hasNext(); link = link.getNext()) {
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
    private final TypeCheckingError error;

    private ExpandPatternErrorResult(TypeCheckingError error) {
      this.error = error;
    }
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding, PatternExpansionMode mode, LinkList links) {
    if (pattern == null) {
      links.append(new TypedDependentLink(true, binding.getName(), binding.getType(), EmptyDependentLink.getInstance()));
      myVisitor.getContext().add(links.getLast());
      return new ExpandPatternOKResult(Reference(links.getLast()), new NamePattern(links.getLast()));
    } else if (pattern instanceof Abstract.NamePattern) {
      links.append(new TypedDependentLink(true, ((Abstract.NamePattern) pattern).getName(), binding.getType(), EmptyDependentLink.getInstance()));
      NamePattern namePattern = new NamePattern(links.getLast());
      myVisitor.getContext().add(links.getLast());
      pattern.setWellTyped(namePattern);
      return new ExpandPatternOKResult(Reference(links.getLast()), namePattern);
    } else if (pattern instanceof Abstract.AnyConstructorPattern || pattern instanceof Abstract.ConstructorPattern) {
      TypeCheckingError error = null;

      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF);
      List<Expression> parameters = new ArrayList<>();
      Expression ftype = type.getFunction(parameters);
      Collections.reverse(parameters);

      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Pattern expected a data type, got: " + type, pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();

      if (mode == PatternExpansionMode.DATATYPE && dataType.getConditions() != null) {
        error = new TypeCheckingError("Pattern matching on a data type with conditions is not allowed here: " + type, pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if ((mode == PatternExpansionMode.FUNCTION || mode == PatternExpansionMode.DATATYPE) && dataType == Prelude.INTERVAL) {
        error = new TypeCheckingError("Pattern matching on an interval is not allowed here", pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (dataType.getMatchedConstructors(parameters) == null) {
        error = new TypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (pattern instanceof Abstract.AnyConstructorPattern) {
        links.append(new TypedDependentLink(true, null, binding.getType(), EmptyDependentLink.getInstance()));
        AnyConstructorPattern newPattern = new AnyConstructorPattern(links.getLast());
        pattern.setWellTyped(newPattern);
        myVisitor.getContext().add(links.getLast());
        return new ExpandPatternOKResult(Reference(links.getLast()), newPattern);
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
        error = new NotInScopeError(pattern, constructorPattern.getConstructorName());
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      if (constructor.hasErrors()) {
        error = new HasErrors(constructor.getName(), pattern);
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }

      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Pattern.MatchResult matchResult = constructor.getPatterns().match(parameters);
        if (matchResult instanceof Pattern.MatchMaybeResult) {
          throw new IllegalStateException();
        } else if (matchResult instanceof Pattern.MatchFailedResult) {
          error = new TypeCheckingError("Constructor is not appropriate, failed to match data type parameters. " +
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
          error = new TypeCheckingError("Too many arguments: " + implicitResult.numExcessive + " excessive", pattern);
        } else if (implicitResult.wrongImplicitPosition < constructorPattern.getArguments().size()) {
          error = new TypeCheckingError("Unexpected implicit argument", constructorPattern.getArguments().get(implicitResult.wrongImplicitPosition));
        } else {
          error = new TypeCheckingError("Too few explicit arguments, expected: " + implicitResult.numExplicit, pattern);
        }
        myVisitor.getErrorReporter().report(error);
        return new ExpandPatternErrorResult(error);
      }
      List<Abstract.PatternArgument> patterns = implicitResult.patterns;

      DependentLink constructorArgs = DependentLink.Helper.subst(constructor.getParameters(), toSubstitution(constructor.getDataTypeParameters(), matchedParameters));
      Expression substExpression = ConCall(constructor, matchedParameters);

      List<PatternArgument> resultPatterns = new ArrayList<>();
      DependentLink tailArgs = constructorArgs;
      for (Abstract.PatternArgument subPattern : patterns) {
        ExpandPatternResult result = expandPattern(subPattern.getPattern(), tailArgs, mode, links);
        if (result instanceof ExpandPatternErrorResult)
          return result;
        ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
        resultPatterns.add(new PatternArgument(okResult.pattern, subPattern.isExplicit(), subPattern.isHidden()));
        tailArgs = DependentLink.Helper.subst(tailArgs.getNext(), new Substitution(tailArgs, okResult.expression));
        substExpression = Apps(substExpression, okResult.expression);
      }

      ConstructorPattern result = new ConstructorPattern(constructor, new Patterns(resultPatterns));
      pattern.setWellTyped(result);
      return new ExpandPatternOKResult(substExpression, result);
    } else {
      throw new IllegalStateException();
    }
  }
}
