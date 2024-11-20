package org.arend.typechecking.patternmatching;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.FunctionInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.context.param.UntypedDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.level.LevelSubstitution;
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
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.LevelEquationsSolver;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DesugarVisitor;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class PatternTypechecking {
  private final ErrorReporter myErrorReporter;
  private final Mode myMode;
  private final CheckTypeVisitor myVisitor;
  private Map<Referable, Binding> myContext;
  private final boolean myFinal;
  private final List<Expression> myCaseArguments;
  private final List<DependentLink> myElimParams;
  private final LinkList myLinkList = new LinkList();
  private final List<Pair<ConstructorExpressionPattern,Integer>> myPathPatterns = new ArrayList<>();
  private boolean myTypecheckOtherClauses = true;

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

  public PatternTypechecking(Mode mode, CheckTypeVisitor visitor, boolean isFinal, @Nullable List<Expression> caseArguments, @NotNull List<DependentLink> elimParams) {
    myErrorReporter = visitor.getErrorReporter();
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
    return typecheckClauses(clauses, null, parameters, expectedType, null);
  }

  public List<ExtElimClause> typecheckClauses(List<Concrete.FunctionClause> clauses, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, Expression expectedType, FunctionDefinition definition) {
    this.clausesParameters = parameters;

    List<ExtElimClause> result = new ArrayList<>(clauses.size());
    boolean ok = true;
    for (Concrete.FunctionClause clause : clauses) {
      if (!myTypecheckOtherClauses) {
        return null;
      }
      if (!typecheckClause(clause, abstractParameters, parameters, expectedType, result, definition)) {
        ok = false;
      }
    }
    return ok ? result : null;
  }

  private boolean typecheckClause(Concrete.FunctionClause clause, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, Expression expectedType, List<ExtElimClause> resultClauses, FunctionDefinition definition) {
    assert myVisitor != null;
    try (var ignored = new Utils.RefContextSaver(myVisitor.getContext(), myVisitor.getLocalExpressionPrettifier())) {
      // Typecheck patterns
      myPathPatterns.clear();
      ExprSubstitution substitution = new ExprSubstitution();
      ExprSubstitution totalSubst = new ExprSubstitution();
      Result result = typecheckPatterns(clause.getPatterns(), abstractParameters, parameters, substitution, totalSubst, clause);
      if (result == null) {
        return false;
      }

      ExtElimClause elimClause = new ExtElimClause(result.patterns, null, totalSubst);

      // If we have the absurd pattern, then RHS is ignored
      if (result.hasEmptyPattern()) {
        if (clause.getExpression() != null) {
          myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.BODY_IGNORED, clause.getExpression()));
        }
        resultClauses.add(elimClause);
        return true;
      } else {
        if (clause.getExpression() == null) {
          myErrorReporter.report(new TypecheckingError("Required a body", clause));
          return false;
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
      LocalInstancePool instancePool = globalInstancePool == null ? null : globalInstancePool.getLocalInstancePool();
      if (instancePool != null) {
        globalInstancePool.setInstancePool(instancePool.subst(substitution));
      }

      TypecheckingResult tcResult;
      try {
        List<Binding> intervalBindings;
        Expression exprType = expectedType;
        if (!myPathPatterns.isEmpty()) {
          Body body = new ElimTypechecking(null, null, null, myMode, null, Level.INFINITY, false, null, 0, null).typecheckElim(resultClauses, parameters, myElimParams);
          if (body == null) {
            myErrorReporter.report(new TypecheckingError("Cannot compute body", clause));
            return false;
          }
          if (!(body instanceof ElimBody)) {
            myErrorReporter.report(new TypecheckingError("Incorrect body", clause));
            return false;
          }
          Sort sort = expectedType.getSortOfType();
          if (sort == null) {
            myErrorReporter.report(new TypecheckingError("Cannot infer the sort of the type", clause));
            return false;
          }

          if (definition != null) definition.setBody(body);
          intervalBindings = new ArrayList<>();
          getIntervalBindings(result.patterns, 0, intervalBindings);

          List<TypedSingleDependentLink> lamBindings = new ArrayList<>(intervalBindings.size());
          ExprSubstitution intervalSubst = new ExprSubstitution();
          for (Binding binding : intervalBindings) {
            TypedSingleDependentLink link = new TypedSingleDependentLink(true, binding.getName(), binding.getType());
            lamBindings.add(link);
            intervalSubst.add(binding, new ReferenceExpression(link));
          }

          LevelPair levels = new LevelPair(sort.getPLevel(), sort.getHLevel());
          Sort hSort = new Sort(sort.getPLevel(), Level.INFINITY);
          exprType = expectedType.subst(intervalSubst);
          List<Expression> exprTypes = new ArrayList<>(intervalBindings.size());
          List<Expression> args = ExpressionPattern.toExpressions(result.patterns);
          for (int i = intervalBindings.size() - 1; i >= 0; i--) {
            exprTypes.add(exprType);
            Binding intervalBinding = intervalBindings.get(i);

            intervalSubst.add(intervalBinding, Left());
            Expression leftArg = evalBody(intervalSubst, (ElimBody) body, args);
            if (leftArg == null && definition != null) {
              leftArg = makeFunCall(definition, args, intervalSubst, clause);
            }

            intervalSubst.add(intervalBinding, Right());
            Expression rightArg = evalBody(intervalSubst, (ElimBody) body, args);
            if (rightArg == null && definition != null) {
              rightArg = makeFunCall(definition, args, intervalSubst, clause);
            }

            if (leftArg == null || rightArg == null) {
              myErrorReporter.report(new TypecheckingError("Cannot evaluate conditions", clause));
              return false;
            }

            if (definition != null) {
              leftArg = normalizeRecursiveCalls(leftArg, definition);
              rightArg = normalizeRecursiveCalls(rightArg, definition);
            }

            for (int j = intervalBindings.size() - 1, k = 0; j > i; j--, k++) {
              leftArg = new PathExpression(levels, new LamExpression(hSort, lamBindings.get(j), exprTypes.get(k).subst(lamBindings.get(i), Left())), new LamExpression(hSort, lamBindings.get(j), leftArg));
              rightArg = new PathExpression(levels, new LamExpression(hSort, lamBindings.get(j), exprTypes.get(k).subst(lamBindings.get(i), Right())), new LamExpression(hSort, lamBindings.get(j), rightArg));
            }

            intervalSubst.add(intervalBinding, new ReferenceExpression(lamBindings.get(i)));
            exprType = DataCallExpression.make(Prelude.PATH, levels, Arrays.asList(new LamExpression(hSort, lamBindings.get(i), exprType), leftArg, rightArg));
          }
        } else {
          intervalBindings = null;
        }

        // Typecheck the RHS
        tcResult = myVisitor.checkExpr(clause.getExpression(), exprType);
        if (intervalBindings != null && tcResult != null) {
          ErrorExpression errorExpr = tcResult.expression.cast(ErrorExpression.class);
          if (!(errorExpr != null && errorExpr.isGoal())) {
            Expression resultExpr = tcResult.expression;
            for (Binding binding : intervalBindings) {
              resultExpr = AtExpression.make(resultExpr, new ReferenceExpression(binding), false);
            }
            tcResult = new TypecheckingResult(resultExpr, expectedType);
          }
          if (errorExpr instanceof GoalErrorExpression) {
            ((GoalErrorExpression) errorExpr).goalError.removeConditions();
          }
        }
        if (myFinal) {
          tcResult = myVisitor.finalize(tcResult, clause.getExpression(), false);
        }
      } finally {
        if (definition != null) definition.setBody(null);
      }
      if (instancePool != null) {
        globalInstancePool.setInstancePool(instancePool);
      }
      if (tcResult == null) {
        return false;
      }
      elimClause.setExpression(tcResult.expression);
      resultClauses.add(elimClause);
      return true;
    }
  }

  private Expression makeFunCall(FunctionDefinition definition, List<Expression> args, ExprSubstitution substitution, Concrete.SourceNode sourceNode) {
    List<Expression> newArgs = new ArrayList<>(args.size());
    for (Expression arg : args) {
      newArgs.add(arg.subst(substitution));
    }
    Levels levels = definition.generateInferVars(myVisitor.getEquations(), sourceNode);
    LevelSubstitution levelSubst = levels.makeSubstitution(definition);
    ExprSubstitution paramSubst = new ExprSubstitution();
    DependentLink param = definition.getParameters();
    for (Expression arg : newArgs) {
      Expression paramType = param.getTypeExpr().subst(paramSubst, levelSubst);
      if (!CompareVisitor.compare(myVisitor.getEquations(), CMP.LE, arg.getType(), paramType, Type.OMEGA, sourceNode)) {
        return null;
      }
      paramSubst.add(param, arg);
      param = param.getNext();
    }
    return FunCallExpression.makeFunCall(definition, levels, newArgs);
  }

  private Expression normalizeRecursiveCalls(Expression expr, FunctionDefinition def) {
    return expr.accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY) {
      @Override
      public Expression visitFunCall(FunCallExpression expr, Void params) {
        if (expr.getDefinition() == def) {
          Expression result = NormalizeVisitor.INSTANCE.eval(expr, false);
          if (result != null) {
            if (result instanceof FunCallExpression funCall && funCall.getDefinition() == def) {
              List<Expression> args = new ArrayList<>();
              for (Expression arg : funCall.getDefCallArguments()) {
                args.add(arg.accept(this, null).normalize(NormalizationMode.WHNF));
              }
              return FunCallExpression.make(def, funCall.getLevels(), args);
            }
            return result.accept(this, null);
          }
        }
        return super.visitDefCall(expr, params);
      }
    }, null);
  }

  private Expression evalBody(ExprSubstitution substitution, ElimBody body, List<Expression> args) {
    List<Expression> substArgs = new ArrayList<>(args.size());
    for (Expression arg : args) {
      substArgs.add(arg.subst(substitution));
    }
    return NormalizeVisitor.INSTANCE.eval(body, substArgs, new ExprSubstitution(), LevelSubstitution.EMPTY, null, null, false);
  }

  private int getIntervalBindings(List<? extends ExpressionPattern> patterns, int index, List<Binding> result) {
    for (ExpressionPattern pattern : patterns) {
      index = getIntervalBindings(pattern.getSubPatterns(), index, result);
      if (pattern == myPathPatterns.get(index).proj1) {
        List<? extends ExpressionPattern> subPatterns = pattern.getSubPatterns();
        for (int i = myPathPatterns.get(index).proj2 - 1; i >= 0; i--) {
          result.add(((BindingPattern) subPatterns.get(subPatterns.size() - 1 - i)).getBinding());
        }
        if (++index >= myPathPatterns.size()) {
          break;
        }
      }
    }
    return index;
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
    Result result = doTypechecking(patterns, parameters, new ExprSubstitution(), null, sourceNode, withElim, 0);
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

  private void typecheckAsPattern(Concrete.TypedReferable asPattern, Expression expression, Expression expectedType) {
    if (myVisitor == null) {
      return;
    }

    if (expression == null || asPattern == null) {
      if (asPattern != null) {
        myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.AS_PATTERN_IGNORED, asPattern));
      }
      return;
    }

    expectedType = expectedType.copy();
    Type type = typecheckType(asPattern.type, expectedType);
    if (asPattern.referable != null) {
      addBinding(asPattern.referable, new TypedEvaluatingBinding(asPattern.referable.textRepresentation(), expression, type == null ? expectedType : type.getExpr()));
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
    Result result = doTypechecking(patterns, parameters, paramSubst, totalSubst, sourceNode, !myElimParams.isEmpty(), 0);
    if (result == null) return null;
    if (myFinal) {
      new StripVisitor(myErrorReporter).visitParameters(Pattern.getFirstBinding(result.patterns));
    }
    return result;
  }

  public static class Result {
    private final List<ExpressionPattern> patterns;
    private final List<Expression> exprs;
    private final ExprSubstitution varSubst; // Substitutes e for x if we matched on a path e = x
    private final int addedIntervalVars;

    private Result(List<ExpressionPattern> patterns, List<Expression> exprs, ExprSubstitution varSubst, int addedIntervalVars) {
      this.patterns = patterns;
      this.exprs = exprs;
      this.varSubst = varSubst;
      this.addedIntervalVars = addedIntervalVars;
    }

    public List<ExpressionPattern> getPatterns() {
      return patterns;
    }

    public boolean hasEmptyPattern() {
      return exprs == null;
    }
  }

  private static void listSubst(List<ExpressionPattern> patterns, List<Expression> exprs, ExprSubstitution varSubst) {
    if (varSubst == null || varSubst.isEmpty()) {
      return;
    }
    patterns.replaceAll(expressionPattern -> expressionPattern.subst(varSubst, LevelSubstitution.EMPTY, null));
    if (exprs != null) {
      exprs.replaceAll(expression -> expression.subst(varSubst, LevelSubstitution.EMPTY));
    }
  }

  public static Expression unfoldType(Expression type, List<FunCallExpression> funCalls) {
    type = type.normalize(NormalizationMode.WHNF);
    while (type instanceof FunCallExpression funCall && funCall.getDefinition().getKind() == CoreFunctionDefinition.Kind.TYPE) {
      Expression next = TypeConstructorExpression.match(funCall, null);
      if (next == null) return type;
      funCalls.add(funCall);
      type = ((TypeConstructorExpression) next).getArgumentType().normalize(NormalizationMode.WHNF);
    }
    return type;
  }

  private Result doTypechecking(List<Concrete.Pattern> patterns, DependentLink parameters, ExprSubstitution paramsSubst, ExprSubstitution totalSubst, ConcreteSourceNode sourceNode, boolean withElim, int addIntervalVars) {
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
          } else if (pattern.getAsReferable() != null) {
            myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.AS_PATTERN_IGNORED, pattern.getAsReferable()));
          }
        }

        Referable referable = null;
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        myLinkList.append(newParam);
        if (pattern instanceof Concrete.NamePattern namePattern) {
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
          Result conResult = doTypechecking(patternArgs, newParameters, paramsSubst, totalSubst, pattern, false, 0);
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
            typecheckAsPattern(pattern.getAsReferable(), null, null);
          } else {
            Expression newExpr = newPattern.toExpression(conResult.exprs);
            typecheckAsPattern(pattern.getAsReferable(), newExpr, expr);
            exprs.add(newExpr);
            paramsSubst.add(parameters, newExpr);
          }

          parameters = parameters.getNext();
          continue;
        } else {
          if (!patternArgs.isEmpty()) {
            if (!expr.reportIfError(myErrorReporter, pattern)) {
              myErrorReporter.report(new TypeMismatchError(DocFactory.text("a sigma type or a class"), expr, pattern));
            }
            return null;
          }
          if (!expr.isInstance(DataCallExpression.class)) {
            if (!expr.reportIfError(myErrorReporter, pattern)) {
              myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type, a sigma type, or a class"), expr, pattern));
            }
            return null;
          }
        }
      }

      // Defined constructor patterns
      if (pattern instanceof Concrete.ConstructorPattern conPattern) {
        Definition def = conPattern.getConstructor() instanceof TCDefReferable ? ((TCDefReferable) conPattern.getConstructor()).getTypechecked() : null;
        if (def instanceof DConstructor constructor && def != Prelude.EMPTY_ARRAY && def != Prelude.ARRAY_CONS) {
          if (myVisitor == null || constructor.getPattern() == null) {
            return null;
          }

          Levels levels;
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
              levels = new LevelPair(dataSort.getPLevel(), dataSort.getHLevel().add(1));
            } else {
              levels = new LevelPair(actualSort.getPLevel(), actualSort.getHLevel());
            }

            Expression expr1 = dataCall.getDefCallArguments().get(2).normalize(NormalizationMode.WHNF);
            Expression expr2 = dataCall.getDefCallArguments().get(1).normalize(NormalizationMode.WHNF);
            ReferenceExpression refExpr1 = expr1.cast(ReferenceExpression.class);
            ReferenceExpression refExpr2 = expr2.cast(ReferenceExpression.class);
            if (refExpr1 == null && refExpr2 == null) {
              myErrorReporter.report(new IdpPatternError(myVisitor == null ? null : myVisitor.getExpressionPrettifier(), IdpPatternError.noVariable(), dataCall, conPattern));
              return null;
            }

            int num = 0;
            boolean both = false;
            for (DependentLink paramLink = myLinkList.getFirst(); paramLink.hasNext(); paramLink = paramLink.getNext()) {
              if (refExpr1 != null && refExpr1.getBinding() == paramLink) {
                if (num == 2) {
                  num = 1;
                  both = true;
                  break;
                } else {
                  num = 1;
                }
              }
              if (refExpr2 != null && refExpr2.getBinding() == paramLink) {
                if (num == 1) {
                  num = 2;
                  both = true;
                  break;
                } else {
                  num = 2;
                }
              }
            }
            if (num == 0) {
              myErrorReporter.report(new IdpPatternError(myVisitor == null ? null : myVisitor.getExpressionPrettifier(), IdpPatternError.noParameter(), dataCall, conPattern));
              return null;
            }

            Expression normType = type.normalize(NormalizationMode.WHNF);
            if (!(normType instanceof DataCallExpression)) {
              if (!CompareVisitor.compare(myVisitor.getEquations(), CMP.EQ, normType, (num == 1 ? refExpr1 : refExpr2).getType(), Type.OMEGA, conPattern)) {
                boolean ok = false;
                if (both) {
                  num = 3 - num;
                  ok = CompareVisitor.compare(myVisitor.getEquations(), CMP.EQ, normType, (num == 1 ? refExpr1 : refExpr2).getType(), Type.OMEGA, conPattern);
                }
                if (!ok) {
                  myErrorReporter.report(new IdpPatternError(myVisitor == null ? null : myVisitor.getExpressionPrettifier(), IdpPatternError.typeMismatch(), dataCall, conPattern));
                  return null;
                }
              }
            }

            Binding substVar = num == 1 ? refExpr1.getBinding() : refExpr2.getBinding();
            Expression otherExpr = ElimBindingVisitor.elimBinding(num == 1 ? expr2 : expr1, substVar);
            if (otherExpr == null) {
              myErrorReporter.report(new IdpPatternError(myVisitor == null ? null : myVisitor.getExpressionPrettifier(), IdpPatternError.variable(substVar.getName()), dataCall, conPattern));
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
                myErrorReporter.report(new IdpPatternError(myVisitor == null ? null : myVisitor.getExpressionPrettifier(), IdpPatternError.subst(substVar.getName(), paramLink.getName(), banVar.getName()), null, conPattern));
                return null;
              }
              assert paramLink != null;
              paramLink.setType(paramLink.getType().subst(new SubstVisitor(varSubst, LevelSubstitution.EMPTY)));
            }
            listSubst(result, exprs, varSubst);
          } else {
            levels = def.generateInferVars(myVisitor.getEquations(), conPattern);
            LevelSubstitution levelSubst = levels.makeSubstitution(def);

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

            patterns.set(k--, constructor.getPattern().toConcrete(conPattern.getData(), conPattern.isExplicit(), conPattern.getConstructorData(), concretePatterns));
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

          Result conResult = doTypechecking(conPattern.getPatterns(), DependentLink.Helper.subst(link, substitution, levelSolution), paramsSubst, totalSubst, conPattern, false, 0);
          if (conResult == null) {
            return null;
          }
          varSubst.addSubst(conResult.varSubst);
          listSubst(result, exprs, conResult.varSubst);
          if (!conResult.varSubst.isEmpty()) {
            args.replaceAll(expression -> expression.subst(conResult.varSubst));
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
            typecheckAsPattern(pattern.getAsReferable(), null, null);
          } else {
            args.addAll(conResult.exprs);
            Expression newExpr = FunCallExpression.make(constructor, levels.subst(levelSolution), args);
            typecheckAsPattern(pattern.getAsReferable(), newExpr, expr);
            exprs.add(newExpr);
            paramsSubst.add(parameters, newExpr);
          }

          parameters = parameters.getNext();
          continue;
        }
      }

      // Constructor patterns
      List<FunCallExpression> typeConstructorFunCalls = new ArrayList<>();
      Expression unfoldedExpr = unfoldType(expr, typeConstructorFunCalls).getUnderlyingExpression();
      DataCallExpression dataCall = unfoldedExpr instanceof DataCallExpression ? (DataCallExpression) unfoldedExpr : null;
      ClassCallExpression classCall = unfoldedExpr instanceof ClassCallExpression ? (ClassCallExpression) unfoldedExpr : null;
      if (!(dataCall != null || classCall != null && classCall.getDefinition() == Prelude.DEP_ARRAY)) {
        if (!expr.reportIfError(myErrorReporter, pattern)) {
          myErrorReporter.report(new TypeMismatchError(DocFactory.text("a data type"), expr, pattern));
        }
        return null;
      }
      if (!myMode.allowInterval() && dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", pattern));
        return null;
      }

      // Empty pattern
      if (pattern instanceof Concrete.TuplePattern) {
        List<Definition> constructors;
        if (dataCall != null) {
          List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
          if (conCalls == null) {
            myErrorReporter.report(new ImpossibleEliminationError(dataCall, pattern, paramsSubst, clausesParameters, parameters, myElimParams, myCaseArguments));
            return null;
          }
          constructors = DataTypeNotEmptyError.getConstructors(conCalls);
        } else {
          Expression length = classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
          if (length != null) length = length.normalize(NormalizationMode.WHNF);
          if (length instanceof IntegerExpression && ((IntegerExpression) length).isZero()) {
            constructors = Collections.singletonList(Prelude.EMPTY_ARRAY);
          } else if (length instanceof IntegerExpression || length instanceof ConCallExpression && ((ConCallExpression) length).getDefinition() == Prelude.SUC) {
            constructors = Collections.singletonList(Prelude.ARRAY_CONS);
          } else {
            constructors = Arrays.asList(Prelude.EMPTY_ARRAY, Prelude.ARRAY_CONS);
          }
        }
        if (!constructors.isEmpty()) {
          myErrorReporter.report(new DataTypeNotEmptyError(dataCall, constructors, pattern));
          return null;
        }
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        myLinkList.append(newParam);
        result.add(new EmptyPattern(newParam));
        exprs = null;
        typecheckAsPattern(pattern.getAsReferable(), null, null);
        parameters = parameters.getNext();
        continue;
      }

      if (!(pattern instanceof Concrete.ConstructorPattern conPattern)) {
        throw new IllegalStateException();
      }

      if (dataCall != null && dataCall.getDefinition() == Prelude.INT && (conPattern.getConstructor() == Prelude.ZERO.getReferable() || conPattern.getConstructor() == Prelude.SUC.getReferable())) {
        boolean isExplicit = conPattern.isExplicit();
        conPattern.setExplicit(true);
        conPattern = new Concrete.ConstructorPattern(conPattern.getData(), isExplicit, conPattern.getConstructorData(), Prelude.POS.getReferable(), Collections.singletonList(conPattern), conPattern.getAsReferable());
      }

      Definition constructor;
      if (dataCall != null && dataCall.getDefinition() == Prelude.FIN) {
        constructor = conPattern.getConstructor() == Prelude.ZERO.getRef() || conPattern.getConstructor() == Prelude.FIN_ZERO.getRef() ? Prelude.FIN_ZERO
          : conPattern.getConstructor() == Prelude.SUC.getRef() || conPattern.getConstructor() == Prelude.FIN_SUC.getRef() ? Prelude.FIN_SUC : null;
      } else {
        constructor = conPattern.getConstructor() instanceof TCDefReferable ? ((TCDefReferable) conPattern.getConstructor()).getTypechecked() : null;
      }
      List<ConCallExpression> conCalls = new ArrayList<>(1);
      if (constructor == null || dataCall != null && (!(constructor instanceof Constructor) || !dataCall.getMatchedConCall((Constructor) constructor, conCalls) || conCalls.isEmpty()) || classCall != null && constructor != Prelude.EMPTY_ARRAY && constructor != Prelude.ARRAY_CONS) {
        Referable conRef = conPattern.getConstructor();
        if (constructor != null || conRef instanceof TCDefReferable && ((TCDefReferable) conRef).getKind() == GlobalReferable.Kind.CONSTRUCTOR) {
          myErrorReporter.report(new ExpectedConstructorError((GlobalReferable) conRef, dataCall != null ? dataCall : classCall, parameters, conPattern, myCaseArguments, myLinkList.getFirst(), clausesParameters));
        }
        return null;
      }
      ConCallExpression conCall = dataCall != null ? conCalls.get(0) : null;
      DependentLink newParameters;
      if (dataCall != null) {
        newParameters = DependentLink.Helper.subst(constructor.getParameters(), new ExprSubstitution().add(((Constructor) constructor).getDataTypeParameters(), conCall.getDataTypeArguments()), dataCall.getLevelSubstitution());
      } else {
        newParameters = ((DConstructor) constructor).getArrayParameters(classCall);
      }

      Expression length = classCall == null ? null : classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
      if (length != null) length = length.normalize(NormalizationMode.WHNF);
      if (length != null && !(length instanceof IntegerExpression || length instanceof ConCallExpression && ((ConCallExpression) length).getDefinition() == Prelude.SUC)) {
        myErrorReporter.report(new ImpossibleEliminationError(classCall, pattern));
        myTypecheckOtherClauses = false;
        return null;
      }
      Expression length1 = length == null || constructor == Prelude.EMPTY_ARRAY ? null : length.normalize(NormalizationMode.WHNF).pred();

      if (length == null && Prelude.ARRAY_CONS != null && conPattern.getConstructor() == Prelude.ARRAY_CONS.getReferable() && conPattern.getPatterns().size() >= 2 && conPattern.getPatterns().get(0).isExplicit() && conPattern.getPatterns().get(1).isExplicit()) {
        Concrete.Pattern subPattern = pattern.getPatterns().get(1);
        int n = 0;
        boolean isNil = false;
        while (subPattern instanceof Concrete.ConstructorPattern subConPattern) {
          if (subConPattern.getConstructor() == Prelude.EMPTY_ARRAY.getReferable()) {
            isNil = true;
            break;
          } else if (subConPattern.getConstructor() == Prelude.ARRAY_CONS.getReferable()) {
            n++;
            if (subConPattern.getPatterns().size() >= 2 && subConPattern.getPatterns().get(0).isExplicit() && subConPattern.getPatterns().get(1).isExplicit()) {
              subPattern = subConPattern.getPatterns().get(1);
            } else {
              break;
            }
          } else {
            break;
          }
        }
        if (n > 0 || isNil) {
          Object data = patterns.get(0).getData();
          Concrete.Pattern newPattern;
          if (isNil) {
            newPattern = new Concrete.NumberPattern(data, n, null);
          } else {
            newPattern = new Concrete.NamePattern(data, true, new LocalReferable("n"), null);
            for (int i = 0; i < n; i++) {
              newPattern = new Concrete.ConstructorPattern(data, data, Prelude.SUC.getReferable(), Collections.singletonList(newPattern), null);
            }
          }
          newPattern.setExplicit(false);
          conPattern.getPatterns().add(0, newPattern);
        }
      }

      Body body = conCall != null ? conCall.getDefinition().getBody() : null;
      Result conResult = doTypechecking(conPattern.getPatterns(), newParameters, paramsSubst, totalSubst, conPattern, false, body instanceof IntervalElim ? ((IntervalElim) body).getCases().size() : 0);
      if (conResult == null) {
        return null;
      }
      varSubst.addSubst(conResult.varSubst);
      listSubst(result, exprs, conResult.varSubst);

      if (!myMode.allowConditions() && conCall != null) {
        if (conCall.getDefinition().getBody() instanceof IntervalElim) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with interval conditions is not allowed here", conPattern));
          return null;
        }
        if (conCall.getDefinition().getBody() instanceof ElimBody && NormalizeVisitor.INSTANCE.doesEvaluate(((ElimBody) conCall.getDefinition().getBody()).getElimTree(), conResult.exprs, true)) {
          myErrorReporter.report(new TypecheckingError("Pattern matching on a constructor with conditions is allowed only when patterns cannot evaluate", conPattern));
          return null;
        }
      }

      ConstructorExpressionPattern resultPattern;
      if (dataCall != null) {
        if (!conResult.varSubst.isEmpty()) {
          conCall = (ConCallExpression) new SubstVisitor(conResult.varSubst, LevelSubstitution.EMPTY).visitConCall(conCall, null);
        }
        resultPattern = new ConstructorExpressionPattern(conCall, conResult.patterns);
      } else {
        Expression elementsType = classCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
        Expression arrayLength = length;
        if (!conResult.varSubst.isEmpty()) {
          SubstVisitor visitor = new SubstVisitor(conResult.varSubst, LevelSubstitution.EMPTY);
          if (elementsType != null) elementsType = elementsType.accept(visitor, null);
          if (arrayLength != null) arrayLength = arrayLength.accept(visitor, null);
        }
        resultPattern = new ConstructorExpressionPattern(new FunCallExpression((DConstructor) constructor, classCall.getLevels(), length1, elementsType), classCall.getThisBinding(), arrayLength, conResult.patterns);
      }
      result.add(resultPattern);
      if (conResult.addedIntervalVars > 0) {
        myPathPatterns.add(new Pair<>(resultPattern, conResult.addedIntervalVars));
        for (DependentLink param = parameters.getNext(); param.hasNext(); param = param.getNext()) {
          param = param.getNextTyped(null);
          if (param.getTypeExpr().findBinding(parameters)) {
            myErrorReporter.report(new TypecheckingError("Partially applied constructor is not allowed because parameter '" + param.getName() + "' depends on '" + parameters.getName() + "'", conPattern));
            return null;
          }
        }
      }

      if (conResult.exprs == null) {
        exprs = null;
        typecheckAsPattern(pattern.getAsReferable(), null, null);
      } else {
        Expression newConCall;
        if (dataCall != null) {
          newConCall = ConCallExpression.make(conCall.getDefinition(), conCall.getLevels(), conCall.getDataTypeArguments(), conResult.exprs);
        } else {
          List<Expression> funCallArgs;
          Expression elementsType = classCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
          if (elementsType != null) {
            elementsType = elementsType.subst(classCall.getThisBinding(), new NewExpression(null, new ClassCallExpression(Prelude.DEP_ARRAY, classCall.getLevels(), Collections.singletonMap(Prelude.ARRAY_LENGTH, constructor == Prelude.EMPTY_ARRAY ? Zero() : length != null ? length : Suc(conResult.exprs.get(0))), Sort.STD.succ(), UniverseKind.NO_UNIVERSES)));
          }
          if (elementsType != null || length1 != null && constructor == Prelude.ARRAY_CONS) {
            funCallArgs = new ArrayList<>();
            if (length1 != null && constructor == Prelude.ARRAY_CONS) {
              funCallArgs.add(length1);
              if (elementsType != null) funCallArgs.add(elementsType);
              funCallArgs.addAll(conResult.exprs);
            } else {
              if (!conResult.exprs.isEmpty()) funCallArgs.add(conResult.exprs.get(0));
              funCallArgs.add(elementsType);
              if (!conResult.exprs.isEmpty()) funCallArgs.addAll(conResult.exprs.subList(1, conResult.exprs.size()));
            }
          } else {
            funCallArgs = conResult.exprs;
          }
          newConCall = FunCallExpression.make((FunctionDefinition) constructor, classCall.getLevels(), funCallArgs);
        }
        for (int i = typeConstructorFunCalls.size() - 1; i >= 0; i--) {
          newConCall = TypeConstructorExpression.match(typeConstructorFunCalls.get(i), newConCall);
        }
        typecheckAsPattern(pattern.getAsReferable(), newConCall, expr.subst(varSubst));
        exprs.add(newConCall);
        paramsSubst.add(parameters, newConCall);
      }
      parameters = parameters.getNext();
    }

    if (!withElim) {
      while (!parameters.isExplicit()) {
        DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
        addBinding(null, newParam);
        myLinkList.append(newParam);
        result.add(new BindingPattern(newParam));
        if (exprs != null) {
          exprs.add(new ReferenceExpression(newParam));
        }
        parameters = parameters.getNext();
      }
    }

    int addedIntervalVars = 0;
    if (parameters.hasNext()) {
      boolean ok = addIntervalVars > 0;
      int size = DependentLink.Helper.size(parameters);
      if (ok) {
        for (; parameters.hasNext() && addedIntervalVars < addIntervalVars; parameters = parameters.getNext()) {
          Expression paramType = parameters.getTypeExpr().normalize(NormalizationMode.WHNF);
          if (paramType instanceof DataCallExpression && ((DataCallExpression) paramType).getDefinition() == Prelude.INTERVAL) {
            DependentLink newParam = parameters.subst(new SubstVisitor(paramsSubst, LevelSubstitution.EMPTY), 1, false);
            myLinkList.append(newParam);
            result.add(new BindingPattern(newParam));
            if (exprs != null) {
              exprs.add(new ReferenceExpression(newParam));
            }
            addedIntervalVars++;
          } else {
            ok = false;
          }
        }
      }
      if (!ok || parameters.hasNext()) {
        myErrorReporter.report(new NotEnoughPatternsError(size > addedIntervalVars ? size - addedIntervalVars : size, sourceNode));
        return null;
      }
    }

    return new Result(result, exprs, varSubst, addedIntervalVars);
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
