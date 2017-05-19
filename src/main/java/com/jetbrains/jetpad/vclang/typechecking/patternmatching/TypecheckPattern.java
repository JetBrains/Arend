package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BindingPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.EmptyPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.StdLevelSubstitution;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypecheckPattern {
  public static Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> typecheckPatternArguments(List<? extends Abstract.PatternArgument> patternArgs, DependentLink parameters, CheckTypeVisitor visitor, Abstract.SourceNode sourceNode, boolean allowInterval) {
    Map<Abstract.ReferableSourceNode, Binding> context = new HashMap<>();
    Pair<List<Pattern>, List<Expression>> result = typecheckPatternArguments(patternArgs, parameters, context, visitor, sourceNode, allowInterval);
    return result == null ? null : new Pair<>(result.proj1, result.proj2 == null ? null : context);
  }

  private static Pair<List<Pattern>, List<Expression>> typecheckPatternArguments(List<? extends Abstract.PatternArgument> patternArgs, DependentLink parameters, Map<Abstract.ReferableSourceNode, Binding> context, CheckTypeVisitor visitor, Abstract.SourceNode sourceNode, boolean allowInterval) {
    List<Pattern> result = new ArrayList<>();
    List<Expression> exprs = new ArrayList<>();

    for (Abstract.PatternArgument patternArg : patternArgs) {
      if (!parameters.hasNext()) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Too many patterns", patternArg));
        return null;
      }

      if (patternArg.isExplicit()) {
        while (!parameters.isExplicit()) {
          result.add(new BindingPattern(parameters));
          if (exprs != null) {
            exprs.add(new ReferenceExpression(parameters));
          }
          parameters = parameters.getNext();
          if (!parameters.hasNext()) {
            visitor.getErrorReporter().report(new LocalTypeCheckingError("Too many patterns", patternArg));
            return null;
          }
        }
      } else {
        if (parameters.isExplicit()) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected an explicit pattern", patternArg));
          return null;
        }
      }

      Abstract.Pattern pattern = patternArg.getPattern();
      if (exprs == null || pattern instanceof Abstract.NamePattern) {
        if (!(pattern instanceof Abstract.NamePattern)) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This pattern is ignored", pattern));
        }
        result.add(new BindingPattern(parameters));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(parameters));
          if (((Abstract.NamePattern) pattern).getReferent() != null) {
            context.put(((Abstract.NamePattern) pattern).getReferent(), parameters);
          }
        }
        parameters = parameters.getNext();
        continue;
      }

      Expression expr = parameters.getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF);
      if (expr.toDataCall() == null) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a data type, actual type: " + expr, pattern));
        return null;
      }
      if (!allowInterval && expr.toDataCall().getDefinition() == Prelude.INTERVAL) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      if (pattern instanceof Abstract.AnyConstructorPattern) {
        List<ConCallExpression> conCalls = expr.toDataCall().getMatchedConstructors();
        if (!conCalls.isEmpty()) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Data type " + expr + " is not empty, available constructors: " + conCalls, pattern));
          return null;
        }
        result.add(EmptyPattern.INSTANCE);
        exprs = null;
        continue;
      }

      if (!(pattern instanceof Abstract.ConstructorPattern)) {
        throw new IllegalStateException();
      }
      Abstract.ConstructorPattern conPattern = (Abstract.ConstructorPattern) pattern;

      Constructor constructor = (Constructor) visitor.getTypecheckingState().getTypechecked(conPattern.getConstructor());
      ConCallExpression conCall = null;
      if (expr.toDataCall().getDefinition().getConstructors().contains(constructor)) {
        conCall = expr.toDataCall().getMatchedConCall(constructor);
      }
      if (conCall == null) {
        visitor.getErrorReporter().report(new LocalTypeCheckingError("'" + constructor + "' is not a constructor of data type '" + expr + "'", pattern));
        return null;
      }

      ExprSubstitution substitution = new ExprSubstitution();
      int i = 0;
      for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), i++) {
        substitution.add(link, conCall.getDataTypeArguments().get(i));
      }
      Pair<List<Pattern>, List<Expression>> conResult = typecheckPatternArguments(conPattern.getArguments(), DependentLink.Helper.subst(constructor.getParameters(), substitution, new StdLevelSubstitution(conCall.getSortArgument())), context, visitor, conPattern, allowInterval);
      if (conResult == null) {
        return null;
      }

      result.add(new ConstructorPattern(conCall, conResult.proj1));
      if (conResult.proj2 == null) {
        exprs = null;
        parameters = parameters.getNext();
      } else {
        conCall.addArguments(conResult.proj2);
        exprs.add(conCall);
        parameters = DependentLink.Helper.subst(parameters.getNext(), new ExprSubstitution(parameters, conCall));
      }
    }

    if (parameters.hasNext()) {
      visitor.getErrorReporter().report(new LocalTypeCheckingError("Not enough patterns, expected " + DependentLink.Helper.size(parameters) + " more", sourceNode));
      return null;
    }

    return new Pair<>(result, exprs);
  }
}
