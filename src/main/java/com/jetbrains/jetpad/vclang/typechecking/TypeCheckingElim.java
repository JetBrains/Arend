package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.MultiElimTreeExpander;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ConditionViolationsCollector;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.CoverageChecker;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubsts;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class TypeCheckingElim {
  private final CheckTypeVisitor myVisitor;

  public TypeCheckingElim(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  public static TypeCheckingError checkConditions(final Abstract.Function def, List<Binding> eliminatingArgs, ElimTreeNode elimTree) {
    return checkConditions(def.getName(), def, def.getArguments(), eli, elimTree);
  }

  public static TypeCheckingError checkConditions(final Name name, final Abstract.SourceNode source, final List<? extends Abstract.Argument> arguments, DependentLink elimminatingArgs, ElimTreeNode elimTree) {
    final StringBuilder errorMsg = new StringBuilder();

    ConditionViolationsCollector.check(elimminatingArgs, elimTree, new ConditionViolationsCollector.ConditionViolationChecker() {
      @Override
      public void check(List<Binding> context, Expression expr1, List<Expression> subst1, Expression expr2, List<Expression> subst2) {
        expr1 = expr1.normalize(NormalizeVisitor.Mode.NF);
        expr2 = expr2.normalize(NormalizeVisitor.Mode.NF);

        if (!expr1.equals(expr2)){
          errorMsg.append("\n").append(name);
          printArgs(subst1, arguments, errorMsg);
          errorMsg.append(" = ").append(expr1).append(" =/= ").append(expr2).append(" = ").append(name);
          printArgs(subst2, arguments, errorMsg);
       }
      }
    }, context.size() - numberOfVariables(arguments));


    if (errorMsg.length() != 0) {
      return new TypeCheckingError("Condition check failed: " + errorMsg.toString(), source, getNames(context));
    }
    return null;
  }

  public static TypeCheckingError checkCoverage(final Abstract.Function def, List<Binding> context, ElimTreeNode elimTree) {
    final StringBuilder incompleteCoverageMessage = new StringBuilder();

    CoverageChecker.check(context, elimTree, context.size() - numberOfVariables(def.getArguments()), new CoverageChecker.CoverageCheckerMissingProcessor() {
      @Override
      public void process(List<Binding> missingContext, List<Expression> missing) {
        if (!isNF(missing)) {
          return;
        }

        incompleteCoverageMessage.append("\n ").append(def.getName());
        printArgs(missing, def.getArguments(), incompleteCoverageMessage);
      }
    });

    if (incompleteCoverageMessage.length() != 0) {
      return new TypeCheckingError("Coverage check failed for: " + incompleteCoverageMessage.toString(), def, getNames(context));
    } else {
      return null;
    }
  }

  private static void printArgs(List<Expression> subst1, List<? extends Abstract.Argument> arguments, StringBuilder errorMsg) {
    for (int i = 0, ii = 0; i < arguments.size(); i++) {
      if (arguments.get(i) instanceof Abstract.TelescopeArgument) {
        for (String ignore : ((Abstract.TelescopeArgument) arguments.get(i)).getNames()) {
          errorMsg.append(" ").append(arguments.get(i).getExplicit() ? "(" : "{");
          errorMsg.append(subst1.get(ii++));
          errorMsg.append(arguments.get(i).getExplicit() ? ")" : "}");
        }
      } else {
        errorMsg.append(" ").append(arguments.get(i).getExplicit() ? "(" : "{");
        errorMsg.append(subst1.get(ii++));
        errorMsg.append(arguments.get(i).getExplicit() ? ")" : "}");
      }
    }
  }

  public ElimTreeNode typeCheckElim(final Abstract.ElimExpression expr, DependentLink eliminatingArgs, Expression expectedType) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError("Cannot infer type of the expression", expr, getNames(myVisitor.getLocalContext()));
    }
    if (eliminatingArgs == null && error == null) {
      error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr, getNames(myVisitor.getLocalContext()));
    }

    if (error != null) {
      myVisitor.getErrorReporter().report(error);
      expr.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
      return null;
    }

    final List<ReferenceExpression> elimExprs = typecheckElimIndices(expr, eliminatingArgs);
    if (elimExprs == null) return null;

    boolean wasError = false;

    List<Pattern> emptyPatterns = new ArrayList<>(Collections.<Pattern>nCopies(elimExprs.get(0).getIndex() + 1, new NamePattern(null)));

    final List<List<Pattern>> patterns = new ArrayList<>();
    final List<Expression> expressions = new ArrayList<>();
    final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
    clause_loop:
    for (Abstract.Clause clause : expr.getClauses()) {
      try (Utils.CompleteContextSaver ignore = new Utils.CompleteContextSaver<>(myVisitor.getLocalContext())) {
        List<Pattern> clausePatterns = new ArrayList<>(emptyPatterns);
        Expression clauseExpectedType = expectedType;
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          CheckTypeVisitor.ExpandPatternResult result = myVisitor.expandPatternOn(clause.getPatterns().get(i), elimExprs.get(i).getIndex(), CheckTypeVisitor.PatternExpansionMode.FUNCTION);
          if (result instanceof CheckTypeVisitor.ExpandPatternErrorResult) {
            expr.getExpressions().get(i).setWellTyped(myVisitor.getLocalContext(), Error(null, ((CheckTypeVisitor.ExpandPatternErrorResult) result).error));
            wasError = true;
            continue clause_loop;
          }
          CheckTypeVisitor.ExpandPatternOKResult okResult = (CheckTypeVisitor.ExpandPatternOKResult) result;
          clausePatterns.set(clausePatterns.size() - 1 - elimExprs.get(i).getIndex(), okResult.pattern);
          clauseExpectedType = expandPatternSubstitute(okResult.pattern, elimExprs.get(i).getIndex(), okResult.expression, clauseExpectedType);
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

    ElimTreeConversionResult elimTreeResult = patternsToElimTree(eliminatingArgs, patterns, expressions, arrows);
    if (elimTreeResult instanceof OKElimTreeConversionResult) {
      return ((OKElimTreeConversionResult) elimTreeResult).elimTree;
    } else if (elimTreeResult instanceof EmptyReachableElimTreeConversionResult) {
      for (int i : ((EmptyReachableElimTreeConversionResult) elimTreeResult).reachable) {
        error = new TypeCheckingError("Empty clause is reachable", expr.getClauses().get(i), getNames(myVisitor.getLocalContext()));
        expr.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
      }
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  public static abstract class ElimTreeConversionResult {}

  public static class OKElimTreeConversionResult extends ElimTreeConversionResult {
    public final ElimTreeNode elimTree;

    public OKElimTreeConversionResult(ElimTreeNode elimTree) {
      this.elimTree = elimTree;
    }
  }

  public static class EmptyReachableElimTreeConversionResult extends ElimTreeConversionResult {
    public final Collection<Integer> reachable;

    public EmptyReachableElimTreeConversionResult(Collection<Integer> reachable) {
      this.reachable = reachable;
    }
  }

  public static ElimTreeConversionResult patternsToElimTree(List<Binding> eliminatingArgs, List<List<Pattern>> patterns, List<Expression> expressions, List<Abstract.Definition.Arrow> arrows) {
    MultiElimTreeExpander.MultiElimTreeExpansionResult treeExpansionResult = MultiElimTreeExpander.expandElimTree(eliminatingArgs, patterns);
    if (treeExpansionResult.branches.isEmpty())
      return new OKElimTreeConversionResult(treeExpansionResult.tree);

    Set<Integer> emptyReachable = new HashSet<>();
    for (MultiElimTreeExpander.MultiBranch branch : treeExpansionResult.branches) {
      for (int i : branch.indicies) {
        if (expressions.get(i) == null) {
          emptyReachable.add(i);
        }
      }

      if (expressions.get(branch.indicies.get(0)) == null) {
        continue;
      }

      branch.leaf.setArrow(arrows.get(branch.indicies.get(0)));
      List<Pattern> curPatterns = patterns.get(branch.indicies.get(0));
      Map<Binding, Expression> substs = new HashMap<>();
      for (int i = 0; i < curPatterns.size(); i++) {
        substs.putAll(toSubsts(curPatterns.get(i).getParameters(), ((Pattern.MatchOKResult)curPatterns.get(i).match(branch.expressions.get(i))).expressions);
      }
      branch.leaf.setExpression(expressions.get(branch.indicies.get(0)).subst(substs));
    }

    if (!emptyReachable.isEmpty()) {
      return new EmptyReachableElimTreeConversionResult(emptyReachable);
    } else {
      return new OKElimTreeConversionResult(treeExpansionResult.tree);
    }
  }

  private static boolean isNF(List<Expression> exprs) {
    for (Expression e : exprs) {
      if (!e.normalize(NormalizeVisitor.Mode.NF).equals(e)) {
        return false;
      }
    }
    return true;
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
        error = new TypeCheckingError("\\elim can be applied only to arguments of the innermost definition", var, getNames(myVisitor.getLocalContext()));
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }
      eliminatingIndicies.add(argsBindings.indexOf(((ReferenceExpression) exprResult.expression).getBinding()));

      if (eliminatingIndicies.size() >= 2 && eliminatingIndicies.get(eliminatingIndicies.size() - 2) >= eliminatingIndicies.get(eliminatingIndicies.size() - 1)) {
        error = new TypeCheckingError("Variable elimination must be in the order of variable introduction", var, getNames(myVisitor.getLocalContext()));
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }

      Expression ftype = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF).getFunction(new ArrayList<Expression>());
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeCheckingError("Elimination is allowed only for a data type variable.", var, getNames(myVisitor.getLocalContext()));
        myVisitor.getErrorReporter().report(error);
        var.setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        return null;
      }
      elimExprs.add((ReferenceExpression) exprResult.expression);
    }
    return elimExprs;
  }
}
