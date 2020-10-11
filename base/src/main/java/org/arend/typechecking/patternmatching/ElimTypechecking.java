package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.constructor.TupleConstructor;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.GetTypeVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.MissingClausesError;
import org.arend.ext.error.RedundantClauseError;
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.Referable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DumbTypechecker;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static org.arend.core.expr.ExpressionFactory.*;

public class ElimTypechecking {
  private final ErrorReporter myErrorReporter;
  private final Equations myEquations;
  private Set<Integer> myUnusedClauses;
  private final PatternTypechecking.Mode myMode;
  private final Expression myExpectedType;
  private final Integer myLevel;
  private final Level myActualLevel;
  private final int myActualLevelSub;
  private boolean myOK;
  private Stack<Util.ClauseElem> myContext;
  private List<Pair<List<Util.ClauseElem>, Boolean>> myMissingClauses;
  private final List<? extends Concrete.FunctionClause> myClauses;
  private final Concrete.SourceNode mySourceNode;
  private boolean myAllowInterval = true;

  private static Integer getMinPlus1(Integer level1, Level l2, int sub) {
    Integer level2 = !l2.isInfinity() && l2.isClosed() ? l2.getConstant() : null;
    Integer result = level1 != null && level2 != null ? Integer.valueOf(Math.min(level1, level2 - sub)) : level2 != null ? Integer.valueOf(level2 - sub) : level1;
    return result == null ? null : result + 1;
  }

  public ElimTypechecking(ErrorReporter errorReporter, Equations equations, Expression expectedType, PatternTypechecking.Mode mode, @Nullable Integer level, @NotNull Level actualLevel, boolean isSFunc, List<? extends Concrete.FunctionClause> clauses, Concrete.SourceNode sourceNode) {
    myErrorReporter = errorReporter;
    myEquations = equations;
    myExpectedType = expectedType;
    myMode = mode;
    myClauses = clauses;
    mySourceNode = sourceNode;

    int actualLevelSub = 0;
    if (!actualLevel.isProp()) {
      Expression pathType = expectedType.getPiParameters(null, false);
      for (DataCallExpression dataCall = pathType.cast(DataCallExpression.class); dataCall != null; dataCall = pathType.cast(DataCallExpression.class)) {
        if (dataCall.getDefinition() == Prelude.PATH) {
          actualLevelSub++;
          pathType = dataCall.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF);
          LamExpression lam = pathType.cast(LamExpression.class);
          if (lam == null) {
            pathType = AppExpression.make(pathType, new ReferenceExpression(new TypedBinding("i", ExpressionFactory.Interval())), true);
            break;
          }
          pathType = lam.getBody().normalize(NormalizationMode.WHNF);
        } else {
          break;
        }
      }

      Sort pathSort = pathType.getSortOfType();
      if (pathSort != null && !pathSort.getHLevel().isInfinity()) {
        actualLevel = pathSort.getHLevel();
        if (!actualLevel.isInfinity() && actualLevel.isClosed() && actualLevel.getConstant() - actualLevelSub < -1) {
          actualLevelSub = actualLevel.getConstant() + 1;
        }
      } else {
        actualLevelSub = 0;
      }
    }

