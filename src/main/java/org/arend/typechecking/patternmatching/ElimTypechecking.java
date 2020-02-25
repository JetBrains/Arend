package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.constructor.TupleConstructor;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
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
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.Referable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DumbTypechecker;
import org.arend.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.arend.core.expr.ExpressionFactory.Left;
import static org.arend.core.expr.ExpressionFactory.Right;

public class ElimTypechecking {
  private final CheckTypeVisitor myVisitor;
  private Set<Concrete.FunctionClause> myUnusedClauses;
  private final PatternTypechecking.Mode myMode;
  private final Expression myExpectedType;
  private final Integer myLevel;
  private final Level myActualLevel;
  private final int myActualLevelSub;
  private boolean myOK;
  private Stack<Util.ClauseElem> myContext;
  private List<Pair<List<Util.ClauseElem>, Boolean>> myMissingClauses;

  private static Integer getMinPlus1(Integer level1, Level l2, int sub) {
    Integer level2 = !l2.isInfinity() && l2.isClosed() ? l2.getConstant() : null;
    Integer result = level1 != null && level2 != null ? Integer.valueOf(Math.min(level1, level2 - sub)) : level2 != null ? Integer.valueOf(level2 - sub) : level1;
    return result == null ? null : result + 1;
  }

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, PatternTypechecking.Mode mode, @Nullable Integer level, @Nonnull Level actualLevel, int actualLevelSub, boolean isSFunc) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myMode = mode;
    myLevel = getMinPlus1(level, actualLevel, actualLevelSub);
    myActualLevel = isSFunc ? null : actualLevel;
    myActualLevelSub = isSFunc ? 0 : actualLevelSub;
  }

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, PatternTypechecking.Mode mode) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myMode = mode;
    myLevel = null;
    myActualLevel = Level.INFINITY;
    myActualLevelSub = 0;
  }

  public static List<DependentLink> getEliminatedParameters(List<? extends Concrete.ReferenceExpression> expressions, List<? extends Concrete.Clause> clauses, DependentLink parameters, CheckTypeVisitor visitor) {
    return getEliminatedParameters(expressions, clauses, parameters, visitor.getErrorReporter(), visitor.getContext());
  }

  public static List<DependentLink> getEliminatedParameters(List<? extends Concrete.ReferenceExpression> expressions, List<? extends Concrete.Clause> clauses, DependentLink parameters, ErrorReporter errorReporter, Map<Referable, Binding> context) {
    List<DependentLink> elimParams = Collections.emptyList();
    if (!expressions.isEmpty()) {
      int expectedNumberOfPatterns = expressions.size();
      DumbTypechecker.findImplicitPatterns(clauses, errorReporter, false);
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
    final Concrete.FunctionClause clause;
    final List<Integer> argIndices;
    final int numberOfFakeVars;
    final ExprSubstitution substitution; // substitutes pattern variables which are replaced with a constructor

    public ExtElimClause(List<ExpressionPattern> patterns, Expression expression, int index, Concrete.FunctionClause clause, List<Integer> argIndices, int numberOfFakeVars, ExprSubstitution substitution) {
      super(patterns, expression);
      this.index = index;
      this.clause = clause;
      this.argIndices = argIndices;
      this.numberOfFakeVars = numberOfFakeVars;
      this.substitution = substitution;
    }

    public ExtElimClause(List<ExpressionPattern> patterns, Expression expression, int index, Concrete.FunctionClause clause) {
      this(patterns, expression, index, clause, new ArrayList<>(), 0, new ExprSubstitution());
    }
  }

  private static List<ElimClause<Pattern>> removeExpressionsFromPatterns(List<? extends ElimClause<ExpressionPattern>> clauses) {
    List<ElimClause<Pattern>> result = new ArrayList<>();
    for (ElimClause<? extends ExpressionPattern> clause : clauses) {
      result.add(new ElimClause<>(ExpressionPattern.removeExpressions(clause.getPatterns()), clause.getExpression()));
    }
    return result;
  }

  public ElimBody typecheckElim(List<? extends ElimClause<ExpressionPattern>> clauses, List<? extends Concrete.FunctionClause> funClauses, Concrete.SourceNode sourceNode, DependentLink parameters) {
    assert !myMode.allowInterval();
    return (ElimBody) typecheckElim(clauses, funClauses, sourceNode, null, parameters, Collections.emptyList());
  }

  public Body typecheckElim(List<? extends ElimClause<ExpressionPattern>> clauses, List<? extends Concrete.FunctionClause> funClauses, Concrete.SourceNode sourceNode, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams) {
    myUnusedClauses = new LinkedHashSet<>(funClauses);

    List<ElimClause<ExpressionPattern>> intervalClauses;
    List<ExtElimClause> nonIntervalClauses = new ArrayList<>();
    if (myMode.allowInterval()) {
      intervalClauses = new ArrayList<>();
      Concrete.FunctionClause errorClause = null;
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
          nonIntervalClauses.add(new ExtElimClause(clause.getPatterns(), clause.getExpression(), i, funClauses.get(i)));
          if (!intervalClauses.isEmpty() && errorClause == null) {
            errorClause = funClauses.get(i);
          }
        } else {
          if (intervals > 1) {
            myVisitor.getErrorReporter().report(new TypecheckingError("Only a single interval pattern per row is allowed", funClauses.get(i)));
            myUnusedClauses.remove(funClauses.get(i));
          } else {
            intervalClauses.add(clause);
          }
          clauses.remove(i--);
        }
      }
      if (errorClause != null) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Non-interval clauses must be placed before the interval ones", errorClause));
      }
    } else {
      intervalClauses = Collections.emptyList();
      for (int i = 0; i < clauses.size(); i++) {
        nonIntervalClauses.add(new ExtElimClause(clauses.get(i).getPatterns(), clauses.get(i).getExpression(), i, funClauses.get(i)));
      }
    }

    List<IntervalElim.CasePair> cases = intervalClauses.isEmpty() ? null : clausesToIntervalElim(intervalClauses, funClauses, parameters);
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
            myVisitor.getErrorReporter().report(new TypecheckingError("A pattern matching on a data type is allowed only before the pattern matching on the interval", funClauses.get(k)));
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
        if (!reportMissingClauses(null, sourceNode, abstractParameters, parameters, elimParams)) {
          reportNoClauses(sourceNode, abstractParameters, parameters, elimParams);
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

      reportMissingClauses(elimTree, sourceNode, abstractParameters, parameters, elimParams);

      if (myOK) {
        for (Concrete.FunctionClause clause : myUnusedClauses) {
          myVisitor.getErrorReporter().report(new CertainTypecheckingError(CertainTypecheckingError.Kind.REDUNDANT_CLAUSE, clause));
        }
      }
    }

    ElimBody elimBody = elimTree == null ? null : new ElimBody(removeExpressionsFromPatterns(clauses), elimTree);
    return cases == null ? elimBody : new IntervalElim(DependentLink.Helper.size(parameters), cases, elimBody);
  }

  private static List<ConCallExpression> getMatchedConstructors(Expression expr) {
    DataCallExpression dataCall = expr.normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
    return dataCall == null ? null : dataCall.getMatchedConstructors();
  }

  private static List<List<ExpressionPattern>> generateMissingClauses(List<DependentLink> eliminatedParameters, int i, ExprSubstitution substitution) {
    if (i == eliminatedParameters.size()) {
      List<List<ExpressionPattern>> result = new ArrayList<>();
      result.add(new ArrayList<>());
      return result;
    }

    DependentLink link = eliminatedParameters.get(i);
    List<ConCallExpression> conCalls = getMatchedConstructors(link.getTypeExpr().subst(substitution));
    if (conCalls != null) {
      List<List<ExpressionPattern>> totalResult = new ArrayList<>();
      for (ConCallExpression conCall : conCalls) {
        List<Expression> arguments = new ArrayList<>();
        List<ExpressionPattern> subPatterns = new ArrayList<>();
        for (DependentLink link1 = conCall.getDefinition().getParameters(); link1.hasNext(); link1 = link1.getNext()) {
          arguments.add(new ReferenceExpression(link1));
          subPatterns.add(new BindingPattern(link1));
        }
        substitution.add(link, ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments));
        List<List<ExpressionPattern>> result = generateMissingClauses(eliminatedParameters, i + 1, substitution);
        for (List<ExpressionPattern> patterns : result) {
          patterns.add(new ConstructorExpressionPattern(conCall, subPatterns));
        }
        totalResult.addAll(result);
      }
      substitution.remove(link);
      return totalResult;
    } else {
      List<List<ExpressionPattern>> result = generateMissingClauses(eliminatedParameters, i + 1, substitution);
      for (List<ExpressionPattern> patterns : result) {
        patterns.add(new BindingPattern(link));
      }
      return result;
    }
  }

  private void reportNoClauses(Concrete.SourceNode sourceNode, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams) {
    if (parameters.hasNext() && !parameters.getNext().hasNext()) {
      DataCallExpression dataCall = parameters.getTypeExpr().cast(DataCallExpression.class);
      if (dataCall != null && dataCall.getDefinition() == Prelude.INTERVAL) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Pattern matching on the interval is not allowed here", sourceNode));
        return;
      }
    }

    List<List<ExpressionPattern>> missingClauses = generateMissingClauses(elimParams.isEmpty() ? DependentLink.Helper.toList(parameters) : elimParams, 0, new ExprSubstitution());

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
        myVisitor.getErrorReporter().report(new CertainTypecheckingError(CertainTypecheckingError.Kind.BODY_REQUIRED, sourceNode));
        return;
      }
    }

    for (List<ExpressionPattern> patterns : missingClauses) {
      Collections.reverse(patterns);
    }

    if (!elimParams.isEmpty()) {
      for (List<ExpressionPattern> patterns : missingClauses) {
        for (int i = 0; i < patterns.size(); i++) {
          if (patterns.get(i) instanceof BindingPattern) {
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

    myVisitor.getErrorReporter().report(new MissingClausesError(missingClauses, abstractParameters, parameters, elimParams, sourceNode));
  }

  private boolean reportMissingClauses(ElimTree elimTree, Concrete.SourceNode sourceNode, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams) {
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
      myVisitor.getErrorReporter().report(new MissingClausesError(missingClauses, abstractParameters, parameters, elimParams, sourceNode));
      return true;
    }

    return false;
  }

  private List<IntervalElim.CasePair> clausesToIntervalElim(List<? extends ElimClause<? extends Pattern>> clauses, List<? extends Concrete.FunctionClause> funClauses, DependentLink parameters) {
    List<IntervalElim.CasePair> result = new ArrayList<>();
    for (int i = 0; i < clauses.get(0).getPatterns().size(); i++) {
      Expression left = null;
      Expression right = null;

      for (int j = 0; j < clauses.size(); j++) {
        ElimClause<? extends Pattern> clause = clauses.get(j);
        if (!(clause.getPatterns().get(i) instanceof ConstructorPattern)) {
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
          myUnusedClauses.remove(funClauses.get(j));

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
        myUnusedClauses.remove(clause.clause);
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
          myUnusedClauses.remove(clause.clause);
          return new BranchElimTree(index, false);
        }
        if (conClause == null && pattern instanceof ConstructorPattern) {
          conClause = clause;
        }
      }

      assert conClause != null;
      ConstructorExpressionPattern someConPattern = (ConstructorExpressionPattern) conClause.getPatterns().get(index);
      List<ConCallExpression> conCalls = null;
      List<Constructor> constructors;
      DataDefinition dataType;
      if (someConPattern.getDefinition() instanceof Constructor) {
        dataType = ((Constructor) someConPattern.getDefinition()).getDataType();
        if (dataType.hasIndexedConstructors()) {
          DataCallExpression dataCall = GetTypeVisitor.INSTANCE.visitConCall(((ConCallExpression) someConPattern.getDataExpression().subst(conClause.substitution)), null);
          conCalls = dataCall.getMatchedConstructors();
          if (conCalls == null) {
            myVisitor.getErrorReporter().report(new ImpossibleEliminationError(dataCall, conClause.clause));
            myOK = false;
            return null;
          }
          constructors = new ArrayList<>(conCalls.size());
          for (ConCallExpression conCall : conCalls) {
            constructors.add(conCall.getDefinition());
          }
        } else {
          constructors = new ArrayList<>(dataType.getConstructors());
        }
      } else {
        if (someConPattern.getDataExpression() instanceof ClassCallExpression) {
          ClassCallExpression classCall = (ClassCallExpression) someConPattern.getDataExpression();
          constructors = Collections.singletonList(new ClassConstructor(classCall.getDefinition(), classCall.getSortArgument(), classCall.getImplementedHere().keySet()));
        } else if (someConPattern.getDataExpression() instanceof SigmaExpression) {
          constructors = Collections.singletonList(new TupleConstructor(someConPattern.getLength()));
        } else {
          assert someConPattern.getDefinition() == Prelude.IDP;
          constructors = Collections.singletonList(new IdpConstructor());
        }
        dataType = null;
      }

      if (dataType == Prelude.INTERVAL) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Pattern matching on the interval is not allowed here", conClause.clause));
        myOK = false;
        return null;
      }

      if (dataType != null && dataType.isSquashed()) {
        if (myActualLevel != null && !Level.compare(myActualLevel, dataType.getSort().getHLevel().add(myActualLevelSub), CMP.LE, myVisitor.getEquations(), conClause.clause)) {
          myVisitor.getErrorReporter().report(new SquashedDataError(dataType, myActualLevel, myActualLevelSub, conClause.clause));
        }

        boolean ok = !dataType.isTruncated() || myLevel != null && myLevel <= dataType.getSort().getHLevel().getConstant() + 1;
        if (!ok) {
          Expression type = myExpectedType.getType();
          if (type != null) {
            type = type.normalize(NormalizationMode.WHNF);
            UniverseExpression universe = type.cast(UniverseExpression.class);
            if (universe != null) {
              ok = Level.compare(universe.getSort().getHLevel(), dataType.getSort().getHLevel(), CMP.LE, myVisitor.getEquations(), conClause.clause);
            } else {
              InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, false, conClause.clause);
              myVisitor.getEquations().addVariable(pl);
              ok = type.isLessOrEquals(new UniverseExpression(new Sort(new Level(pl), dataType.getSort().getHLevel())), myVisitor.getEquations(), conClause.clause);
            }
          }
        }
        if (!ok) {
          myVisitor.getErrorReporter().report(new TruncatedDataError(dataType, myExpectedType, conClause.clause));
          myOK = false;
        }
      }

      if (myLevel != null && !constructors.isEmpty() && !(constructors.get(0) instanceof SingleConstructor)) {
        //noinspection ConstantConditions
        constructors.removeIf(constructor -> numberOfIntervals + (constructor.getBody() instanceof IntervalElim ? ((IntervalElim) constructor.getBody()).getNumberOfTotalElim() : 0) > myLevel);
      }

      boolean hasVars = false;
      Map<Constructor, List<ExtElimClause>> constructorMap = new LinkedHashMap<>();
      for (ExtElimClause clause : clauses) {
        if (clause.getPatterns().get(index) instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clause);
          }
        } else {
          Definition def = clause.getPatterns().get(index).getDefinition();
          if (!(def instanceof Constructor) && !constructors.isEmpty() && constructors.get(0) instanceof SingleConstructor) {
            def = constructors.get(0);
          }
          if (def instanceof Constructor) {
            constructorMap.computeIfAbsent((Constructor) def, k -> new ArrayList<>()).add(clause);
          }
        }
      }

      if (myMode.checkCoverage() && !hasVars) {
        for (Constructor constructor : constructors) {
          if (!constructorMap.containsKey(constructor)) {
            try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
              myContext.push(Util.makeDataClauseElem(constructor, someConPattern));
              for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext()) {
                myContext.push(new Util.PatternClauseElem(new BindingPattern(link)));
              }
              addMissingClause(new ArrayList<>(myContext), false);
            }
          }
        }
      }

      BranchElimTree branchElimTree = new BranchElimTree(index, hasVars);
      for (Constructor constructor : constructors) {
        List<ExtElimClause> conClauseList = constructorMap.get(constructor);
        if (conClauseList == null) {
          continue;
        }
        myContext.push(Util.makeDataClauseElem(constructor, someConPattern));

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
            patterns.addAll(((ConstructorExpressionPattern) oldPatterns.get(index)).getSubPatterns());
            newSubstitution = conClauseList.get(i).substitution;
          } else {
            Expression substExpr;
            DependentLink conParameters;
            List<Expression> arguments = new ArrayList<>(patterns.size());
            if (conCalls != null) {
              ConCallExpression conCall = null;
              for (ConCallExpression conCall1 : conCalls) {
                if (conCall1.getDefinition() == constructor) {
                  conCall = conCall1;
                  break;
                }
              }
              assert conCall != null;
              List<Expression> dataTypesArgs = conCall.getDataTypeArguments();
              substExpr = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), dataTypesArgs, arguments);
              conParameters = DependentLink.Helper.subst(constructor.getParameters(), DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), dataTypesArgs));
            } else {
              if (constructor instanceof SingleConstructor) {
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
              } else {
                List<Expression> dataTypesArgs = new ArrayList<>();
                for (Expression dataTypeArg : someConPattern.getDataTypeArguments()) {
                  dataTypesArgs.add(dataTypeArg.subst(conClause.substitution));
                }
                substExpr = ConCallExpression.make(constructor, someConPattern.getSortArgument(), dataTypesArgs, arguments);
                conParameters = DependentLink.Helper.subst(constructor.getParameters(), DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), someConPattern.getDataTypeArguments()));
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
          conClauseList.set(i, new ExtElimClause(patterns, clause.getExpression(), clause.index, clause.clause, indices, numberOfFakeVars, newSubstitution));
        }

        ElimTree elimTree = clausesToElimTree(conClauseList, argsStackSize + index + (hasVars ? 1 : 0), myLevel == null ? 0 : numberOfIntervals + (constructor.getBody() instanceof IntervalElim ? ((IntervalElim) constructor.getBody()).getNumberOfTotalElim() : 0));
        if (elimTree == null) {
          myOK = false;
        } else {
          branchElimTree.addChild(constructor, elimTree);
        }

        myContext.pop();
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
}
