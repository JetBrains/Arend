package org.arend.typechecking.patternmatching;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.ElimBindingVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;

import java.util.*;

public class PatternTypechecking {
  private final ErrorReporter myErrorReporter;
  private final EnumSet<Flag> myFlags;
  private final CheckTypeVisitor myVisitor;
  private final TypecheckerState myState;
  private Map<Referable, Binding> myContext;
  private final boolean myFinal;

  public enum Flag { ALLOW_INTERVAL, ALLOW_CONDITIONS, CHECK_COVERAGE, CONTEXT_FREE }

  public PatternTypechecking(ErrorReporter errorReporter, EnumSet<Flag> flags, CheckTypeVisitor visitor, boolean isFinal) {
    myErrorReporter = errorReporter;
    myFlags = flags;
    myVisitor = visitor;
    myState = visitor.getTypecheckingState();
    myFinal = isFinal;
  }

  public PatternTypechecking(ErrorReporter errorReporter, EnumSet<Flag> flags, TypecheckerState state) {
    myErrorReporter = errorReporter;
    myFlags = flags;
    myVisitor = null;
    myState = state;
    myFinal = true;
  }

  private void collectBindings(List<Pattern> patterns) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof BindingPattern) {
        myVisitor.addBinding(null, ((BindingPattern) pattern).getBinding());
      } else if (pattern instanceof ConstructorPattern) {
        collectBindings(((ConstructorPattern) pattern).getArguments());
      }
    }
  }

  Pair<List<Pattern>, TypecheckingResult> typecheckClause(Concrete.FunctionClause clause, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams, Expression expectedType) {
    try (Utils.SetContextSaver<Referable> ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
        // Typecheck patterns
        ExprSubstitution substitution = new ExprSubstitution();
        Pair<List<Pattern>, List<Expression>> result = typecheckPatterns(clause.getPatterns(), abstractParameters, parameters, substitution, elimParams, clause);
        if (result == null) {
          return null;
        }

        // If we have the absurd pattern, then RHS is ignored
        if (result.proj2 == null) {
          if (clause.getExpression() != null) {
            myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.BODY_IGNORED, clause.getExpression()));
          }
          return new Pair<>(result.proj1, null);
        } else {
          if (clause.getExpression() == null) {
            myErrorReporter.report(new TypecheckingError("Required a body", clause));
            return null;
          }
        }

        Iterator<Map.Entry<Referable, Binding>> it = myContext.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<Referable, Binding> entry = it.next();
          Expression expr = substitution.get(entry.getValue());
          if (expr instanceof ReferenceExpression) {
            entry.setValue(((ReferenceExpression) expr).getBinding());
          } else if (expr != null) {
            it.remove();
          }
        }
        expectedType = expectedType.subst(substitution);

        GlobalInstancePool globalInstancePool = myVisitor.getInstancePool();
        InstancePool instancePool = globalInstancePool == null ? null : globalInstancePool.getInstancePool();
        if (instancePool != null) {
          globalInstancePool.setInstancePool(instancePool.subst(substitution));
        }

        // Typecheck the RHS
        TypecheckingResult tcResult;
        if (myFinal) {
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
  }

  public Pair<List<Pattern>, List<Expression>> typecheckPatterns(List<? extends Concrete.Pattern> patterns, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, ExprSubstitution substitution, List<DependentLink> elimParams, Concrete.SourceNode sourceNode) {
    myContext = myVisitor.getContext();
    if (myFlags.contains(Flag.CONTEXT_FREE)) {
      myContext.clear();
    }

    // Typecheck patterns
    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, substitution, elimParams, sourceNode);

    // Compute the context and the set of free bindings for CheckTypeVisitor
    if (result != null && result.proj2 != null && abstractParameters != null) {
      int i = 0;
      DependentLink link = parameters;
      if (!elimParams.isEmpty()) {
        for (Concrete.Parameter parameter : abstractParameters) {
          for (Referable referable : parameter.getReferableList()) {
            if (referable != null && !elimParams.contains(link)) {
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

  Pair<List<Pattern>, Map<Referable, Binding>> typecheckPatterns(List<? extends Concrete.Pattern> patterns, DependentLink parameters, Concrete.SourceNode sourceNode, @SuppressWarnings("SameParameterValue") boolean withElim) {
    myContext = new HashMap<>();
    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, new LinkList(), new ExprSubstitution(), sourceNode, withElim);
    if (result == null) {
      return null;
    }
    fixPatterns(result.proj1);
    return new Pair<>(result.proj1, result.proj2 == null ? null : myContext);
  }

  public List<Pattern> typecheckPatterns(List<? extends Concrete.Pattern> patterns, DependentLink parameters, List<DependentLink> elimParams) {
    if (patterns.isEmpty()) {
      return Collections.emptyList();
    }

    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, new ExprSubstitution(), elimParams, patterns.get(0));
    if (result == null) {
      return null;
    }
    if (elimParams.isEmpty()) {
      return result.proj1;
    }

    List<Pattern> list = new ArrayList<>();
    for (DependentLink elimParam : elimParams) {
      int i = 0;
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        if (link == elimParam) {
          break;
        }
        i++;
      }
      if (i < result.proj1.size()) {
        list.add(result.proj1.get(i));
      }
    }
    return list;
  }

  private Type typecheckType(Concrete.Expression cType, Expression expectedType) {
    if (cType == null || myVisitor == null) {
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
    if (myVisitor == null) {
      return;
    }

    if (expression == null || asPatterns.isEmpty()) {
      if (!asPatterns.isEmpty()) {
        myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.AS_PATTERN_IGNORED, asPatterns.get(0)));
      }
      return;
    }

    expectedType = expectedType.copy();
    for (Concrete.TypedReferable typedReferable : asPatterns) {
      Type type = typecheckType(typedReferable.type, expectedType);
      if (typedReferable.referable != null && myContext != null) {
        myContext.put(typedReferable.referable, new TypedEvaluatingBinding(typedReferable.referable.textRepresentation(), expression, type == null ? expectedType : type.getExpr()));
      }
    }
  }

  private Pair<List<Pattern>, List<Expression>> doTypechecking(List<? extends Concrete.Pattern> patterns, DependentLink parameters, ExprSubstitution substitution, List<DependentLink> elimParams, Concrete.SourceNode sourceNode) {
    // Put patterns in the correct order
    // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
    if (!elimParams.isEmpty()) {
      List<Concrete.Pattern> patterns1 = new ArrayList<>();
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        int index = elimParams.indexOf(link);
        patterns1.add(index < 0 || index >= patterns.size() ? null : patterns.get(index));
      }
      patterns = patterns1;
    }

    Pair<List<Pattern>, List<Expression>> result = doTypechecking(patterns, parameters, new LinkList(), substitution, sourceNode, !elimParams.isEmpty());
    if (result == null) {
      return null;
    }
    fixPatterns(result.proj1);
    return result;
  }

  private Pair<List<Pattern>, List<Expression>> doTypechecking(List<? extends Concrete.Pattern> patterns, DependentLink parameters, LinkList linkList, ExprSubstitution paramsSubst, Concrete.SourceNode sourceNode, boolean withElim) {
    List<Pattern> result = new ArrayList<>();
    List<Expression> exprs = new ArrayList<>();

    for (Concrete.Pattern pattern : patterns) {
      if (!parameters.hasNext()) {
        myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.TOO_MANY_PATTERNS, pattern));
        return null;
      }

      if (!withElim && pattern != null) {
        if (pattern.isExplicit()) {
          while (!parameters.isExplicit()) {
            DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
            linkList.append(newParam);
            result.add(new BindingPattern(newParam));
            if (exprs != null) {
              exprs.add(new ReferenceExpression(newParam));
            }
            if (myVisitor != null) {
              myVisitor.addBinding(null, newParam);
            }
            parameters = parameters.getNext();
            if (!parameters.hasNext()) {
              myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.TOO_MANY_PATTERNS, pattern));
              return null;
            }
          }
        } else {
          if (parameters.isExplicit()) {
            myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.EXPECTED_EXPLICIT_PATTERN, pattern));
            return null;
          }
        }
      }

      if (exprs == null || pattern == null || pattern instanceof Concrete.NamePattern) {
        if (pattern != null) {
          if (!(pattern instanceof Concrete.NamePattern)) {
            myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.PATTERN_IGNORED, pattern));
          } else if (!pattern.getAsReferables().isEmpty()) {
            myErrorReporter.report(new TypecheckingError(TypecheckingError.Kind.AS_PATTERN_IGNORED, pattern.getAsReferables().get(0)));
          }
        }

        Referable referable = null;
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        linkList.append(newParam);
        if (pattern instanceof Concrete.NamePattern) {
          Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
          referable = namePattern.getReferable();
          String name = referable == null ? null : referable.textRepresentation();
          if (name != null) {
            newParam.setName(name);
          }
          typecheckType(namePattern.type, newParam.getTypeExpr());
        }
        result.add(new BindingPattern(newParam));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(newParam));
        }
        if (referable != null && myContext != null) {
          myContext.put(referable, newParam);
        } else if (myVisitor != null) {
          myVisitor.addBinding(null, newParam);
        }
        parameters = parameters.getNext();
        continue;
      }

      Expression expr = parameters.getTypeExpr().subst(paramsSubst).normalize(NormalizeVisitor.Mode.WHNF);
      if (pattern instanceof Concrete.TuplePattern) {
        List<Concrete.Pattern> patternArgs = ((Concrete.TuplePattern) pattern).getPatterns();
        // Either sigma or class patterns
        SigmaExpression sigmaExpr = expr.cast(SigmaExpression.class);
        ClassCallExpression classCall = sigmaExpr == null ? expr.cast(ClassCallExpression.class) : null;
        if (sigmaExpr != null || classCall != null) {
          DependentLink newParameters = sigmaExpr != null ? DependentLink.Helper.copy(sigmaExpr.getParameters()) : classCall.getClassFieldParameters();
          Pair<List<Pattern>, List<Expression>> conResult = doTypechecking(patternArgs, newParameters, linkList, paramsSubst, pattern, false);
          if (conResult == null) {
            return null;
          }

          ConstructorPattern newPattern = sigmaExpr != null
            ? new ConstructorPattern(sigmaExpr, new Patterns(conResult.proj1))
            : new ConstructorPattern(classCall, new Patterns(conResult.proj1));
          result.add(newPattern);
          if (conResult.proj2 == null) {
            exprs = null;
            typecheckAsPatterns(pattern.getAsReferables(), null, null);
          } else {
            Expression newExpr = newPattern.toExpression(conResult.proj2);
            typecheckAsPatterns(pattern.getAsReferables(), newExpr, expr);
            exprs.add(newExpr);
            paramsSubst.add(parameters, newExpr);
          }

          parameters = parameters.getNext();
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

      // Defined constructor patterns
      if (pattern instanceof Concrete.ConstructorPattern) {
        Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
        Definition def = conPattern.getConstructor() instanceof TCReferable ? myState.getTypechecked((TCReferable) conPattern.getConstructor()) : null;
        if (def instanceof DConstructor) {
          if (myVisitor == null || ((DConstructor) def).getPattern() == null) {
            return null;
          }

          DConstructor constructor = (DConstructor) def;
          Sort sortArg = Sort.generateInferVars(myVisitor.getEquations(), def.hasUniverses(), conPattern);
          LevelSubstitution levelSubst = sortArg.toLevelSubstitution();
          DependentLink link = constructor.getParameters();
          ExprSubstitution substitution = new ExprSubstitution();
          List<Expression> args = new ArrayList<>();

          if (constructor == Prelude.IDP) {
            DataCallExpression dataCall = expr.cast(DataCallExpression.class);
            LamExpression typeLam = dataCall == null || dataCall.getDefinition() != Prelude.PATH ? null : dataCall.getDefCallArguments().get(0).normalize(NormalizeVisitor.Mode.WHNF).cast(LamExpression.class);
            Expression type = typeLam == null ? null : ElimBindingVisitor.elimLamBinding(typeLam);
            if (type == null) {
              myErrorReporter.report(new TypeMismatchError(expr, constructor.getResultType().subst(substitution, levelSubst), conPattern));
              return null;
            }

            Expression varExpr = dataCall.getDefCallArguments().get(2).normalize(NormalizeVisitor.Mode.WHNF);
            Expression otherExpr = dataCall.getDefCallArguments().get(1).normalize(NormalizeVisitor.Mode.WHNF);
            ReferenceExpression refExpr = varExpr.cast(ReferenceExpression.class);
            if (refExpr == null) {
              refExpr = otherExpr.cast(ReferenceExpression.class);
              otherExpr = varExpr;
            }
            if (refExpr == null) {
              myErrorReporter.report(new IdpPatternError(IdpPatternError.noVariable(), dataCall, conPattern));
              return null;
            }
            otherExpr = ElimBindingVisitor.elimBinding(otherExpr, refExpr.getBinding());
            type = ElimBindingVisitor.elimBinding(type, refExpr.getBinding());
            if (otherExpr == null || type == null) {
              myErrorReporter.report(new IdpPatternError(IdpPatternError.variable(refExpr.getBinding().getName()), dataCall, conPattern));
              return null;
            }

            args.add(type);
            substitution.add(link, type);
            link = link.getNext();
            args.add(otherExpr);
            substitution.add(link, otherExpr);
            link = link.getNext();

            ExprSubstitution varSubst = new ExprSubstitution(refExpr.getBinding(), otherExpr);
            paramsSubst.subst(varSubst);
            for (DependentLink paramLink = linkList.getFirst(); paramLink.hasNext(); paramLink = paramLink.getNext()) {
              paramLink = paramLink.getNextTyped(null);
              paramLink.setType(paramLink.getType().subst(new SubstVisitor(varSubst, LevelSubstitution.EMPTY)));
            }
          } else {
            if (constructor.getNumberOfParameters() > 0) {
              Set<Binding> bindings = myVisitor.getAllBindings();
              for (int i = 0; i < constructor.getNumberOfParameters(); i++) {
                Expression arg = new InferenceReferenceExpression(new FunctionInferenceVariable(constructor, link, i + 1, link.getTypeExpr().subst(substitution, levelSubst), conPattern, bindings), myVisitor.getEquations());
                args.add(arg);
                substitution.add(link, arg);
                link = link.getNext();
              }
            }

            Expression actualType = constructor.getResultType().subst(substitution, levelSubst).normalize(NormalizeVisitor.Mode.WHNF);
            if (!CompareVisitor.compare(myVisitor.getEquations(), Equations.CMP.EQ, actualType, expr, ExpectedType.OMEGA, conPattern)) {
              myErrorReporter.report(new TypeMismatchError(expr, actualType, conPattern));
              return null;
            }
          }
          LevelSubstitution levelSolution = myFinal ? myVisitor.getEquations().solve(conPattern) : LevelSubstitution.EMPTY;
          substitution.subst(levelSolution);

          Pair<List<Pattern>, List<Expression>> pair = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(link, substitution, levelSolution), linkList, paramsSubst, conPattern, false);
          if (pair == null) {
            return null;
          }

          Map<DependentLink, Pattern> patternSubst = new HashMap<>();
          for (Pattern patternArg : pair.proj1) {
            patternSubst.put(link, patternArg);
            link = link.getNext();
          }

          result.add(constructor.getPattern().subst(substitution, levelSolution, patternSubst));
          if (pair.proj2 == null) {
            exprs = null;
            typecheckAsPatterns(pattern.getAsReferables(), null, null);
          } else {
            args.addAll(pair.proj2);
            Expression newExpr = new FunCallExpression(constructor, sortArg.subst(levelSolution), args);
            typecheckAsPatterns(pattern.getAsReferables(), newExpr, expr);
            exprs.add(newExpr);
            paramsSubst.add(parameters, newExpr);
          }

          parameters = parameters.getNext();
          continue;
        }
      }

      // Constructor patterns
      DataCallExpression dataCall = expr.cast(DataCallExpression.class);
      if (dataCall == null) {
        if (!expr.isError()) {
          myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type"), expr, pattern));
        }
        return null;
      }
      if (!myFlags.contains(Flag.ALLOW_INTERVAL) && dataCall.getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      // Empty pattern
      if (pattern instanceof Concrete.TuplePattern) {
        List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
        if (conCalls == null) {
          myErrorReporter.report(new ImpossibleEliminationError(dataCall, pattern));
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
        Referable conRef = conPattern.getConstructor();
        if (constructor != null || conRef instanceof TCReferable && ((TCReferable) conRef).getKind() == GlobalReferable.Kind.CONSTRUCTOR) {
          myErrorReporter.report(new ExpectedConstructorError((GlobalReferable) conRef, dataCall, conPattern));
        }
        return null;
      }
      ConCallExpression conCall = conCalls.get(0);
      ExprSubstitution substitution = new ExprSubstitution();
      int i = 0;
      for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), i++) {
        substitution.add(link, conCall.getDataTypeArguments().get(i));
      }
      Pair<List<Pattern>, List<Expression>> conResult = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(constructor.getParameters(), substitution, new StdLevelSubstitution(conCall.getSortArgument())), linkList, paramsSubst, conPattern, false);
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
      } else {
        Expression newConCall = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), conResult.proj2);
        typecheckAsPatterns(pattern.getAsReferables(), newConCall, expr);
        exprs.add(newConCall);
        paramsSubst.add(parameters, newConCall);
      }
      parameters = parameters.getNext();
    }

    if (!withElim) {
      while (!parameters.isExplicit()) {
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        linkList.append(newParam);
        result.add(new BindingPattern(newParam));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(newParam));
        }
        parameters = parameters.getNext();
      }
    }

    if (parameters.hasNext()) {
      myErrorReporter.report(new NotEnoughPatternsError(DependentLink.Helper.size(parameters), sourceNode));
      return null;
    }

    return new Pair<>(result, exprs);
  }

  // Chains the bindings in the leaves of patterns
  private static void fixPatterns(List<Pattern> patterns) {
    List<DependentLink> leaves = new ArrayList<>();
    getLeaves(patterns, leaves);

    for (int i = 0; i < leaves.size(); i++) {
      DependentLink next = i < leaves.size() - 1 ? leaves.get(i + 1) : EmptyDependentLink.getInstance();
      DependentLink leaf = leaves.get(i);
      if (leaf.getNext() != next && leaf instanceof TypedDependentLink) {
        leaf.setNext(next);
      }
    }
  }

  private static void getLeaves(List<Pattern> patterns, List<DependentLink> leaves) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof ConstructorPattern) {
        getLeaves(((ConstructorPattern) pattern).getArguments(), leaves);
      } else if (pattern instanceof BindingPattern) {
        leaves.add(((BindingPattern) pattern).getBinding());
      }
    }
  }
}