    myLevel = getMinPlus1(level, actualLevel, actualLevelSub);
    myActualLevel = isSFunc ? null : actualLevel;
    myActualLevelSub = isSFunc ? 0 : actualLevelSub;
  }

  public ElimTypechecking(ErrorReporter errorReporter, Equations equations, Expression expectedType, PatternTypechecking.Mode mode, List<? extends Concrete.FunctionClause> clauses, Concrete.SourceNode sourceNode) {
    myErrorReporter = errorReporter;
    myEquations = equations;
    myExpectedType = expectedType;
    myMode = mode;
    myLevel = null;
    myActualLevel = Level.INFINITY;
    myActualLevelSub = 0;
    myClauses = clauses;
    mySourceNode = sourceNode;
  }

  public static List<DependentLink> getEliminatedParameters(List<? extends Concrete.ReferenceExpression> expressions, List<? extends Concrete.Clause> clauses, DependentLink parameters, CheckTypeVisitor visitor) {
    return getEliminatedParameters(expressions, clauses, parameters, visitor.getErrorReporter(), visitor.getContext());
  }

  public static List<DependentLink> getEliminatedParameters(List<? extends Concrete.ReferenceExpression> expressions, List<? extends Concrete.Clause> clauses, DependentLink parameters, ErrorReporter errorReporter, Map<Referable, Binding> context) {
    List<DependentLink> elimParams = Collections.emptyList();
    if (!expressions.isEmpty()) {
      int expectedNumberOfPatterns = expressions.size();
      DumbTypechecker.findImplicitPatterns(clauses, errorReporter);
      for (Concrete.Clause clause : clauses) {
        if (clause.getPatterns() != null && clause.getPatterns().size() != expectedNumberOfPatterns) {
          if (clause.getPatterns().size() > expectedNumberOfPatterns) {
            errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, clause.getPatterns().get(expectedNumberOfPatterns)));
          } else {
            errorReporter.report(new NotEnoughPatternsError(expectedNumberOfPatterns - clause.getPatterns().size(), clause));
          }
          return null;
        }
      }

      DependentLink link = parameters;
      elimParams = new ArrayList<>(expressions.size());
      for (Concrete.ReferenceExpression expr : expressions) {
        DependentLink elimParam = (DependentLink) context.get(expr.getReferent());
        if (!elimParams.isEmpty() && elimParam == link) {
          errorReporter.report(new TypecheckingError("Duplicated eliminated parameter", expr));
          return null;
        }
        while (elimParam != link) {
          if (!link.hasNext()) {
            link = parameters;
            while (link.hasNext() && link != elimParam) {
              link = link.getNext();
            }
            errorReporter.report(new TypecheckingError(link == elimParam ? "Variable elimination must be in the order of variable introduction" : "Only parameters can be eliminated", expr));
            return null;
          }
          link = link.getNext();
        }
        elimParams.add(elimParam);
      }
    }
    return elimParams;
  }

  private static class ExtElimClause extends ElimClause<ExpressionPattern> {
    final int index;
    final List<Integer> argIndices;
    final int numberOfFakeVars;
    final ExprSubstitution substitution; // substitutes pattern variables which are replaced with a constructor

    public ExtElimClause(List<ExpressionPattern> patterns, Expression expression, int index, List<Integer> argIndices, int numberOfFakeVars, ExprSubstitution substitution) {
      super(patterns, expression);
      this.index = index;
      this.argIndices = argIndices;
      this.numberOfFakeVars = numberOfFakeVars;
      this.substitution = substitution;
    }

    public ExtElimClause(List<ExpressionPattern> patterns, Expression expression, int index) {
      this(patterns, expression, index, new ArrayList<>(), 0, new ExprSubstitution());
    }
  }

  private static List<ElimClause<Pattern>> removeExpressionsFromPatterns(List<? extends ElimClause<ExpressionPattern>> clauses) {
    List<ElimClause<Pattern>> result = new ArrayList<>();
    for (ElimClause<? extends ExpressionPattern> clause : clauses) {
      result.add(new ElimClause<>(ExpressionPattern.removeExpressions(clause.getPatterns()), clause.getExpression()));
    }
    return result;
  }

  public ElimBody typecheckElim(List<? extends ElimClause<ExpressionPattern>> clauses, DependentLink parameters) {
    myAllowInterval = false;
    return (ElimBody) typecheckElim(clauses, parameters, Collections.emptyList());
  }

  public Body typecheckElim(List<? extends ElimClause<ExpressionPattern>> clauses, DependentLink parameters, List<DependentLink> elimParams) {
    myOK = true;
    myUnusedClauses = new LinkedHashSet<>();
    for (int i = 0; i < clauses.size(); i++) {
      myUnusedClauses.add(i);
    }

    List<ElimClause<ExpressionPattern>> intervalClauses;
    List<ExtElimClause> nonIntervalClauses = new ArrayList<>();
    if (myAllowInterval && myMode.allowInterval()) {
      intervalClauses = new ArrayList<>();
      Concrete.SourceNode errorClause = null;
      for (int i = 0; i < clauses.size(); i++) {
        ElimClause<ExpressionPattern> clause = clauses.get(i);
        boolean hasNonIntervals = false;
        int intervals = 0;
        for (Pattern pattern : clause.getPatterns()) {
          if (pattern instanceof BindingPattern) {
            continue;
          }
          if (pattern instanceof ConstructorPattern) {
            Definition constructor = pattern.getDefinition();
            if (constructor == Prelude.LEFT || constructor == Prelude.RIGHT) {
              intervals++;
              continue;
            }
          }
          hasNonIntervals = true;
          break;
        }
        if (hasNonIntervals || intervals == 0) {
          nonIntervalClauses.add(new ExtElimClause(clause.getPatterns(), clause.getExpression(), i));
          if (!intervalClauses.isEmpty() && errorClause == null) {
            errorClause = getClause(i);
          }
        } else {
          if (intervals > 1) {
            myErrorReporter.report(new TypecheckingError("Only a single interval pattern per row is allowed", getClause(i)));
            myUnusedClauses.remove(i);
          } else {
            intervalClauses.add(clause);
          }
          clauses.remove(i--);
        }
      }
      if (errorClause != null) {
        myErrorReporter.report(new TypecheckingError("Non-interval clauses must be placed before the interval ones", errorClause));
      }
    } else {
      intervalClauses = Collections.emptyList();
      for (int i = 0; i < clauses.size(); i++) {
        nonIntervalClauses.add(new ExtElimClause(clauses.get(i).getPatterns(), clauses.get(i).getExpression(), i));
      }
    }

    List<IntervalElim.CasePair> cases = intervalClauses.isEmpty() ? null : clausesToIntervalElim(intervalClauses, nonIntervalClauses.size(), parameters);
    if (cases != null) {
      int i = 0;
      for (; i < cases.size(); i++) {
        if (cases.get(i).proj1 != null || cases.get(i).proj2 != null) {
          break;
        }
      }
      cases = cases.subList(i, cases.size());

      for (int k = 0; k < nonIntervalClauses.size(); k++) {
        for (int j = i; j < nonIntervalClauses.get(k).getPatterns().size(); j++) {
          if (!(nonIntervalClauses.get(k).getPatterns().get(j) instanceof BindingPattern)) {
            myErrorReporter.report(new TypecheckingError("A pattern matching on a data type is allowed only before the pattern matching on the interval", getClause(k)));
            myOK = false;
          }
        }
      }
    }

    ElimTree elimTree;
    if (nonIntervalClauses.isEmpty()) {
      DependentLink emptyLink = null;
      if (elimParams.isEmpty()) {
        for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
          link = link.getNextTyped(null);
          List<ConCallExpression> conCalls = getMatchedConstructors(link.getTypeExpr());
          if (conCalls != null && conCalls.isEmpty()) {
            emptyLink = link;
            break;
          }
        }
      } else {
        for (DependentLink link : elimParams) {
          List<ConCallExpression> conCalls = getMatchedConstructors(link.getTypeExpr());
          if (conCalls != null && conCalls.isEmpty()) {
            emptyLink = link;
            break;
          }
        }
      }

      if (emptyLink == null && myMode.checkCoverage()) {
        if (!reportMissingClauses(null, parameters, elimParams)) {
          reportNoClauses(parameters, elimParams);
        }
      }

      if (emptyLink != null) {
        int index = 0;
        for (DependentLink link = parameters; link != emptyLink; link = link.getNext()) {
          index++;
        }
        elimTree = new BranchElimTree(index, false);
      } else {
        elimTree = null;
      }
    } else {
      myContext = new Stack<>();
      elimTree = clausesToElimTree(nonIntervalClauses, 0, 0);

      reportMissingClauses(elimTree, parameters, elimParams);

      if (myOK) {
        for (Integer clauseIndex : myUnusedClauses) {
          myErrorReporter.report(new RedundantClauseError(getClause(clauseIndex)));
        }
      }
    }

    ElimBody elimBody = elimTree == null ? null : new ElimBody(removeExpressionsFromPatterns(clauses), elimTree);
    return cases == null ? elimBody : new IntervalElim(DependentLink.Helper.size(parameters), cases, elimBody);
  }

  private Concrete.SourceNode getClause(int index) {
    return myClauses == null ? mySourceNode : myClauses.get(index);
  }

  private static List<ConCallExpression> getMatchedConstructors(Expression expr) {
    DataCallExpression dataCall = expr.normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
    return dataCall == null ? null : dataCall.getMatchedConstructors();
  }

  private static List<List<ExpressionPattern>> generateMissingClauses(List<DependentLink> elimParams, int i, ExprSubstitution substitution, Map<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> paramSpec, Map<DependentLink, List<ConCallExpression>> paramSpec2) {
    if (i == elimParams.size()) {
      List<List<ExpressionPattern>> result = new ArrayList<>();
      result.add(new ArrayList<>());
      return result;
    }

    DependentLink link = elimParams.get(i);
    var spec = paramSpec.get(link);
    if (spec != null) {
      List<List<ExpressionPattern>> totalResult = new ArrayList<>();
      for (Pair<ExpressionPattern, Map<DependentLink, Constructor>> pair : spec) {
        substitution.add(link, pair.proj1.toExpression());
        List<List<ExpressionPattern>> result = generateMissingClauses(elimParams, i + 1, substitution, paramSpec, paramSpec2);
        for (List<ExpressionPattern> row : result) {
          boolean ok = true;
          for (int j = 0; j < row.size(); j++) {
            Constructor constructor = pair.proj2.get(elimParams.get(elimParams.size() - 1 - j));
            if (!(constructor == null || row.get(j).getDefinition() == constructor)) {
              ok = false;
              break;
            }
          }
          if (ok) {
            List<ExpressionPattern> newRow = new ArrayList<>(row);
            newRow.add(pair.proj1);
            totalResult.add(newRow);
          }
        }
      }
      return totalResult;
    }

    List<ConCallExpression> conCalls = paramSpec2.get(link);
    if (conCalls == null) {
      conCalls = getMatchedConstructors(link.getTypeExpr().subst(substitution));
    }

    if (conCalls != null) {
      List<List<ExpressionPattern>> totalResult = new ArrayList<>();
      if (conCalls.isEmpty()) {
        List<ExpressionPattern> patterns = new ArrayList<>();
        for (int j = elimParams.size() - 1; j > i; j--) {
          patterns.add(new BindingPattern(elimParams.get(j)));
        }
        patterns.add(EmptyPattern.INSTANCE);
        totalResult.add(patterns);
      } else {
        boolean firstHasEmpty = false;
        for (ConCallExpression conCall : conCalls) {
          List<Expression> arguments = new ArrayList<>();
          List<ExpressionPattern> subPatterns = new ArrayList<>();
          for (DependentLink link1 = conCall.getDefinition().getParameters(); link1.hasNext(); link1 = link1.getNext()) {
            arguments.add(new ReferenceExpression(link1));
            subPatterns.add(new BindingPattern(link1));
          }
          substitution.add(link, ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments));
          List<List<ExpressionPattern>> result = generateMissingClauses(elimParams, i + 1, substitution, paramSpec, paramSpec2);

          boolean hasEmpty = false;
          if (result.size() == 1) {
            boolean onlyVars = true;
            for (ExpressionPattern pattern : result.get(0)) {
              if (!(pattern instanceof BindingPattern || pattern instanceof EmptyPattern)) {
                onlyVars = false;
                break;
              }
              if (pattern instanceof EmptyPattern) {
                hasEmpty = true;
              }
            }
            if (hasEmpty && !onlyVars) {
              hasEmpty = false;
            }
          }
          if (hasEmpty) {
            if (!totalResult.isEmpty()) {
              continue;
            }
            firstHasEmpty = true;
          }

          for (List<ExpressionPattern> patterns : result) {
            patterns.add(new ConstructorExpressionPattern(conCall, subPatterns));
          }
          totalResult.addAll(result);
        }
        substitution.remove(link);

        if (firstHasEmpty && totalResult.size() > 1) {
          totalResult.remove(0);
        }
      }
      return totalResult;
    } else {
      List<List<ExpressionPattern>> result = generateMissingClauses(elimParams, i + 1, substitution, paramSpec, paramSpec2);
      for (List<ExpressionPattern> patterns : result) {
        patterns.add(new BindingPattern(link));
      }
      return result;
    }
  }

  private static int numberOfIntervals(List<? extends ExpressionPattern> patterns) {
    int result = 0;
    for (ExpressionPattern pattern : patterns) {
      if (pattern.getDefinition() instanceof Constructor) {
        Body body = ((Constructor) pattern.getDefinition()).getBody();
        if (body instanceof IntervalElim) {
          result += ((IntervalElim) body).getNumberOfTotalElim();
        }
      }
      result += numberOfIntervals(pattern.getSubPatterns());
    }
    return result;
  }

  private Map<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> computeParamSpec(DependentLink param, DataCallExpression dataCall, List<DependentLink> elimParams, Map<DependentLink, List<ConCallExpression>> paramSpec2) {
    Map<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> paramSpec = new HashMap<>();

    for (Constructor constructor : dataCall.getDefinition().getConstructors()) {
      Map<Binding, ExpressionPattern> result = new HashMap<>();
      ConCallExpression conCall = ExpressionMatcher.computeMatchingPatterns(dataCall, constructor, null, result);
      if (result.isEmpty()) {
        continue;
      }
      if (conCall != null) {
        paramSpec2.computeIfAbsent(param, k -> new ArrayList<>()).add(conCall);

        Map<DependentLink, List<ExpressionPattern>> map = new HashMap<>();
        for (Map.Entry<Binding, ExpressionPattern> entry : result.entrySet()) {
          if (entry.getKey() instanceof DependentLink) {
            DependentLink key = (DependentLink) entry.getKey();
            if (!elimParams.isEmpty() && !elimParams.contains(key)) {
              myErrorReporter.report(new ImpossibleEliminationError(dataCall, mySourceNode, null));
              return null;
            }
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(entry.getValue());
          }
        }

        for (Map.Entry<DependentLink, List<ExpressionPattern>> entry : map.entrySet()) {
          var list = paramSpec.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
          Map<DependentLink, Constructor> single = Collections.singletonMap(param, constructor);
          for (ExpressionPattern pattern : entry.getValue()) {
            list.add(new Pair<>(pattern, new HashMap<>(single)));
          }
        }
      }
    }

    return paramSpec;
  }

  private void reportNoClauses(DependentLink parameters, List<DependentLink> elimParams) {
    if (parameters.hasNext() && !parameters.getNext().hasNext()) {
      DataCallExpression dataCall = parameters.getTypeExpr().cast(DataCallExpression.class);
      if (dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", mySourceNode));
        return;
      }
    }

    // If paramSpec[p] is non-null, then it gives a specification for parameter p.
    // We should generate the list of patterns corresponding to the first projection of elements of paramSpec[p].
    // Moreover, for every index i and every parameter p' such that paramSpec[p][i].proj2[p'] is non-null and is equal to con,
    // then this spec applies only if the pattern generated for parameter p is of the form (con p_1 ... p_k).
    Map<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> paramSpec = new HashMap<>();
    Map<DependentLink, List<ConCallExpression>> paramSpec2 = new HashMap<>();
    for (DependentLink param = parameters; param.hasNext(); param = param.getNext()) {
      DataCallExpression dataCall = param.getTypeExpr().normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
      if (dataCall != null) {
        Map<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> newParamSpec = computeParamSpec(param, dataCall, elimParams, paramSpec2);
        if (newParamSpec == null) {
          return;
        }
        for (Map.Entry<DependentLink, List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>>> entry : newParamSpec.entrySet()) {
          paramSpec.compute(entry.getKey(), (k,list) -> {
            if (list == null) {
              return entry.getValue();
            }
            List<Pair<ExpressionPattern, Map<DependentLink, Constructor>>> result = new ArrayList<>();
            for (Pair<ExpressionPattern, Map<DependentLink, Constructor>> pair1 : list) {
              for (Pair<ExpressionPattern, Map<DependentLink, Constructor>> pair2 : entry.getValue()) {
                ExpressionPattern pattern = pair1.proj1.intersect(pair2.proj1);
                if (pattern != null) {
                  pair1.proj2.putAll(pair2.proj2);
                  result.add(new Pair<>(pattern, pair1.proj2));
                }
              }
            }
            return result;
          });
        }
      }
    }

    List<List<ExpressionPattern>> missingClauses = generateMissingClauses(elimParams.isEmpty() ? DependentLink.Helper.toList(parameters) : elimParams, 0, new ExprSubstitution(), paramSpec, paramSpec2);

    if (myLevel != null) {
      missingClauses.removeIf(clause -> numberOfIntervals(clause) > myLevel);
    }

    if (missingClauses.isEmpty()) {
      return;
    }

    if (missingClauses.size() == 1 && elimParams.isEmpty()) {
      boolean allVars = true;
      for (ExpressionPattern pattern : missingClauses.get(0)) {
        if (!(pattern instanceof BindingPattern)) {
          allVars = false;
          break;
        }
      }

      if (allVars) {
        myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.BODY_REQUIRED, mySourceNode));
        return;
      }
    }

    for (List<ExpressionPattern> patterns : missingClauses) {
      Collections.reverse(patterns);
    }

    if (!elimParams.isEmpty()) {
      for (List<ExpressionPattern> patterns : missingClauses) {
        DependentLink param = parameters;
        for (int i = 0; i < patterns.size(); i++, param = param.getNext()) {
          if (patterns.get(i) instanceof BindingPattern && !paramSpec.containsKey(param)) {
            ConstructorExpressionPattern newPattern;
            List<ExpressionPattern> subPatterns;
            Expression type = ((BindingPattern) patterns.get(i)).getBinding().getTypeExpr().getUnderlyingExpression();
            if (type instanceof SigmaExpression) {
              subPatterns = new ArrayList<>();
              newPattern = new ConstructorExpressionPattern((SigmaExpression) type, subPatterns);
            } else if (type instanceof ClassCallExpression) {
              subPatterns = new ArrayList<>();
              newPattern = new ConstructorExpressionPattern((ClassCallExpression) type, subPatterns);
            } else {
              continue;
            }

            patterns.set(i, newPattern);
            for (DependentLink link = newPattern.getParameters(); link.hasNext(); link = link.getNext()) {
              subPatterns.add(new BindingPattern(link));
            }
          }
        }
      }
    }

    myErrorReporter.report(new MissingClausesError(missingClauses, parameters, elimParams, true, mySourceNode));
  }

  private boolean reportMissingClauses(ElimTree elimTree, DependentLink parameters, List<DependentLink> elimParams) {
    if (myMissingClauses == null || myMissingClauses.isEmpty()) {
      return false;
    }

    List<List<ExpressionPattern>> missingClauses = new ArrayList<>(myMissingClauses.size());
    loop:
    for (Pair<List<Util.ClauseElem>, Boolean> missingClause : myMissingClauses) {
      List<ExpressionPattern> patterns = Util.unflattenClauses(missingClause.proj1);
      List<Expression> expressions = new ArrayList<>(patterns.size());
      for (ExpressionPattern pattern : patterns) {
        expressions.add(pattern.toExpression());
      }

      if (!missingClause.proj2) {
        if (elimTree != null && NormalizeVisitor.INSTANCE.doesEvaluate(elimTree, expressions, false)) {
          continue;
        }

        Util.addArguments(patterns, parameters);

        int i = patterns.size() - 1;
        for (; i >= 0; i--) {
          if (!(patterns.get(i) instanceof BindingPattern)) {
            break;
          }
        }
        DependentLink link = parameters;
        ExprSubstitution substitution = new ExprSubstitution();
        for (int j = 0; j < i + 1; j++) {
          substitution.add(link, patterns.get(j).toExpression());
          link = link.getNext();
        }
        for (; link.hasNext(); link = link.getNext()) {
          link = link.getNextTyped(null);
          List<ConCallExpression> conCalls = getMatchedConstructors(link.getTypeExpr().subst(substitution));
          if (conCalls != null && conCalls.isEmpty()) {
            continue loop;
          }
        }

        myOK = false;
      }

      Util.removeArguments(patterns, parameters, elimParams);
      missingClauses.add(patterns);
    }

    if (!missingClauses.isEmpty()) {
      myErrorReporter.report(new MissingClausesError(missingClauses, parameters, elimParams, true, mySourceNode));
      return true;
    }

    return false;
  }

  private List<IntervalElim.CasePair> clausesToIntervalElim(List<? extends ElimClause<? extends Pattern>> clauses, int prefix, DependentLink parameters) {
    List<IntervalElim.CasePair> result = new ArrayList<>();
    for (int i = 0; i < clauses.get(0).getPatterns().size(); i++) {
      Expression left = null;
      Expression right = null;

      for (int j = 0; j < clauses.size(); j++) {
        ElimClause<? extends Pattern> clause = clauses.get(j);
        if (!(clause.getPatterns().get(i) instanceof ConstructorPattern) || clause.getExpression() == null) {
          continue;
        }

        boolean found = false;
        Definition constructor = clause.getPatterns().get(i).getDefinition();
        if (constructor == Prelude.LEFT) {
          if (left == null) {
            found = true;
          }
        } else if (constructor == Prelude.RIGHT) {
          if (right == null) {
            found = true;
          }
        } else {
          throw new IllegalStateException();
        }

        if (found) {
          myUnusedClauses.remove(j + prefix);

          ExprSubstitution substitution = new ExprSubstitution();
          DependentLink oldLink = clause.getParameters();
          DependentLink newLink = parameters;
          for (int k = 0; newLink.hasNext(); k++, newLink = newLink.getNext()) {
            if (k == i) {
              continue;
            }
            substitution.add(oldLink, new ReferenceExpression(newLink));
            oldLink = oldLink.getNext();
          }

          if (constructor == Prelude.LEFT) {
            left = clause.getExpression().subst(substitution);
          } else {
            right = clause.getExpression().subst(substitution);
          }
        }

        if (left != null && right != null) {
          break;
        }
      }

      if (left != null && right == null || left == null && right != null) {
        List<Util.ClauseElem> missingClause = new ArrayList<>();
        int j = 0;
        for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), j++) {
          missingClause.add(new Util.PatternClauseElem(j == i
            ? new ConstructorExpressionPattern(left == null ? Left() : Right(), Collections.emptyList())
            : new BindingPattern(link)));
        }
        addMissingClause(missingClause, true);
      }

      result.add(new IntervalElim.CasePair(left, right));
    }
    return result;
  }

  private static boolean isConsequent(List<Integer> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) != i) {
        return false;
      }
    }
    return true;
  }

  private ElimTree clausesToElimTree(List<ExtElimClause> clauses, int argsStackSize, int numberOfIntervals) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = 0;
      loop:
      for (; index < clauses.get(0).getPatterns().size(); index++) {
        for (ExtElimClause clause : clauses) {
          if (!(clause.getPatterns().get(index) instanceof BindingPattern)) {
            if (clauses.get(0).getPatterns().get(index) instanceof BindingPattern && clause.getPatterns().get(index) instanceof ConstructorPattern) {
              Definition definition = clause.getPatterns().get(index).getDefinition();
              if (definition == Prelude.LEFT || definition == Prelude.RIGHT) {
                final int finalIndex = index;
                clauses = clauses.stream().filter(clauseData1 -> clauseData1.getPatterns().get(finalIndex) instanceof BindingPattern).collect(Collectors.toList());
                continue loop;
              }
            }
            break loop;
          }
        }
      }

      // If all patterns are variables
      if (index == clauses.get(0).getPatterns().size()) {
        ExtElimClause clause = clauses.get(0);
        myUnusedClauses.remove(clause.index);
        List<Integer> indices = clause.argIndices;
        if (index > clause.numberOfFakeVars) {
          indices = new ArrayList<>(indices);
          for (int i = clause.numberOfFakeVars; i < index; i++) {
            indices.add(argsStackSize + i);
          }
        }
        return new LeafElimTree(index, isConsequent(indices) ? null : indices, clause.index);
      }

      for (int i = 0; i < index; i++) {
        myContext.push(new Util.PatternClauseElem(clauses.get(0).getPatterns().get(i)));
      }

      ExtElimClause conClause = null;
      for (ExtElimClause clause : clauses) {
        Pattern pattern = clause.getPatterns().get(index);
        if (pattern instanceof EmptyPattern) {
          myUnusedClauses.remove(clause.index);
          return new BranchElimTree(index, false);
        }
        if (conClause == null && pattern instanceof ConstructorPattern) {
          conClause = clause;
        }
      }

      assert conClause != null;
      ConstructorExpressionPattern someConPattern = (ConstructorExpressionPattern) conClause.getPatterns().get(index);
      List<ConCallExpression> conCalls = null;
      List<BranchKey> branchKeys;
      DataDefinition dataType;
      if (someConPattern.getDefinition() instanceof Constructor) {
        dataType = ((Constructor) someConPattern.getDefinition()).getDataType();
        if (dataType.hasIndexedConstructors() || dataType == Prelude.PATH) {
          DataCallExpression dataCall = GetTypeVisitor.INSTANCE.visitConCall(((ConCallExpression) someConPattern.getDataExpression().subst(conClause.substitution)), null);
          conCalls = dataCall.getMatchedConstructors();
          if (conCalls == null) {
            myErrorReporter.report(new ImpossibleEliminationError(dataCall, getClause(conClause.index), null));
            myOK = false;
            return null;
          }
          branchKeys = new ArrayList<>(conCalls.size());
          for (ConCallExpression conCall : conCalls) {
            branchKeys.add(conCall.getDefinition());
          }
        } else {
          branchKeys = new ArrayList<>(dataType.getConstructors());
        }
      } else {
        if (someConPattern.getDataExpression() instanceof ClassCallExpression) {
          ClassCallExpression classCall = (ClassCallExpression) someConPattern.getDataExpression();
          branchKeys = Collections.singletonList(new ClassConstructor(classCall.getDefinition(), classCall.getSortArgument(), classCall.getImplementedHere().keySet()));
        } else if (someConPattern.getDataExpression() instanceof SigmaExpression) {
          branchKeys = Collections.singletonList(new TupleConstructor(someConPattern.getLength()));
        } else {
          assert someConPattern.getDefinition() == Prelude.IDP;
          branchKeys = Collections.singletonList(new IdpConstructor());
        }
        dataType = null;
      }

      if (dataType == Prelude.INTERVAL) {
        myErrorReporter.report(new TypecheckingError("Pattern matching on the interval is not allowed here", getClause(conClause.index)));
        myOK = false;
        return null;
      }

      if (dataType != null && dataType.isSquashed()) {
        if (myActualLevel != null && !Level.compare(myActualLevel, dataType.getSort().getHLevel().add(myActualLevelSub), CMP.LE, myEquations, getClause(conClause.index))) {
          myErrorReporter.report(new SquashedDataError(dataType, myActualLevel, myActualLevelSub, getClause(conClause.index)));
        }

        boolean ok = !dataType.isTruncated() || myLevel != null && myLevel <= dataType.getTruncatedLevel() + 1;
        if (!ok) {
          Expression type = myExpectedType.getType();
          if (type != null) {
            type = type.normalize(NormalizationMode.WHNF);
            UniverseExpression universe = type.cast(UniverseExpression.class);
            if (universe != null) {
              ok = Level.compare(universe.getSort().getHLevel(), dataType.getSort().getHLevel(), CMP.LE, myEquations, getClause(conClause.index));
            } else {
              InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, false, getClause(conClause.index));
              myEquations.addVariable(pl);
              ok = type.isLessOrEquals(new UniverseExpression(new Sort(new Level(pl), dataType.getSort().getHLevel())), myEquations, getClause(conClause.index));
            }
          }
        }
        if (!ok) {
          myErrorReporter.report(new TruncatedDataError(dataType, myExpectedType, getClause(conClause.index)));
          myOK = false;
        }
      }

      if (myLevel != null && !branchKeys.isEmpty() && !(branchKeys.get(0) instanceof SingleConstructor)) {
        //noinspection ConstantConditions
        branchKeys.removeIf(key -> numberOfIntervals + (key.getBody() instanceof IntervalElim ? ((IntervalElim) key.getBody()).getNumberOfTotalElim() : 0) > myLevel);
      }

      boolean hasVars = false;
      Map<BranchKey, List<ExtElimClause>> branchKeyMap = new LinkedHashMap<>();
      for (ExtElimClause clause : clauses) {
        if (clause.getPatterns().get(index) instanceof BindingPattern) {
          hasVars = true;
          for (BranchKey key : branchKeys) {
            branchKeyMap.computeIfAbsent(key, k -> new ArrayList<>()).add(clause);
          }
        } else {
          Definition def = clause.getPatterns().get(index).getDefinition();
          BranchKey key = def instanceof Constructor ? (Constructor) def : null;
          if (key == null && !branchKeys.isEmpty() && branchKeys.get(0) instanceof SingleConstructor) {
            key = branchKeys.get(0);
          }
          if (key != null) {
            branchKeyMap.computeIfAbsent(key, k -> new ArrayList<>()).add(clause);
          }
        }
      }

      if (myMode.checkCoverage() && !hasVars) {
        for (BranchKey key : branchKeys) {
          if (!branchKeyMap.containsKey(key)) {
            try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
              myContext.push(Util.makeDataClauseElem(key, someConPattern));
              for (DependentLink link = key.getParameters(); link.hasNext(); link = link.getNext()) {
                myContext.push(new Util.PatternClauseElem(new BindingPattern(link)));
              }
              addMissingClause(new ArrayList<>(myContext), false);
            }
          }
        }
      }

      BranchElimTree branchElimTree = new BranchElimTree(index, hasVars);
      for (BranchKey branchKey : branchKeys) {
        List<ExtElimClause> conClauseList = branchKeyMap.get(branchKey);
        if (conClauseList == null) {
          continue;
        }
        myContext.push(Util.makeDataClauseElem(branchKey, someConPattern));

        for (int i = 0; i < conClauseList.size(); i++) {
          ExtElimClause clause = conClauseList.get(i);
          List<Integer> indices = new ArrayList<>(clause.argIndices);
          for (int j = clause.numberOfFakeVars; j < index; j++) {
            indices.add(argsStackSize + j);
          }
          int numberOfFakeVars = Math.max(clause.numberOfFakeVars - index, 0);

          List<ExpressionPattern> patterns = new ArrayList<>();
          List<ExpressionPattern> oldPatterns = clause.getPatterns();
          ExprSubstitution newSubstitution;
          if (oldPatterns.get(index) instanceof ConstructorExpressionPattern) {
            patterns.addAll(oldPatterns.get(index).getSubPatterns());
            newSubstitution = conClauseList.get(i).substitution;
          } else {
            Expression substExpr;
            DependentLink conParameters;
            List<Expression> arguments = new ArrayList<>();
            if (conCalls != null) {
              ConCallExpression conCall = null;
              for (ConCallExpression conCall1 : conCalls) {
                if (conCall1.getDefinition() == branchKey) {
                  conCall = conCall1;
                  break;
                }
              }
              assert conCall != null;
              List<Expression> dataTypesArgs = conCall.getDataTypeArguments();
              substExpr = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), dataTypesArgs, arguments);
              conParameters = DependentLink.Helper.subst(branchKey.getParameters(), DependentLink.Helper.toSubstitution(conCall.getDefinition().getDataTypeParameters(), dataTypesArgs));
            } else {
              if (branchKey instanceof SingleConstructor) {
                conParameters = someConPattern.getParameters();
                Expression someExpr = someConPattern.getDataExpression();
                if (someExpr instanceof ClassCallExpression) {
                  ClassCallExpression classCall = (ClassCallExpression) someExpr;
                  Map<ClassField, Expression> implementations = new HashMap<>();
                  DependentLink link = conParameters;
                  for (ClassField field : classCall.getDefinition().getFields()) {
                    if (!classCall.isImplemented(field)) {
                      implementations.put(field, new ReferenceExpression(link));
                      link = link.getNext();
                    }
                  }
                  substExpr = new NewExpression(null, new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES));
                } else if (someExpr instanceof SigmaExpression) {
                  substExpr = new TupleExpression(arguments, (SigmaExpression) someExpr);
                  conParameters = DependentLink.Helper.copy(conParameters);
                } else if (someExpr instanceof FunCallExpression) {
                  substExpr = someExpr;
                } else {
                  throw new IllegalStateException();
                }
              } else if (branchKey instanceof Constructor) {
                List<Expression> dataTypesArgs = new ArrayList<>();
                for (Expression dataTypeArg : someConPattern.getDataTypeArguments()) {
                  dataTypesArgs.add(dataTypeArg.subst(conClause.substitution));
                }
                Constructor constructor = (Constructor) branchKey;
                substExpr = ConCallExpression.make(constructor, someConPattern.getSortArgument(), dataTypesArgs, arguments);
                conParameters = DependentLink.Helper.subst(constructor.getParameters(), DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), someConPattern.getDataTypeArguments()));
              } else {
                throw new IllegalStateException();
              }
            }

            if (numberOfFakeVars == 0) {
              indices.add(argsStackSize + index);
            } else {
              numberOfFakeVars--;
            }
            for (DependentLink link = conParameters; link.hasNext(); link = link.getNext()) {
              patterns.add(new BindingPattern(link));
              arguments.add(new ReferenceExpression(link));
              numberOfFakeVars++;
            }

            newSubstitution = new ExprSubstitution(conClauseList.get(i).substitution);
            newSubstitution.addSubst(((BindingPattern) oldPatterns.get(index)).getBinding(), substExpr);
          }

          patterns.addAll(oldPatterns.subList(index + 1, oldPatterns.size()));
          conClauseList.set(i, new ExtElimClause(patterns, clause.getExpression(), clause.index, indices, numberOfFakeVars, newSubstitution));
        }

        ElimTree elimTree = clausesToElimTree(conClauseList, argsStackSize + index + (hasVars ? 1 : 0), myLevel == null ? 0 : numberOfIntervals + (branchKey.getBody() instanceof IntervalElim ? ((IntervalElim) branchKey.getBody()).getNumberOfTotalElim() : 0));
        if (elimTree == null) {
          myOK = false;
        } else {
          branchElimTree.addChild(branchKey, elimTree);
        }

        myContext.pop();

        // If we match on a variable and the constructor has conditions,
        // we need to check that it isn't mapped to a clause with a variable
        // unless constructors to which the current one evaluates is also mapped to the same clause.
        // We need this because condition checker doesn't check clauses with variables.
        if (hasVars && dataType != null && branchKey instanceof Constructor && branchKey.getBody() != null) {
          Set<Integer> indices = new HashSet<>();
          collectClauseIndices(elimTree, indices);
          for (ExtElimClause clause : clauses) {
            if (!(clause.getPatterns().get(index) instanceof BindingPattern)) {
              indices.remove(clause.index);
            }
          }

          if (!indices.isEmpty()) {
            Set<Integer> depIndices = new HashSet<>();
            Set<Constructor> depConstructors = new HashSet<>();
            collectConstructors(dataType, branchKey.getBody(), depConstructors);
            boolean ok = true;
            for (Constructor depConstructor : depConstructors) {
              ElimTree depElimTree = branchElimTree.getChild(depConstructor);
              if (depElimTree == null) {
                ok = false;
              } else {
                collectClauseIndices(depElimTree, depIndices);
              }
            }

            if (ok && !depIndices.isEmpty()) {
              if (indices.size() > 1) {
                ok = false;
              } else {
                ok = depIndices.size() == 1 && depIndices.iterator().next().equals(indices.iterator().next());
              }
            }

            if (!ok) {
              Concrete.SourceNode sourceNode;
              if (myClauses != null) {
                Concrete.FunctionClause functionClause = myClauses.get(indices.iterator().next());
                sourceNode = index < functionClause.getPatterns().size() ? functionClause.getPatterns().get(index) : functionClause;
              } else {
                sourceNode = mySourceNode;
              }
              myErrorReporter.report(new HigherConstructorMatchingError((Constructor) branchKey, sourceNode));
            }
          }
        }
      }

      return branchElimTree;
    }
  }

  private void addMissingClause(List<Util.ClauseElem> clause, boolean isInterval) {
    if (myMissingClauses == null) {
      myMissingClauses = new ArrayList<>();
    }
    myMissingClauses.add(new Pair<>(clause, isInterval));
  }

  private void collectClauseIndices(ElimTree elimTree, Set<Integer> indices) {
    if (elimTree instanceof LeafElimTree) {
      indices.add(((LeafElimTree) elimTree).getClauseIndex());
    } else if (elimTree instanceof BranchElimTree) {
      for (Map.Entry<BranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        collectClauseIndices(entry.getValue(), indices);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  private void collectConstructors(DataDefinition dataDef, Body body, Set<Constructor> result) {
    if (body == null) {
      return;
    }

    if (body instanceof Expression) {
      if (body instanceof ConCallExpression) {
        result.add(((ConCallExpression) body).getDefinition());
      } else {
        result.addAll(dataDef.getConstructors());
      }
    } else if (body instanceof ElimBody) {
      for (ElimClause<Pattern> clause : ((ElimBody) body).getClauses()) {
        collectConstructors(dataDef, clause.getExpression(), result);
      }
    } else if (body instanceof IntervalElim) {
      for (IntervalElim.CasePair pair : ((IntervalElim) body).getCases()) {
        collectConstructors(dataDef, pair.proj1, result);
        collectConstructors(dataDef, pair.proj2, result);
      }
      collectConstructors(dataDef, ((IntervalElim) body).getOtherwise(), result);
    } else {
      throw new IllegalStateException();
    }
  }
}
