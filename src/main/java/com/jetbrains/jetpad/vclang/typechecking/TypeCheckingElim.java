package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Name;
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

  public abstract static class ExpandPatternResult {}

  public static class ExpandPatternOKResult extends ExpandPatternResult {
    public final DependentLink last;
    public final Expression expression;
    public final Pattern pattern;

    public ExpandPatternOKResult(DependentLink last, Expression expression, Pattern pattern) {
      this.last = last;
      this.expression = expression;
      this.pattern = pattern;
    }
  }

  public static class ExpandPatternErrorResult extends  ExpandPatternResult {
    public final TypeCheckingError error;

    public ExpandPatternErrorResult(TypeCheckingError error) {
      this.error = error;
    }
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding, PatternExpansionMode mode) {
    if (pattern instanceof Abstract.NamePattern || pattern == null) {
      String name = pattern == null || ((NamePattern) pattern).getName() == null ? null : ((NamePattern) pattern).getName();
      DependentLink link = new TypedDependentLink(true, name, binding.getType(), null);
      NamePattern namePattern = new NamePattern(link);
      myVisitor.getLocalContext().add(link);
      if (pattern != null)
        pattern.setWellTyped(namePattern);
      return new ExpandPatternOKResult(link, Reference(binding), namePattern);
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
        DependentLink link = new TypedDependentLink(true, null, binding.getType(), null);
        AnyConstructorPattern newPattern = new AnyConstructorPattern(link);
        pattern.setWellTyped(newPattern);
        myVisitor.getLocalContext().add(link);
        return new ExpandPatternOKResult(link, Reference(link), newPattern);
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
      Collections.reverse(matchedParameters);

      ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getArguments(), constructor.getDataTypeParameters());
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

      DependentLink constructorArgs = constructor.getDataTypeParameters().subst(toSubstitution(constructor.getDataTypeParameters(), matchedParameters));
      Expression substExpression = ConCall(constructor, matchedParameters);

      List<PatternArgument> resultPatterns = new ArrayList<>();
      DependentLink last = null;
      DependentLink tailArgs = constructorArgs;
      for (Abstract.PatternArgument subPattern : patterns) {
        ExpandPatternResult result = expandPattern(subPattern.getPattern(), constructorArgs, mode);
        if (result instanceof ExpandPatternErrorResult)
          return result;
        ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
        resultPatterns.add(new PatternArgument(okResult.pattern, subPattern.isExplicit(), subPattern.isHidden()));
        if (last != null)
          last.setNext(okResult.pattern.getParameters());
        tailArgs = tailArgs.getNext().subst(new Substitution(tailArgs, okResult.expression));
        substExpression = Apps(substExpression, okResult.expression);
        last = okResult.last;
      }

      ConstructorPattern result = new ConstructorPattern(constructor, new Patterns(resultPatterns));
      pattern.setWellTyped(result);
      return new ExpandPatternOKResult(last, substExpression, result);
    } else {
      throw new IllegalStateException();
    }
  }

  public static TypeCheckingError checkConditions(final Abstract.Function def, DependentLink eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditions(def.getName(), def, eliminatingArgs, elimTree);
  }

  public static TypeCheckingError checkConditions(final Name name, final Abstract.SourceNode source, final DependentLink eliminatingArgs, ElimTreeNode elimTree) {
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

  public Patterns visitPatternArgs(List<Abstract.PatternArgument> patternArgs, DependentLink eliminatingArgs, List<Expression> substIn, PatternExpansionMode mode) {
    List<PatternArgument> typedPatterns = new ArrayList<>();
    DependentLink last = null;
    for (int i = 0; i < patternArgs.size(); i++) {
      ExpandPatternResult result = expandPattern(patternArgs.get(i).getPattern(), eliminatingArgs, mode);
      if (result == null || result instanceof ExpandPatternErrorResult)
        return null;

      typedPatterns.add(new PatternArgument(((ExpandPatternOKResult) result).pattern, patternArgs.get(i).isExplicit(), patternArgs.get(i).isHidden()));

      for (int j = 0; j < substIn.size(); j++) {
        substIn.set(j, substIn.get(j).subst(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
      }

      if (last != null)
        last.setNext(((ExpandPatternOKResult) result).pattern.getParameters());
      eliminatingArgs = eliminatingArgs.getNext().subst(new Substitution(eliminatingArgs, ((ExpandPatternOKResult) result).expression));
      last = ((ExpandPatternOKResult) result).last;
    }
    return new Patterns(typedPatterns);
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

  public ElimTreeNode typeCheckElim(final Abstract.ElimExpression expr, DependentLink eliminatingArgs, Expression expectedType) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError("Cannot infer type of the expression", expr);
    }
    if (eliminatingArgs == null && error == null) {
      error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    }

    if (error != null) {
      myVisitor.getErrorReporter().report(error);
      expr.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
      return null;
    }

    final List<ReferenceExpression> elimExprs = typecheckElimIndices(expr, eliminatingArgs);
    if (elimExprs == null) return null;

    boolean wasError = false;

    List<Binding> argsBindings = toContext(eliminatingArgs);

    final List<List<Pattern>> patterns = new ArrayList<>();
    final List<Expression> expressions = new ArrayList<>();
    final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
    clause_loop:
    for (Abstract.Clause clause : expr.getClauses()) {
      try (Utils.CompleteContextSaver ignore = new Utils.CompleteContextSaver<>(myVisitor.getLocalContext())) {
        List<Pattern> clausePatterns = new ArrayList<>();
        Expression clauseExpectedType = expectedType;

        DependentLink tailArgs = eliminatingArgs;
        DependentLink last = null;
        for (int i = 0, j = 0; tailArgs != null && i < argsBindings.size(); i++) {
          ExpandPatternResult result;
          if (j < elimExprs.size() && elimExprs.get(j) == argsBindings.get(i)) {
            result = expandPattern(clause.getPatterns().get(j), tailArgs, PatternExpansionMode.FUNCTION);
            j++;
          } else {
            result = expandPattern(null, tailArgs, PatternExpansionMode.FUNCTION);
          }

          if (result instanceof ExpandPatternErrorResult) {
            expr.getExpressions().get(i).setWellTyped(myVisitor.getLocalContext(), Error(null, ((ExpandPatternErrorResult) result).error));
            wasError = true;
            continue clause_loop;
          }

          ExpandPatternOKResult okResult = (ExpandPatternOKResult) result;
          clausePatterns.add(okResult.pattern);
          clauseExpectedType = clauseExpectedType.subst(tailArgs, okResult.expression);
          tailArgs = tailArgs.getNext() == null ? null : tailArgs.getNext().subst(new Substitution(tailArgs, okResult.expression));
          if (last != null)
            last.setNext(okResult.pattern.getParameters());
          last = okResult.last;
        }

        if (clause.getExpression() != null) {
          CheckTypeVisitor.Result clauseResult = myVisitor.typeCheck(clause.getExpression(), clauseExpectedType);
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

    PatternsToElimTreeConversion.Result elimTreeResult = PatternsToElimTreeConversion.convert(eliminatingArgs, patterns, expressions, arrows);

    if (elimTreeResult instanceof PatternsToElimTreeConversion.OKResult) {
      return ((PatternsToElimTreeConversion.OKResult) elimTreeResult).elimTree;
    } else if (elimTreeResult instanceof PatternsToElimTreeConversion.EmptyReachableResult) {
      for (int i : ((PatternsToElimTreeConversion.EmptyReachableResult) elimTreeResult).reachable) {
        error = new TypeCheckingError("Empty clause is reachable", expr.getClauses().get(i));
        expr.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
      }
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  private List<ReferenceExpression> typecheckElimIndices(Abstract.ElimExpression expr, DependentLink eliminatingArgs) {
    List<Binding> argsBindings = toContext(eliminatingArgs);
    List<Integer> eliminatingIndicies = new ArrayList<>();

    TypeCheckingError error;
    final List<ReferenceExpression> elimExprs = new ArrayList<>(expr.getExpressions().size());
    for (Abstract.Expression var : expr.getExpressions()){
      CheckTypeVisitor.Result exprResult = myVisitor.lookupLocalVar(var, expr);
      if (exprResult == null) {
        return null;
      }

      if (!argsBindings.contains(((ReferenceExpression) exprResult.expression).getBinding())) {
        error = new TypeCheckingError("\\elim can be applied only to arguments of the innermost definition", var);
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }
      eliminatingIndicies.add(argsBindings.indexOf(((ReferenceExpression) exprResult.expression).getBinding()));

      if (eliminatingIndicies.size() >= 2 && eliminatingIndicies.get(eliminatingIndicies.size() - 2) >= eliminatingIndicies.get(eliminatingIndicies.size() - 1)) {
        error = new TypeCheckingError("Variable elimination must be in the order of variable introduction", var);
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }

      Expression ftype = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF).getFunction(new ArrayList<Expression>());
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Elimination is allowed only for a data type variable.", var);
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }
      elimExprs.add((ReferenceExpression) exprResult.expression);
    }
    return elimExprs;
  }

  private static void printArgs(DependentLink eliminatingArgs, Substitution argsSubst, StringBuilder errorMsg) {
    for (DependentLink link = eliminatingArgs; link != null; link = link.getNext()) {
      errorMsg.append(" ").append(link.isExplicit() ? "(" : "{");
      errorMsg.append(argsSubst.get(link));
      errorMsg.append(link.isExplicit() ? ")" : "}");
    }
  }
}
