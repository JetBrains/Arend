package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
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
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

class PatternTypechecking {
  private final LocalErrorReporter myErrorReporter;
  private final boolean myAllowInterval;
  private Map<Abstract.ReferableSourceNode, Binding> myContext;

  PatternTypechecking(LocalErrorReporter errorReporter, boolean allowInterval) {
    myErrorReporter = errorReporter;
    myAllowInterval = allowInterval;
  }

  private void collectBindings(List<Pattern> patterns, Collection<? super DependentLink> bindings) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof BindingPattern) {
        bindings.add(((BindingPattern) pattern).getBinding());
      } else if (pattern instanceof ConstructorPattern) {
        collectBindings(((ConstructorPattern) pattern).getPatterns(), bindings);
      }
    }
  }

  Pair<List<Pattern>, CheckTypeVisitor.Result> typecheckClause(Abstract.Clause clause, DependentLink parameters, List<DependentLink> elimParams, Expression expectedType, CheckTypeVisitor visitor, boolean isFinal) {
    myContext = visitor.getContext();

    // Typecheck patterns
    Pair<List<Pattern>, List<Expression>> result;
    if (elimParams != null) {
      // Put patterns in the correct order
      // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
      List<Abstract.Pattern> patterns = new ArrayList<>();
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        int index = elimParams.indexOf(link);
        patterns.add(index < 0 ? null : clause.getPatterns().get(index));
      }
      result = doTypechecking(patterns, DependentLink.Helper.subst(parameters, new ExprSubstitution()), clause, true);
    } else {
      result = doTypechecking(clause.getPatterns(), DependentLink.Helper.subst(parameters, new ExprSubstitution()), clause, false);
    }
    if (result == null) {
      return null;
    }

    // If we have the absurd pattern, then RHS is ignored
    if (result.proj2 == null) {
      if (clause.getExpression() != null) {
        myErrorReporter.report(new LocalTypeCheckingError(Error.Level.WARNING, "The RHS is ignored", clause.getExpression()));
      }
      return new Pair<>(result.proj1, null);
    } else {
      if (clause.getExpression() == null) {
        myErrorReporter.report(new LocalTypeCheckingError("Required a RHS", clause));
        return null;
      }
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (Expression expr : result.proj2) {
      substitution.add(parameters, expr);
      parameters = parameters.getNext();
    }
    for (Map.Entry<Abstract.ReferableSourceNode, Binding> entry : myContext.entrySet()) {
      Expression expr = substitution.get(entry.getValue());
      if (expr != null) {
        entry.setValue(expr.toReference().getBinding());
      }
    }
    expectedType = expectedType.subst(substitution);

    // Typecheck the RHS
    CheckTypeVisitor.Result tcResult;
    if (isFinal) {
      visitor.getFreeBindings().clear();
      collectBindings(result.proj1, visitor.getFreeBindings());
      if (visitor.getTypeCheckingDefCall().getThisClass() != null) {
        visitor.setThis(visitor.getTypeCheckingDefCall().getThisClass(), getBinding(result.proj1, true));
      }
      tcResult = visitor.finalCheckExpr(clause.getExpression(), expectedType);
    } else {
      tcResult = visitor.checkExpr(clause.getExpression(), expectedType);
    }
    return tcResult == null ? null : new Pair<>(result.proj1, tcResult);
  }

  Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> typecheckPatterns(List<? extends Abstract.Pattern> patterns, DependentLink parameters, Abstract.SourceNode sourceNode, boolean fullList) {
    myContext = new HashMap<>();
    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, sourceNode, fullList);
    return result == null ? null : new Pair<>(result.proj1, result.proj2 == null ? null : myContext);
  }

  private static DependentLink getBinding(List<Pattern> patterns, boolean first) {
    for (int i = first ? 0 : patterns.size() - 1; first ? i < patterns.size() : i >= 0; i += first ? 1 : -1) {
      if (patterns.get(i) instanceof EmptyPattern) {
        return EmptyDependentLink.getInstance();
      }
      if (patterns.get(i) instanceof BindingPattern) {
        return ((BindingPattern) patterns.get(i)).getBinding();
      } else
      if (patterns.get(i) instanceof ConstructorPattern) {
        DependentLink last = getBinding(((ConstructorPattern) patterns.get(i)).getPatterns(), first);
        if (last != null) {
          return last;
        }
      }
    }
    return null;
  }

  private void addPattern(List<Pattern> patterns, Pattern pattern) {
    DependentLink last = getBinding(patterns, false);
    if (last != null && last.hasNext()) {
      DependentLink first = getBinding(Collections.singletonList(pattern), true);
      if (first != null) {
        last.setNext(first);
      }
    }
    patterns.add(pattern);
  }

  private Pair<List<Pattern>, List<Expression>> doTypechecking(List<? extends Abstract.Pattern> patterns, DependentLink parameters, Abstract.SourceNode sourceNode, boolean fullList) {
    List<Pattern> result = new ArrayList<>();
    List<Expression> exprs = new ArrayList<>();

    for (Abstract.Pattern pattern : patterns) {
      if (!parameters.hasNext()) {
        myErrorReporter.report(new LocalTypeCheckingError("Too many patterns", pattern));
        return null;
      }

      if (!fullList) {
        if (pattern.isExplicit()) {
          while (!parameters.isExplicit()) {
            addPattern(result, new BindingPattern(parameters));
            if (exprs != null) {
              exprs.add(new ReferenceExpression(parameters));
            }
            parameters = parameters.getNext();
            if (!parameters.hasNext()) {
              myErrorReporter.report(new LocalTypeCheckingError("Too many patterns", pattern));
              return null;
            }
          }
        } else {
          if (parameters.isExplicit()) {
            myErrorReporter.report(new LocalTypeCheckingError("Expected an explicit pattern", pattern));
            return null;
          }
        }
      }

      if (exprs == null || pattern == null || pattern instanceof Abstract.NamePattern) {
        if (!(pattern == null || pattern instanceof Abstract.NamePattern)) {
          myErrorReporter.report(new LocalTypeCheckingError(Error.Level.WARNING, "This pattern is ignored", pattern));
        }
        addPattern(result, new BindingPattern(parameters));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(parameters));
          if (pattern != null && ((Abstract.NamePattern) pattern).getReferent() != null) {
            myContext.put(((Abstract.NamePattern) pattern).getReferent(), parameters);
          }
        }
        parameters = parameters.getNext();
        continue;
      }

      Expression expr = parameters.getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF);
      if (expr.toDataCall() == null) {
        myErrorReporter.report(new LocalTypeCheckingError("Expected a data type, actual type: " + expr, pattern));
        return null;
      }
      if (!myAllowInterval && expr.toDataCall().getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new LocalTypeCheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      if (pattern instanceof Abstract.EmptyPattern) {
        List<ConCallExpression> conCalls = expr.toDataCall().getMatchedConstructors();
        if (conCalls == null) {
          myErrorReporter.report(new LocalTypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", pattern));
          return null;
        }
        if (!conCalls.isEmpty()) {
          myErrorReporter.report(new LocalTypeCheckingError("Data type " + expr + " is not empty, available constructors: " + conCalls, pattern));
          return null;
        }
        addPattern(result, EmptyPattern.INSTANCE);
        exprs = null;
        parameters = parameters.getNext();
        continue;
      }

      if (!(pattern instanceof Abstract.ConstructorPattern)) {
        throw new IllegalStateException();
      }
      Abstract.ConstructorPattern conPattern = (Abstract.ConstructorPattern) pattern;

      Constructor constructor = expr.toDataCall().getDefinition().getConstructor(conPattern.getConstructor());
      List<ConCallExpression> conCalls = new ArrayList<>(1);
      if (constructor == null || !expr.toDataCall().getMatchedConCall(constructor, conCalls) || conCalls.isEmpty() ) {
        myErrorReporter.report(new LocalTypeCheckingError("'" + conPattern.getConstructor() + "' is not a constructor of data type '" + expr + "'", pattern));
        return null;
      }
      ConCallExpression conCall = conCalls.get(0);

      ExprSubstitution substitution = new ExprSubstitution();
      int i = 0;
      for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), i++) {
        substitution.add(link, conCall.getDataTypeArguments().get(i));
      }
      Pair<List<Pattern>, List<Expression>> conResult = doTypechecking(conPattern.getArguments(), DependentLink.Helper.subst(constructor.getParameters(), substitution, new StdLevelSubstitution(conCall.getSortArgument())), conPattern, false);
      if (conResult == null) {
        return null;
      }

      addPattern(result, new ConstructorPattern(conCall, conResult.proj1));
      if (conResult.proj2 == null) {
        exprs = null;
        parameters = parameters.getNext();
      } else {
        conCall.addArguments(conResult.proj2);
        exprs.add(conCall);
        parameters = DependentLink.Helper.subst(parameters.getNext(), new ExprSubstitution(parameters, conCall));
      }
    }

    while (!parameters.isExplicit()) {
      addPattern(result, new BindingPattern(parameters));
      if (exprs != null) {
        exprs.add(new ReferenceExpression(parameters));
      }
      parameters = parameters.getNext();
    }

    if (parameters.hasNext()) {
      myErrorReporter.report(new LocalTypeCheckingError("Not enough patterns, expected " + DependentLink.Helper.size(parameters) + " more", sourceNode));
      return null;
    }

    return new Pair<>(result, exprs);
  }
}
