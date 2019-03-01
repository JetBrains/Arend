package org.arend.typechecking.patternmatching;

import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.Error;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.UnresolvedReference;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.DataTypeNotEmptyError;
import org.arend.typechecking.error.local.ExpectedConstructor;
import org.arend.typechecking.error.local.TypeMismatchError;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.*;

public class PatternTypechecking {
  private final LocalErrorReporter myErrorReporter;
  private final EnumSet<Flag> myFlags;
  private final CheckTypeVisitor myVisitor;
  private Map<Referable, Binding> myContext;

  public enum Flag { ALLOW_INTERVAL, ALLOW_CONDITIONS, CHECK_COVERAGE, CONTEXT_FREE }

  public PatternTypechecking(LocalErrorReporter errorReporter, EnumSet<Flag> flags, CheckTypeVisitor visitor) {
    myErrorReporter = errorReporter;
    myFlags = flags;
    myVisitor = visitor;
  }

  private void collectBindings(List<Pattern> patterns) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof BindingPattern) {
        myVisitor.getFreeBindings().add(((BindingPattern) pattern).getBinding());
      } else if (pattern instanceof ConstructorPattern) {
        collectBindings(((ConstructorPattern) pattern).getArguments());
      }
    }
  }

  Pair<List<Pattern>, CheckTypeVisitor.Result> typecheckClause(Concrete.FunctionClause clause, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams, Expression expectedType) {
    try (Utils.SetContextSaver<Referable> ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
      // Typecheck patterns
      Pair<List<Pattern>, List<Expression>> result = typecheckPatterns(clause.getPatterns(), abstractParameters, parameters, elimParams, clause);
      if (result == null) {
        return null;
      }

      // If we have the absurd pattern, then RHS is ignored
      if (result.proj2 == null) {
        if (clause.getExpression() != null) {
          myErrorReporter.report(new TypecheckingError(Error.Level.WARNING, "The RHS is ignored", clause.getExpression()));
        }
        return new Pair<>(result.proj1, null);
      } else {
        if (clause.getExpression() == null) {
          myErrorReporter.report(new TypecheckingError("Required a RHS", clause));
          return null;
        }
      }

      ExprSubstitution substitution = new ExprSubstitution();
      for (Expression expr : result.proj2) {
        substitution.add(parameters, expr);
        parameters = parameters.getNext();
      }
      for (Map.Entry<Referable, Binding> entry : myContext.entrySet()) {
        Expression expr = substitution.get(entry.getValue());
        if (expr != null) {
          entry.setValue(expr.cast(ReferenceExpression.class).getBinding());
        }
      }
      expectedType = expectedType.subst(substitution);

      GlobalInstancePool globalInstancePool = myVisitor.getInstancePool();
      InstancePool instancePool = globalInstancePool == null ? null : globalInstancePool.getInstancePool();
      if (instancePool != null) {
        globalInstancePool.setInstancePool(instancePool.subst(substitution));
      }

      // Typecheck the RHS
      CheckTypeVisitor.Result tcResult;
      if (abstractParameters != null) {
        tcResult = myVisitor.finalCheckExpr(clause.getExpression(), expectedType, false);
      } else {
        tcResult = myVisitor.checkExpr(clause.getExpression(), expectedType);
      }
      if (instancePool != null) {
        globalInstancePool.setInstancePool(instancePool);
      }
      return tcResult == null ? null : new Pair<>(result.proj1, tcResult);
    }
  }

  public Pair<List<Pattern>, List<Expression>> typecheckPatterns(List<? extends Concrete.Pattern> patterns, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams, Concrete.SourceNode sourceNode) {
    myContext = myVisitor.getContext();
    if (myFlags.contains(Flag.CONTEXT_FREE)) {
      myContext.clear();
    }

    // Typecheck patterns
    Pair<List<Pattern>, List<Expression>> result;
    if (!elimParams.isEmpty()) {
      // Put patterns in the correct order
      // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
      List<Concrete.Pattern> patterns1 = new ArrayList<>();
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        int index = elimParams.indexOf(link);
        patterns1.add(index < 0 ? null : patterns.get(index));
      }
      result = doTypechecking(patterns1, DependentLink.Helper.subst(parameters, new ExprSubstitution()), sourceNode, true);
    } else {
      result = doTypechecking(patterns, DependentLink.Helper.subst(parameters, new ExprSubstitution()), sourceNode, false);
    }

    // Compute the context and the set of free bindings for CheckTypeVisitor
    if (result != null && result.proj2 != null && abstractParameters != null) {
      int i = 0;
      DependentLink link = parameters;
      if (!elimParams.isEmpty()) {
        for (Concrete.Parameter parameter : abstractParameters) {
          for (Referable referable : parameter.getReferableList()) {
            if (!elimParams.contains(link)) {
              myContext.put(referable, ((BindingPattern) result.proj1.get(i)).getBinding());
            }
            link = link.getNext();
            i++;
          }
        }
      }

      if (myFlags.contains(Flag.CONTEXT_FREE)) {
        myVisitor.getFreeBindings().clear();
      }
      collectBindings(result.proj1);
    }

    return result;
  }

  Pair<List<Pattern>, Map<Referable, Binding>> typecheckPatterns(List<? extends Concrete.Pattern> patterns, DependentLink parameters, Concrete.SourceNode sourceNode, @SuppressWarnings("SameParameterValue") boolean fullList) {
    myContext = new HashMap<>();
    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, sourceNode, fullList);
    return result == null ? null : new Pair<>(result.proj1, result.proj2 == null ? null : myContext);
  }

  private static DependentLink getFirstBinding(List<Pattern> patterns) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof EmptyPattern) {
        return EmptyDependentLink.getInstance();
      }
      if (pattern instanceof BindingPattern) {
        return ((BindingPattern) pattern).getBinding();
      } else if (pattern instanceof ConstructorPattern) {
        DependentLink last = getFirstBinding(((ConstructorPattern) pattern).getArguments());
        if (last != null) {
          return last;
        }
      }
    }
    return null;
  }

  private Type typecheckType(Concrete.Expression cType, Expression expectedType) {
    if (cType == null) {
      return null;
    }

    Type type = myVisitor.checkType(cType, ExpectedType.OMEGA);
    if (type != null && !expectedType.isLessOrEquals(type.getExpr(), myVisitor.getEquations(), cType)) {
      myErrorReporter.report(new TypeMismatchError(type.getExpr(), expectedType, cType));
      return null;
    }
    return type;
  }

  private void typecheckAsPatterns(List<Concrete.TypedReferable> asPatterns, Expression expression, Expression expectedType) {
    if (expression == null) {
      if (!asPatterns.isEmpty()) {
        myErrorReporter.report(new TypecheckingError(Error.Level.WARNING, "As-pattern is ignored", asPatterns.get(0)));
      }
      return;
    }

    for (Concrete.TypedReferable typedReferable : asPatterns) {
      Type type = typecheckType(typedReferable.type, expectedType);
      if (typedReferable.referable != null) {
        myContext.put(typedReferable.referable, new EvaluatingBinding(typedReferable.referable.textRepresentation(), expression, type == null ? expectedType : type.getExpr()));
      }
    }
  }

  private Pair<List<Pattern>, List<Expression>> doTypechecking(List<? extends Concrete.Pattern> patterns, DependentLink parameters, Concrete.SourceNode sourceNode, boolean fullList) {
    List<Pattern> result = new ArrayList<>();
    List<Expression> exprs = new ArrayList<>();

    for (Concrete.Pattern pattern : patterns) {
      if (!parameters.hasNext()) {
        myErrorReporter.report(new TypecheckingError("Too many patterns", pattern));
        return null;
      }

      if (!fullList && pattern != null) {
        if (pattern.isExplicit()) {
          while (!parameters.isExplicit()) {
            result.add(new BindingPattern(parameters));
            if (exprs != null) {
              exprs.add(new ReferenceExpression(parameters));
            }
            parameters = parameters.getNext();
            if (!parameters.hasNext()) {
              myErrorReporter.report(new TypecheckingError("Too many patterns", pattern));
              return null;
            }
          }
        } else {
          if (parameters.isExplicit()) {
            myErrorReporter.report(new TypecheckingError("Expected an explicit pattern", pattern));
            return null;
          }
        }
      }

      if (exprs == null || pattern == null || pattern instanceof Concrete.NamePattern) {
        if (!(pattern == null || pattern instanceof Concrete.NamePattern)) {
          myErrorReporter.report(new TypecheckingError(Error.Level.WARNING, "This pattern is ignored", pattern));
        }
        Referable referable = null;
        if (pattern instanceof Concrete.NamePattern) {
          Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
          referable = namePattern.getReferable();
          String name = referable == null ? null : referable.textRepresentation();
          if (name != null) {
            parameters.setName(name);
          }
          typecheckType(namePattern.type, parameters.getTypeExpr());
        }
        result.add(new BindingPattern(parameters));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(parameters));
          if (pattern != null) {
            myContext.put(referable, parameters);
          }
        }
        parameters = parameters.getNext();
        continue;
      }

      Expression expr = parameters.getTypeExpr().normalize(NormalizeVisitor.Mode.WHNF);
      if (pattern instanceof Concrete.TuplePattern) {
        List<Concrete.Pattern> patternArgs = ((Concrete.TuplePattern) pattern).getPatterns();
        // Either sigma or class patterns
        if (expr.isInstance(SigmaExpression.class) || expr.isInstance(ClassCallExpression.class)) {
          DependentLink newParameters = expr.isInstance(SigmaExpression.class) ? expr.cast(SigmaExpression.class).getParameters() : expr.cast(ClassCallExpression.class).getClassFieldParameters();
          Pair<List<Pattern>, List<Expression>> conResult = doTypechecking(patternArgs, newParameters, pattern, false);
          if (conResult == null) {
            return null;
          }

          ConstructorPattern newPattern = expr.isInstance(SigmaExpression.class)
            ? new ConstructorPattern(expr.cast(SigmaExpression.class), new Patterns(conResult.proj1))
            : new ConstructorPattern(expr.cast(ClassCallExpression.class), new Patterns(conResult.proj1));
          result.add(newPattern);
          if (conResult.proj2 == null) {
            exprs = null;
            typecheckAsPatterns(pattern.getAsReferables(), null, null);
            parameters = parameters.getNext();
          } else {
            Expression newExpr = newPattern.toExpression(conResult.proj2);
            typecheckAsPatterns(pattern.getAsReferables(), newExpr, parameters.getTypeExpr());
            exprs.add(newExpr);
            parameters = DependentLink.Helper.subst(parameters.getNext(), new ExprSubstitution(parameters, newExpr));
          }

          continue;
        } else {
          if (!patternArgs.isEmpty()) {
            if (!expr.isError()) {
              myErrorReporter.report(new TypeMismatchError(DocFactory.text("a sigma type or a class"), expr, pattern));
            }
            return null;
          }
          if (!expr.isInstance(DataCallExpression.class)) {
            if (!expr.isError()) {
              myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type, a sigma type, or a class"), expr, pattern));
            }
            return null;
          }
        }
      }

      // Constructor patterns
      if (!expr.isInstance(DataCallExpression.class)) {
        if (!expr.isError()) {
          myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type"), expr, pattern));
        }
        return null;
      }
      DataCallExpression dataCall = expr.cast(DataCallExpression.class);
      if (!myFlags.contains(Flag.ALLOW_INTERVAL) && dataCall.getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      // Empty pattern
      if (pattern instanceof Concrete.TuplePattern) {
        List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
        if (conCalls == null) {
          myErrorReporter.report(new TypecheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", pattern));
          return null;
        }
        if (!conCalls.isEmpty()) {
          List<Constructor> constructors = new ArrayList<>(conCalls.size());
          for (ConCallExpression conCall : conCalls) {
            constructors.add(conCall.getDefinition());
          }
          myErrorReporter.report(new DataTypeNotEmptyError(dataCall, constructors, pattern));
          return null;
        }
        result.add(EmptyPattern.INSTANCE);
        exprs = null;
        typecheckAsPatterns(pattern.getAsReferables(), null, null);
        parameters = parameters.getNext();
        continue;
      }

      if (!(pattern instanceof Concrete.ConstructorPattern)) {
        throw new IllegalStateException();
      }
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;

      if (dataCall.getDefinition() == Prelude.INT && (conPattern.getConstructor() == Prelude.ZERO.getReferable() || conPattern.getConstructor() == Prelude.SUC.getReferable())) {
        boolean isExplicit = conPattern.isExplicit();
        conPattern.setExplicit(true);
        conPattern = new Concrete.ConstructorPattern(conPattern.getData(), isExplicit, Prelude.POS.getReferable(), Collections.singletonList(conPattern), conPattern.getAsReferables());
      }

      Constructor constructor = conPattern.getConstructor() instanceof GlobalReferable ? dataCall.getDefinition().getConstructor((GlobalReferable) conPattern.getConstructor()) : null;
      List<ConCallExpression> conCalls = new ArrayList<>(1);
      if (constructor == null || !dataCall.getMatchedConCall(constructor, conCalls) || conCalls.isEmpty() ) {
        if (!(conPattern.getConstructor() instanceof UnresolvedReference)) {
          myErrorReporter.report(new ExpectedConstructor(conPattern.getConstructor(), dataCall, conPattern));
        }
        return null;
      }
      ConCallExpression conCall = conCalls.get(0);
      ExprSubstitution substitution = new ExprSubstitution();
      int i = 0;
      for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), i++) {
        substitution.add(link, conCall.getDataTypeArguments().get(i));
      }
      Pair<List<Pattern>, List<Expression>> conResult = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(constructor.getParameters(), substitution, new StdLevelSubstitution(conCall.getSortArgument())), conPattern, false);
      if (conResult == null) {
        return null;
      }

      if (!myFlags.contains(Flag.ALLOW_CONDITIONS)) {
        if (conCall.getDefinition().getBody() instanceof IntervalElim) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with interval conditions is not allowed here", conPattern));
          return null;
        }
        if (conCall.getDefinition().getBody() instanceof ElimTree && NormalizeVisitor.INSTANCE.doesEvaluate((ElimTree) conCall.getDefinition().getBody(), conResult.proj2, true)) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with conditions is allowed only when patterns cannot evaluate", conPattern));
          return null;
        }
      }

      result.add(new ConstructorPattern(conCall, new Patterns(conResult.proj1)));
      if (conResult.proj2 == null) {
        exprs = null;
        typecheckAsPatterns(pattern.getAsReferables(), null, null);
        parameters = parameters.getNext();
      } else {
        Expression newConCall = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), conResult.proj2);
        typecheckAsPatterns(pattern.getAsReferables(), newConCall, parameters.getTypeExpr());
        exprs.add(newConCall);
        parameters = DependentLink.Helper.subst(parameters.getNext(), new ExprSubstitution(parameters, newConCall));
      }
    }

    if (!fullList) {
      while (!parameters.isExplicit()) {
        result.add(new BindingPattern(parameters));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(parameters));
        }
        parameters = parameters.getNext();
      }
    }

    if (parameters.hasNext()) {
      myErrorReporter.report(new TypecheckingError("Not enough patterns, expected " + DependentLink.Helper.size(parameters) + " more", sourceNode));
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    fixPatterns(result, substitution);
    if (!substitution.isEmpty()) {
      if (exprs != null) {
        for (int i = 0; i < exprs.size(); i++) {
          exprs.set(i, exprs.get(i).subst(substitution));
        }
      }

      for (Map.Entry<Referable, Binding> entry : myContext.entrySet()) {
        Expression expr = substitution.get(entry.getValue());
        if (expr != null) {
          entry.setValue(((ReferenceExpression) expr).getBinding());
        }
      }

      DependentLink link = getFirstBinding(result);
      if (link != null && link.hasNext()) {
        link = link.getNext();
        for (; link.hasNext(); link = link.getNext()) {
          link = link.getNextTyped(null);
          link.setType(link.getType().subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)));
        }
      }
    }

    return new Pair<>(result, exprs);
  }

  // Chains the bindings in the leaves of patterns
  private void fixPatterns(List<Pattern> patterns, ExprSubstitution substitution) {
    List<DependentLink> leaves = new ArrayList<>();
    getLeaves(patterns, leaves);

    for (int i = 0; i < leaves.size(); i++) {
      DependentLink next = i < leaves.size() - 1 ? leaves.get(i + 1) : EmptyDependentLink.getInstance();
      DependentLink leaf = leaves.get(i);
      if (leaf.getNext() == next) {
        continue;
      }
      if (leaf instanceof TypedDependentLink) {
        leaf.setNext(next);
      } else {
        TypedDependentLink newLeaf = new TypedDependentLink(leaf.isExplicit(), leaf.getName(), leaf.getType(), next);
        substitution.add(leaf, new ReferenceExpression(newLeaf));
        leaves.set(i, newLeaf);
        if (i > 0) {
          leaves.get(i - 1).setNext(newLeaf);
        }
      }
    }

    if (!substitution.isEmpty()) {
      setLeaves(patterns, leaves, 0);
    }
  }

  private void getLeaves(List<Pattern> patterns, List<DependentLink> leaves) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof ConstructorPattern) {
        getLeaves(((ConstructorPattern) pattern).getArguments(), leaves);
      } else if (pattern instanceof BindingPattern) {
        leaves.add(((BindingPattern) pattern).getBinding());
      }
    }
  }

  private int setLeaves(List<Pattern> patterns, List<DependentLink> leaves, int j) {
    for (int i = 0; i < patterns.size(); i++) {
      Pattern pattern = patterns.get(i);
      if (pattern instanceof BindingPattern) {
        patterns.set(i, new BindingPattern(leaves.get(j++)));
      } else if (pattern instanceof ConstructorPattern) {
        j = setLeaves(((ConstructorPattern) pattern).getArguments(), leaves, j);
      }
    }
    return j;
  }
}
