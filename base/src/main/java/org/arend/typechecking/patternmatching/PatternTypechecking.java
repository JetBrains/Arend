package org.arend.typechecking.patternmatching;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.UntypedDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.ElimBindingVisitor;
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.concrete.pattern.ConcretePattern;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.extImpl.definitionRenamer.PatternContextDataImpl;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.LevelEquationsSolver;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DesugarVisitor;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class PatternTypechecking {
  private final ErrorReporter myErrorReporter;
  private final Mode myMode;
  private final CheckTypeVisitor myVisitor;
  private Map<Referable, Binding> myContext;
  private final boolean myFinal;
  private final List<Expression> myCaseArguments;
  private final List<DependentLink> myElimParams;
  private final LinkList myLinkList = new LinkList();

  private DependentLink clausesParameters = null;

  public enum Mode {
    DATA {
      @Override public boolean allowIdp() { return false; }
      @Override public boolean allowInterval() { return false; }
      @Override public boolean allowConditions() { return false; }
      @Override public boolean checkCoverage() { return false; }
    },
    FUNCTION,
    CONSTRUCTOR {
      @Override public boolean allowIdp() { return false; }
      @Override public boolean checkCoverage() { return false; }
      @Override public boolean isContextFree() { return false; }
    },
    CASE {
      @Override public boolean allowInterval() { return false; }
      @Override public boolean isContextFree() { return false; }
    };

    public boolean allowIdp() { return true; }
    public boolean allowInterval() { return true; }
    public boolean allowConditions() { return true; }
    public boolean checkCoverage() { return true; }
    public boolean isContextFree() { return true; }
  }

  public PatternTypechecking(ErrorReporter errorReporter, Mode mode, CheckTypeVisitor visitor, boolean isFinal, @Nullable List<Expression> caseArguments, @NotNull List<DependentLink> elimParams) {
    myErrorReporter = errorReporter;
    myMode = mode;
    myVisitor = visitor;
    myFinal = isFinal;
    myCaseArguments = caseArguments;
    myElimParams = elimParams;
  }

  public PatternTypechecking(ErrorReporter errorReporter, Mode mode) {
    myErrorReporter = errorReporter;
    myMode = mode;
    myVisitor = null;
    myFinal = true;
    myCaseArguments = null;
    myElimParams = Collections.emptyList();
  }

  private void addBinding(Referable referable, Binding binding) {
    if (myVisitor != null) {
      myVisitor.addBinding(referable, binding);
    } else if (myContext != null && referable != null) {
      myContext.put(referable, binding);
    }
  }

  public List<ExtElimClause> typecheckClauses(List<Concrete.FunctionClause> clauses, DependentLink parameters, Expression expectedType) {
    return typecheckClauses(clauses, null, parameters, expectedType);
  }

  public List<ExtElimClause> typecheckClauses(List<Concrete.FunctionClause> clauses, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, Expression expectedType) {
    this.clausesParameters = parameters;

    List<ExtElimClause> result = new ArrayList<>(clauses.size());
    boolean ok = true;
    for (Concrete.FunctionClause clause : clauses) {
      ExtElimClause elimClause = typecheckClause(clause, abstractParameters, parameters, expectedType);
      if (elimClause == null) {
        ok = false;
      } else {
        result.add(elimClause);
      }
    }
    return ok ? result : null;
  }

  private ExtElimClause typecheckClause(Concrete.FunctionClause clause, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, Expression expectedType) {
    assert myVisitor != null;
    try (var ignored = new Utils.SetContextSaver<>(myVisitor.getContext())) {
      // Typecheck patterns
      ExprSubstitution substitution = new ExprSubstitution();
      ExprSubstitution totalSubst = new ExprSubstitution();
      Result result = typecheckPatterns(clause.getPatterns(), abstractParameters, parameters, substitution, totalSubst, clause);
      if (result == null) {
        return null;
      }

      ExtElimClause elimClause = new ExtElimClause(result.patterns, null, totalSubst);

      // If we have the absurd pattern, then RHS is ignored
      if (result.hasEmptyPattern()) {
        if (clause.getExpression() != null) {
          myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.BODY_IGNORED, clause.getExpression()));
        }
        return elimClause;
      } else {
        if (clause.getExpression() == null) {
          myErrorReporter.report(new TypecheckingError("Required a body", clause));
          return null;
        }
      }

      for (Map.Entry<Referable, Binding> entry : myContext.entrySet()) {
        Binding binding = entry.getValue();
        Expression expr = substitution.get(binding);
        Binding newBinding = expr instanceof ReferenceExpression
          ? ((ReferenceExpression) expr).getBinding()
          : expr != null
            ? new TypedEvaluatingBinding(binding.getName(), expr, binding.getTypeExpr())
            : null;
        if (newBinding != null) {
          entry.setValue(newBinding);
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
        tcResult = myVisitor.finalCheckExpr(clause.getExpression(), expectedType);
      } else {
        tcResult = myVisitor.checkExpr(clause.getExpression(), expectedType);
      }
      if (instancePool != null) {
        globalInstancePool.setInstancePool(instancePool);
      }
      if (tcResult == null) {
        return null;
      }
      elimClause.setExpression(tcResult.expression);
      return elimClause;
    }
  }

  public Result typecheckPatterns(List<Concrete.Pattern> patterns, List<? extends Concrete.Parameter> concreteParams, DependentLink parameters, ExprSubstitution substitution, ExprSubstitution totalSubst, ConcreteSourceNode sourceNode) {
    assert myVisitor != null;
    myContext = myVisitor.getContext();
    if (myMode.isContextFree()) {
      myContext.clear();
    }

    return doTypechecking(patterns, concreteParams, parameters, substitution, totalSubst, sourceNode);
  }

  @TestOnly
  Pair<List<ExpressionPattern>, Map<Referable, Binding>> typecheckPatterns(List<Concrete.Pattern> patterns, DependentLink parameters, Concrete.SourceNode sourceNode, @SuppressWarnings("SameParameterValue") boolean withElim) {
    myContext = myVisitor == null ? new HashMap<>() : myVisitor.getContext();
    myLinkList.clear();
    Result result = doTypechecking(patterns, parameters, new ExprSubstitution(), null, sourceNode, withElim);
    return result == null ? null : new Pair<>(result.patterns, result.exprs == null ? null : myContext);
  }

  private Type typecheckType(Concrete.Expression cType, Expression expectedType) {
    if (cType == null || myVisitor == null) {
      return null;
    }

    Type type = myVisitor.checkType(cType, Type.OMEGA);
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
        myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.AS_PATTERN_IGNORED, asPatterns.get(0)));
      }
      return;
    }

    expectedType = expectedType.copy();
    for (Concrete.TypedReferable typedReferable : asPatterns) {
      Type type = typecheckType(typedReferable.type, expectedType);
      if (typedReferable.referable != null) {
        addBinding(typedReferable.referable, new TypedEvaluatingBinding(typedReferable.referable.textRepresentation(), expression, type == null ? expectedType : type.getExpr()));
      }
    }
  }

  private Result doTypechecking(List<Concrete.Pattern> patterns, List<? extends Concrete.Parameter> concreteParams, DependentLink parameters, ExprSubstitution paramSubst, ExprSubstitution totalSubst, ConcreteSourceNode sourceNode) {
    // Put patterns in the correct order
    // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
    if (!myElimParams.isEmpty()) {
      List<Pair<Object, Referable>> params;
      if (concreteParams != null) {
        params = new ArrayList<>();
        for (Concrete.Parameter param : concreteParams) {
          for (Referable ref : param.getReferableList()) {
            params.add(new Pair<>(param.getData(), ref));
          }
        }
      } else {
        params = Collections.emptyList();
      }
      List<Concrete.Pattern> patterns1 = new ArrayList<>();
      int i = 0;
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), i++) {
        int index = myElimParams.indexOf(link);
        patterns1.add(index < 0 || index >= patterns.size() ? (i < params.size() ? new Concrete.NamePattern(params.get(i).proj1, true, params.get(i).proj2, null) : null) : patterns.get(index));
      }
      patterns = patterns1;
    }

    myLinkList.clear();
    return doTypechecking(patterns, parameters, paramSubst, totalSubst, sourceNode, !myElimParams.isEmpty());
  }

  public static class Result {
    private final List<ExpressionPattern> patterns;
    private final List<Expression> exprs;
    private final ExprSubstitution varSubst; // Substitutes e for x if we matched on a path e = x

    private Result(List<ExpressionPattern> patterns, List<Expression> exprs, ExprSubstitution varSubst) {
      this.patterns = patterns;
      this.exprs = exprs;
      this.varSubst = varSubst;
    }

    public List<ExpressionPattern> getPatterns() {
      return patterns;
    }

    public boolean hasEmptyPattern() {
      return exprs == null;
    }
  }

  private static void listSubst(List<ExpressionPattern> patterns, List<Expression> exprs, ExprSubstitution varSubst) {
    if (varSubst == null) {
      return;
    }
    for (int i = 0; i < patterns.size(); i++) {
      patterns.set(i, patterns.get(i).subst(varSubst, LevelSubstitution.EMPTY, null));
    }
    if (exprs == null) {
      return;
    }
    for (int i = 0; i < exprs.size(); i++) {
      exprs.set(i, exprs.get(i).subst(varSubst, LevelSubstitution.EMPTY));
    }
  }

  private Result doTypechecking(List<Concrete.Pattern> patterns, DependentLink parameters, ExprSubstitution paramsSubst, ExprSubstitution totalSubst, ConcreteSourceNode sourceNode, boolean withElim) {
    List<ExpressionPattern> result = new ArrayList<>();
    List<Expression> exprs = new ArrayList<>();
    ExprSubstitution varSubst = new ExprSubstitution();

    for (int k = 0; k <= patterns.size(); k++) {
      Concrete.Pattern pattern = k < patterns.size() ? patterns.get(k) : null;
      if (!parameters.hasNext()) {
        if (k == patterns.size()) {
          break;
        }
        myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, pattern == null ? sourceNode : pattern));
        return null;
      }

      if (!withElim && (pattern != null || k == patterns.size())) {
        if (k == patterns.size() || pattern.isExplicit()) {
          while (!parameters.isExplicit()) {
            DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
            myLinkList.append(newParam);
            result.add(new BindingPattern(newParam));
            if (exprs != null) {
              exprs.add(new ReferenceExpression(newParam));
            }
            addBinding(null, newParam);
            parameters = parameters.getNext();
            if (!parameters.hasNext()) {
              if (k == patterns.size()) {
                break;
              }
              myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, pattern == null ? sourceNode : pattern));
              return null;
            }
          }
          if (k == patterns.size()) {
            break;
          }
        } else {
          if (parameters.isExplicit()) {
            myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.EXPECTED_EXPLICIT_PATTERN, pattern));
            return null;
          }
        }
      }

      if (exprs == null || pattern == null || pattern instanceof Concrete.NamePattern) {
        if (pattern != null) {
          if (!(pattern instanceof Concrete.NamePattern)) {
            myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.PATTERN_IGNORED, pattern));
          } else if (!pattern.getAsReferables().isEmpty()) {
            myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.AS_PATTERN_IGNORED, pattern.getAsReferables().get(0)));
          }
        }

        Referable referable = null;
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        myLinkList.append(newParam);
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
        addBinding(referable, newParam);
        parameters = parameters.getNext();
        continue;
      }

      Expression expr = parameters.getTypeExpr().subst(paramsSubst).normalize(NormalizationMode.WHNF);

      if (pattern instanceof Concrete.NumberPattern) {
        var newPattern = translateNumberPatterns((Concrete.NumberPattern) pattern, expr);
        if (newPattern == null) {
          return null;
        }
        newPattern.setExplicit(pattern.isExplicit());
        if (newPattern instanceof Concrete.NamePattern) {
          patterns.set(k--, newPattern);
          continue;
        } else {
          pattern = newPattern;
        }
      }

      if (pattern instanceof Concrete.TuplePattern) {
        List<Concrete.Pattern> patternArgs = ((Concrete.TuplePattern) pattern).getPatterns();
        // Either sigma or class patterns
        SigmaExpression sigmaExpr = expr.cast(SigmaExpression.class);
        ClassCallExpression classCall = sigmaExpr == null ? expr.cast(ClassCallExpression.class) : null;
        if (sigmaExpr != null || classCall != null) {
          DependentLink newParameters = sigmaExpr != null ? DependentLink.Helper.copy(sigmaExpr.getParameters()) : classCall.getClassFieldParameters();
          Result conResult = doTypechecking(patternArgs, newParameters, paramsSubst, totalSubst, pattern, false);
          if (conResult == null) {
            return null;
          }
          varSubst.addSubst(conResult.varSubst);
          listSubst(result, exprs, conResult.varSubst);

          ConstructorExpressionPattern newPattern = sigmaExpr != null
            ? new ConstructorExpressionPattern(!conResult.varSubst.isEmpty() ? (SigmaExpression) new SubstVisitor(conResult.varSubst, LevelSubstitution.EMPTY).visitSigma(sigmaExpr, null) : sigmaExpr, conResult.patterns)
            : new ConstructorExpressionPattern(!conResult.varSubst.isEmpty() ? (ClassCallExpression) new SubstVisitor(conResult.varSubst, LevelSubstitution.EMPTY).visitClassCall(classCall, null) : classCall, conResult.patterns);
          result.add(newPattern);
          if (conResult.exprs == null) {
            exprs = null;
            typecheckAsPatterns(pattern.getAsReferables(), null, null);
          } else {
            Expression newExpr = newPattern.toExpression(conResult.exprs);
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
        Definition def = conPattern.getConstructor() instanceof TCDefReferable ? ((TCDefReferable) conPattern.getConstructor()).getTypechecked() : null;
        if (def instanceof DConstructor) {
          if (myVisitor == null || ((DConstructor) def).getPattern() == null) {
            return null;
          }

          DConstructor constructor = (DConstructor) def;
          Sort sortArg;
          DependentLink link = constructor.getParameters();
          ExprSubstitution substitution = new ExprSubstitution();
          List<Expression> args = new ArrayList<>();

          if (constructor == Prelude.IDP) {
            if (!myMode.allowIdp()) {
              myErrorReporter.report(new TypecheckingError("Pattern matching on idp is not allowed here", pattern));
              return null;
            }

            DataCallExpression dataCall = expr.cast(DataCallExpression.class);
            LamExpression typeLam = dataCall == null || dataCall.getDefinition() != Prelude.PATH ? null : dataCall.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF).cast(LamExpression.class);
            Expression type = ElimBindingVisitor.elimLamBinding(typeLam);
            if (type == null) {
              myErrorReporter.report(new TypeMismatchError(expr, constructor.getResultType().subst(substitution), conPattern));
              return null;
            }

            Sort actualSort = type.getSortOfType();
            if (actualSort == null) {
              Sort dataSort = dataCall.getSortOfType();
              sortArg = new Sort(dataSort.getPLevel(), dataSort.getHLevel().add(1));
            } else {
              sortArg = actualSort;
            }

            Expression expr1 = dataCall.getDefCallArguments().get(2).normalize(NormalizationMode.WHNF);
            Expression expr2 = dataCall.getDefCallArguments().get(1).normalize(NormalizationMode.WHNF);
            ReferenceExpression refExpr1 = expr1.cast(ReferenceExpression.class);
            ReferenceExpression refExpr2 = expr2.cast(ReferenceExpression.class);
            if (refExpr1 == null && refExpr2 == null) {
              myErrorReporter.report(new IdpPatternError(IdpPatternError.noVariable(), dataCall, conPattern));
              return null;
            }

            int num = 0;
            for (DependentLink paramLink = myLinkList.getFirst(); paramLink.hasNext(); paramLink = paramLink.getNext()) {
              if (refExpr1 != null && refExpr1.getBinding() == paramLink) {
                if (num == 2) {
                  num = 1;
                  break;
                } else {
                  num = 1;
                }
              }
              if (refExpr2 != null && refExpr2.getBinding() == paramLink) {
                if (num == 1) {
                  num = 2;
                  break;
                } else {
                  num = 2;
                }
              }
            }
            if (num == 0) {
              myErrorReporter.report(new IdpPatternError(IdpPatternError.noParameter(), dataCall, conPattern));
              return null;
            }
            Binding substVar = num == 1 ? refExpr1.getBinding() : refExpr2.getBinding();
            Expression otherExpr = ElimBindingVisitor.elimBinding(num == 1 ? expr2 : expr1, substVar);
            if (otherExpr == null) {
              myErrorReporter.report(new IdpPatternError(IdpPatternError.variable(substVar.getName()), dataCall, conPattern));
              return null;
            }
            Expression otherExpr2 = ElimBindingVisitor.elimBinding(num == 1 ? dataCall.getDefCallArguments().get(1) : dataCall.getDefCallArguments().get(2), substVar);
            if (otherExpr2 == null) {
              otherExpr2 = otherExpr;
            }

            args.add(type);
            substitution.add(link, type);
            link = link.getNext();
            args.add(otherExpr);
            substitution.add(link, otherExpr);
            link = link.getNext();

            varSubst.addSubst(substVar, otherExpr);
            if (totalSubst != null) {
              totalSubst.addSubst(substVar, otherExpr);
            }
            paramsSubst.addSubst(substVar, otherExpr2);
            Set<Binding> freeVars = FreeVariablesCollector.getFreeVariables(otherExpr);
            Binding banVar = null;
            List<DependentLink> params = DependentLink.Helper.toList(myLinkList.getFirst());
            for (int i = params.size() - 1; i >= 0; i--) {
              DependentLink paramLink = params.get(i);
              if (paramLink == substVar) {
                break;
              }
              if (freeVars.contains(paramLink)) {
                banVar = paramLink;
              }
              if (paramLink instanceof UntypedDependentLink) {
                continue;
              }
              if (banVar != null && paramLink.getTypeExpr().findBinding(substVar)) {
                myErrorReporter.report(new IdpPatternError(IdpPatternError.subst(substVar.getName(), paramLink.getName(), banVar.getName()), null, conPattern));
                return null;
              }
              assert paramLink != null;
              paramLink.setType(paramLink.getType().subst(new SubstVisitor(varSubst, LevelSubstitution.EMPTY)));
            }
            listSubst(result, exprs, varSubst);
          } else {
            sortArg = Sort.generateInferVars(myVisitor.getEquations(), def.getUniverseKind(), conPattern);
            LevelSubstitution levelSubst = sortArg.toLevelSubstitution();

            FreeVariablesCollector collector = new FreeVariablesCollector();
            constructor.getResultType().accept(collector, null);
            if (constructor.getNumberOfParameters() > 0 || !collector.getResult().isEmpty()) {
              Set<Binding> bindings = myVisitor.getAllBindings();
              int i = 0;
              for (; i < constructor.getNumberOfParameters(); i++) {
                Expression arg = InferenceReferenceExpression.make(new FunctionInferenceVariable(constructor, link, i + 1, link.getTypeExpr().subst(substitution, levelSubst), conPattern, bindings), myVisitor.getEquations());
                args.add(arg);
                substitution.add(link, arg);
                collector.getResult().remove(link);
                link = link.getNext();
              }
              if (!collector.getResult().isEmpty()) {
                for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
                  substitution.add(link1, InferenceReferenceExpression.make(new FunctionInferenceVariable(constructor, link1, i + 1, link1.getTypeExpr().subst(substitution, levelSubst), conPattern, bindings), myVisitor.getEquations()));
                }
              }
            }

            Expression actualType = constructor.getResultType().subst(substitution, levelSubst).normalize(NormalizationMode.WHNF);
            if (!CompareVisitor.compare(myVisitor.getEquations(), CMP.EQ, actualType, expr, Type.OMEGA, conPattern)) {
              myErrorReporter.report(new TypeMismatchError(expr, actualType, conPattern));
              return null;
            }
            myVisitor.getEquations().solveEquations();

            Map<DependentLink, Concrete.Pattern> concretePatterns = new HashMap<>();
            DependentLink param = constructor.getParameters();
            for (int j = 0; j < constructor.getNumberOfParameters() && param.hasNext(); j++) {
              param = param.getNext();
            }

            for (Concrete.Pattern subPattern : conPattern.getPatterns()) {
              while (param.hasNext() && subPattern.isExplicit() && !param.isExplicit()) {
                param = param.getNext();
              }
              if (!param.hasNext()) {
                break;
              }

              if (param.isExplicit() != subPattern.isExplicit()) {
                myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.EXPECTED_EXPLICIT_PATTERN, subPattern));
                continue;
              }

              concretePatterns.put(param, subPattern);
              param = param.getNext();
            }

            if (param.hasNext()) {
              myErrorReporter.report(new NotEnoughPatternsError(DependentLink.Helper.size(param), conPattern));
            }

            patterns.set(k--, constructor.getPattern().toConcrete(conPattern.getData(), conPattern.isExplicit(), concretePatterns));
            continue;
          }
          myVisitor.getEquations().solveEquations();
          LevelSubstitution levelSolution;
          if (myFinal) {
            LevelEquationsSolver levelSolver = myVisitor.getEquations().makeLevelEquationsSolver();
            levelSolution = levelSolver.solveLevels();
            myVisitor.getEquations().finalizeEquations(levelSolution, conPattern);
          } else {
            levelSolution = LevelSubstitution.EMPTY;
          }
          substitution.subst(levelSolution);

          Result conResult = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(link, substitution, levelSolution), paramsSubst, totalSubst, conPattern, false);
          if (conResult == null) {
            return null;
          }
          varSubst.addSubst(conResult.varSubst);
          listSubst(result, exprs, conResult.varSubst);
          if (!conResult.varSubst.isEmpty()) {
            for (int i = 0; i < args.size(); i++) {
              args.set(i, args.get(i).subst(conResult.varSubst));
            }
          }

          Map<DependentLink, ExpressionPattern> patternSubst = new HashMap<>();
          DependentLink link1 = link;
          for (ExpressionPattern patternArg : conResult.patterns) {
            patternSubst.put(link1, patternArg);
            link1 = link1.getNext();
          }
          if (conResult.exprs != null) {
            link1 = link;
            for (Expression expression : conResult.exprs) {
              substitution.add(link1, expression);
              link1 = link1.getNext();
            }
          }

          substitution.subst(conResult.varSubst);
          result.add(constructor.getPattern().subst(substitution, levelSolution, patternSubst));
          if (conResult.exprs == null) {
            exprs = null;
            typecheckAsPatterns(pattern.getAsReferables(), null, null);
          } else {
            args.addAll(conResult.exprs);
            Expression newExpr = FunCallExpression.make(constructor, sortArg.subst(levelSolution), args);
            typecheckAsPatterns(pattern.getAsReferables(), newExpr, expr);
            exprs.add(newExpr);
            paramsSubst.add(parameters, newExpr);
          }

          parameters = parameters.getNext();
          continue;
        }
      }

      // Constructor patterns
      DataCallExpression dataCall = TypeCoerceExpression.unfoldType(expr).cast(DataCallExpression.class);
      if (dataCall == null) {
        if (!expr.isError()) {
          myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type"), expr, pattern));
        }
        return null;
      }
      if (!myMode.allowInterval() && dataCall.getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      // Empty pattern
      if (pattern instanceof Concrete.TuplePattern) {
        List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
        if (conCalls == null) {
          myErrorReporter.report(new ImpossibleEliminationError(dataCall, pattern, paramsSubst, clausesParameters, parameters, myElimParams, myCaseArguments));
          return null;

        }
        if (!conCalls.isEmpty()) {
          myErrorReporter.report(new DataTypeNotEmptyError(dataCall, DataTypeNotEmptyError.getConstructors(conCalls), pattern));
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

      Constructor constructor;
      if (dataCall.getDefinition() == Prelude.FIN) {
        constructor = conPattern.getConstructor() == Prelude.ZERO.getRef() || conPattern.getConstructor() == Prelude.FIN_ZERO.getRef() ? Prelude.FIN_ZERO
          : conPattern.getConstructor() == Prelude.SUC.getRef() || conPattern.getConstructor() == Prelude.FIN_SUC.getRef() ? Prelude.FIN_SUC : null;
      } else {
        constructor = conPattern.getConstructor() instanceof GlobalReferable ? dataCall.getDefinition().getConstructor((GlobalReferable) conPattern.getConstructor()) : null;
      }
      List<ConCallExpression> conCalls = new ArrayList<>(1);
      if (constructor == null || !dataCall.getMatchedConCall(constructor, conCalls) || conCalls.isEmpty()) {
        Referable conRef = conPattern.getConstructor();
        if (constructor != null || conRef instanceof TCDefReferable && ((TCDefReferable) conRef).getKind() == GlobalReferable.Kind.CONSTRUCTOR) {
          myErrorReporter.report(new ExpectedConstructorError((GlobalReferable) conRef, dataCall, parameters, conPattern, myCaseArguments, myLinkList.getFirst(), clausesParameters));
        }
        return null;
      }
      ConCallExpression conCall = conCalls.get(0);
      ExprSubstitution substitution = new ExprSubstitution();
      int i = 0;
      for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), i++) {
        substitution.add(link, conCall.getDataTypeArguments().get(i));
      }
      Result conResult = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(constructor.getParameters(), substitution, conCall.getSortArgument().toLevelSubstitution()), paramsSubst, totalSubst, conPattern, false);
      if (conResult == null) {
        return null;
      }
      varSubst.addSubst(conResult.varSubst);
      listSubst(result, exprs, conResult.varSubst);

      if (!myMode.allowConditions()) {
        if (conCall.getDefinition().getBody() instanceof IntervalElim) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with interval conditions is not allowed here", conPattern));
          return null;
        }
        if (conCall.getDefinition().getBody() instanceof ElimBody && NormalizeVisitor.INSTANCE.doesEvaluate(((ElimBody) conCall.getDefinition().getBody()).getElimTree(), conResult.exprs, true)) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with conditions is allowed only when patterns cannot evaluate", conPattern));
          return null;
        }
      }

      if (!conResult.varSubst.isEmpty()) {
        conCall = (ConCallExpression) new SubstVisitor(conResult.varSubst, LevelSubstitution.EMPTY).visitConCall(conCall, null);
      }
      result.add(new ConstructorExpressionPattern(conCall, conResult.patterns));
      if (conResult.exprs == null) {
        exprs = null;
        typecheckAsPatterns(pattern.getAsReferables(), null, null);
      } else {
        Expression newConCall = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), conResult.exprs);
        typecheckAsPatterns(pattern.getAsReferables(), newConCall, expr);
        exprs.add(newConCall);
        paramsSubst.add(parameters, newConCall);
      }
      parameters = parameters.getNext();
    }

    if (!withElim) {
      while (!parameters.isExplicit()) {
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        myLinkList.append(newParam);
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

    return new Result(result, exprs, varSubst);
  }

  private Concrete.@Nullable Pattern translateNumberPatterns(Concrete.NumberPattern pattern, @NotNull Expression typeExpr) {
    if (myVisitor == null) {
      return DesugarVisitor.desugarNumberPattern(pattern, myErrorReporter);
    }
    var ext = myVisitor.getExtension();
    if (ext == null) {
      return DesugarVisitor.desugarNumberPattern(pattern, myErrorReporter);
    }
    var checker = ext.getLiteralTypechecker();
    if (checker == null) {
      return DesugarVisitor.desugarNumberPattern(pattern, myErrorReporter);
    }

    int numberOfErrors = myVisitor.getNumberOfErrors();
    ConcretePattern result = checker.desugarNumberPattern(pattern, myVisitor, new PatternContextDataImpl(typeExpr, pattern));
    if (result == null && myVisitor.getNumberOfErrors() == numberOfErrors) {
      myErrorReporter.report(new TypecheckingError("Cannot typecheck pattern", pattern));
    }
    if (result != null && !(result instanceof Concrete.Pattern)) {
      throw new IllegalStateException("ConcretePattern must be created with ConcreteFactory");
    }
    if (result instanceof Concrete.NumberPattern) {
      throw new IllegalStateException("desugarNumberPattern should not return ConcreteNumberPattern");
    }
    return (Concrete.Pattern) result;
  }
}
