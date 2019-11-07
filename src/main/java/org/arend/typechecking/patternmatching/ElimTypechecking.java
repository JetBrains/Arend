package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.constructor.TupleConstructor;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.GetTypeVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;
import org.arend.naming.reference.Referable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.result.TypecheckingResult;
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
  private final EnumSet<PatternTypechecking.Flag> myFlags;
  private final Expression myExpectedType;
  private final Integer myLevel;
  private final Level myActualLevel;
  private final int myActualLevelSub;
  private boolean myOK;
  private Stack<Util.ClauseElem> myContext;
  private List<Pair<List<Util.ClauseElem>, Boolean>> myMissingClauses;
  private final boolean myCase;

  private static Integer getMinPlus1(Integer level1, Level l2, int sub) {
    Integer level2 = !l2.isInfinity() && l2.isClosed() ? l2.getConstant() : null;
    Integer result = level1 != null && level2 != null ? Integer.valueOf(Math.min(level1, level2 - sub)) : level2 != null ? Integer.valueOf(level2 - sub) : level1;
    return result == null ? null : result + 1;
  }

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, EnumSet<PatternTypechecking.Flag> flags, @Nullable Integer level, @Nonnull Level actualLevel, int actualLevelSub, boolean isSFunc, boolean isCase) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myFlags = flags;
    myLevel = getMinPlus1(level, actualLevel, actualLevelSub);
    myActualLevel = isSFunc ? null : actualLevel;
    myActualLevelSub = isSFunc ? 0 : actualLevelSub;
    myCase = isCase;
  }

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, EnumSet<PatternTypechecking.Flag> flags) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myFlags = flags;
    myLevel = null;
    myActualLevel = Level.INFINITY;
    myActualLevelSub = 0;
    myCase = false;
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
            errorReporter.report(new TypecheckingError(TypecheckingError.Kind.TOO_MANY_PATTERNS, clause.getPatterns().get(expectedNumberOfPatterns)));
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

  public ElimTree typecheckElim(List<? extends Concrete.FunctionClause> funClauses, Concrete.SourceNode sourceNode, DependentLink parameters, List<Clause> resultClauses) {
    assert !myFlags.contains(PatternTypechecking.Flag.ALLOW_INTERVAL);
    return (ElimTree) typecheckElim(funClauses, sourceNode, null, parameters, Collections.emptyList(), resultClauses);
  }

  public Body typecheckElim(List<? extends Concrete.FunctionClause> funClauses, Concrete.SourceNode sourceNode, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams, List<Clause> resultClauses) {
    List<ExtClause> clauses = new ArrayList<>(funClauses.size());
    PatternTypechecking patternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), myFlags, myVisitor);
    myOK = true;
    for (Concrete.FunctionClause clause : funClauses) {
      Pair<List<Pattern>, TypecheckingResult> result = patternTypechecking.typecheckClause(clause, abstractParameters, parameters, elimParams, myExpectedType);
      if (result == null) {
        myOK = false;
      } else {
        clauses.add(new ExtClause(result.proj1, result.proj2 == null ? null : result.proj2.expression, new ExprSubstitution(), clause));
      }
    }
    if (!myOK) {
      return null;
    }

    myUnusedClauses = new LinkedHashSet<>(funClauses);

    List<ExtClause> intervalClauses = Collections.emptyList();
    List<ExtClause> nonIntervalClauses = clauses;
    if (myFlags.contains(PatternTypechecking.Flag.ALLOW_INTERVAL)) {
      intervalClauses = new ArrayList<>();
      nonIntervalClauses = new ArrayList<>();
      for (ExtClause clause : clauses) {
        boolean hasNonIntervals = false;
        int intervals = 0;
        for (Pattern pattern : clause.patterns) {
          if (pattern instanceof BindingPattern) {
            continue;
          }
          if (pattern instanceof ConstructorPattern) {
            Definition definition = ((ConstructorPattern) pattern).getDefinition();
            if (definition == Prelude.LEFT || definition == Prelude.RIGHT) {
              intervals++;
              continue;
            }
          }
          hasNonIntervals = true;
          break;
        }
        if (hasNonIntervals || intervals == 0) {
          nonIntervalClauses.add(clause);
          if (resultClauses != null) {
            resultClauses.add(new Clause(clause.patterns, clause.expression, clause.clause));
          }
        } else {
          if (intervals > 1) {
            myVisitor.getErrorReporter().report(new TypecheckingError("Only a single interval pattern per row is allowed", clause.clause));
            myUnusedClauses.remove(clause.clause);
          } else {
            intervalClauses.add(clause);
          }
        }
      }
    }

    List<Pair<Expression, Expression>> cases = intervalClauses.isEmpty() ? null : clausesToIntervalElim(intervalClauses, parameters);
    if (cases != null) {
      int i = 0;
      for (; i < cases.size(); i++) {
        if (cases.get(i).proj1 != null || cases.get(i).proj2 != null) {
          break;
        }
      }
      cases = cases.subList(i, cases.size());

      for (ExtClause clause : nonIntervalClauses) {
        for (int j = i; j < clause.patterns.size(); j++) {
          if (!(clause.patterns.get(j) instanceof BindingPattern)) {
            myVisitor.getErrorReporter().report(new TypecheckingError("A pattern matching on a data type is allowed only before the pattern matching on the interval", clause.clause));
            myOK = false;
          }
        }
      }
    }

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

      if (emptyLink == null && myFlags.contains(PatternTypechecking.Flag.CHECK_COVERAGE)) {
        if (!reportMissingClauses(null, sourceNode, abstractParameters, parameters, elimParams)) {
          reportNoClauses(sourceNode, abstractParameters, parameters, elimParams);
        }
      }

      ElimTree elimTree = null;
      if (emptyLink != null) {
        int index = 0;
        for (DependentLink link = parameters; link != emptyLink; link = link.getNext()) {
          index++;
        }
        elimTree = new BranchElimTree(DependentLink.Helper.take(parameters, index), Collections.emptyMap());
      }

      return cases == null ? elimTree : new IntervalElim(DependentLink.Helper.size(parameters), cases, elimTree);
    }

    myContext = new Stack<>();
    ElimTree elimTree = clausesToElimTree(nonIntervalClauses, 0);

    reportMissingClauses(elimTree, sourceNode, abstractParameters, parameters, elimParams);

    if (myOK) {
      for (Concrete.FunctionClause clause : myUnusedClauses) {
        myVisitor.getErrorReporter().report(new TypecheckingError(TypecheckingError.Kind.REDUNDANT_CLAUSE, clause));
      }
    }
    return cases == null ? elimTree : new IntervalElim(DependentLink.Helper.size(parameters), cases, elimTree);
  }

  private static List<ConCallExpression> getMatchedConstructors(Expression expr) {
    DataCallExpression dataCall = expr.normalize(NormalizeVisitor.Mode.WHNF).cast(DataCallExpression.class);
    return dataCall == null ? null : dataCall.getMatchedConstructors();
  }

  private static List<List<Pattern>> generateMissingClauses(List<DependentLink> eliminatedParameters, int i, ExprSubstitution substitution) {
    if (i == eliminatedParameters.size()) {
      List<List<Pattern>> result = new ArrayList<>();
      result.add(new ArrayList<>());
      return result;
    }

    DependentLink link = eliminatedParameters.get(i);
    List<ConCallExpression> conCalls = getMatchedConstructors(link.getTypeExpr().subst(substitution));
    if (conCalls != null) {
      List<List<Pattern>> totalResult = new ArrayList<>();
      for (ConCallExpression conCall : conCalls) {
        List<Expression> arguments = new ArrayList<>();
        List<Pattern> patternArgs = new ArrayList<>();
        for (DependentLink link1 = conCall.getDefinition().getParameters(); link1.hasNext(); link1 = link1.getNext()) {
          arguments.add(new ReferenceExpression(link1));
          patternArgs.add(new BindingPattern(link1));
        }
        substitution.add(link, ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments));
        List<List<Pattern>> result = generateMissingClauses(eliminatedParameters, i + 1, substitution);
        for (List<Pattern> patterns : result) {
          patterns.add(new ConstructorPattern(conCall, new Patterns(patternArgs)));
        }
        totalResult.addAll(result);
      }
      substitution.remove(link);
      return totalResult;
    } else {
      List<List<Pattern>> result = generateMissingClauses(eliminatedParameters, i + 1, substitution);
      for (List<Pattern> patterns : result) {
        patterns.add(new BindingPattern(link));
      }
      return result;
    }
  }

  private void reportNoClauses(Concrete.SourceNode sourceNode, List<? extends Concrete.Parameter> abstractParameters, DependentLink parameters, List<DependentLink> elimParams) {
    List<List<Pattern>> missingClauses = generateMissingClauses(elimParams.isEmpty() ? DependentLink.Helper.toList(parameters) : elimParams, 0, new ExprSubstitution());

    if (missingClauses.isEmpty()) {
      return;
    }

    if (missingClauses.size() == 1 && elimParams.isEmpty()) {
      boolean allVars = true;
      for (Pattern pattern : missingClauses.get(0)) {
        if (!(pattern instanceof BindingPattern)) {
          allVars = false;
          break;
        }
      }

      if (allVars) {
        myVisitor.getErrorReporter().report(new TypecheckingError(TypecheckingError.Kind.BODY_REQUIRED, sourceNode));
        return;
      }
    }

    for (List<Pattern> patterns : missingClauses) {
      Collections.reverse(patterns);
    }

    if (!elimParams.isEmpty()) {
      for (List<Pattern> patterns : missingClauses) {
        for (int i = 0; i < patterns.size(); i++) {
          if (patterns.get(i) instanceof BindingPattern) {
            ConstructorPattern newPattern;
            List<Pattern> args;
            Expression type = ((BindingPattern) patterns.get(i)).getBinding().getTypeExpr().getUnderlyingExpression();
            if (type instanceof SigmaExpression) {
              args = new ArrayList<>();
              newPattern = new ConstructorPattern((SigmaExpression) type, new Patterns(args));
            } else if (type instanceof ClassCallExpression) {
              args = new ArrayList<>();
              newPattern = new ConstructorPattern((ClassCallExpression) type, new Patterns(args));
            } else {
              continue;
            }

            patterns.set(i, newPattern);
            for (DependentLink link = newPattern.getParameters(); link.hasNext(); link = link.getNext()) {
              args.add(new BindingPattern(link));
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

    List<List<Pattern>> missingClauses = new ArrayList<>(myMissingClauses.size());
    loop:
    for (Pair<List<Util.ClauseElem>, Boolean> missingClause : myMissingClauses) {
      List<Pattern> patterns = Util.unflattenClauses(missingClause.proj1);
      List<Expression> expressions = new ArrayList<>(patterns.size());
      for (Pattern pattern : patterns) {
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

  private static class ExtClause extends Clause {
    final ExprSubstitution substitution;

    ExtClause(List<Pattern> patterns, Expression expression, ExprSubstitution substitution, Concrete.FunctionClause clause) {
      super(patterns, expression, clause);
      this.substitution = substitution;
    }
  }

  private List<Pair<Expression, Expression>> clausesToIntervalElim(List<ExtClause> clauseDataList, DependentLink parameters) {
    List<Pair<Expression, Expression>> result = new ArrayList<>(clauseDataList.get(0).patterns.size());
    for (int i = 0; i < clauseDataList.get(0).patterns.size(); i++) {
      Expression left = null;
      Expression right = null;

      for (ExtClause clauseData : clauseDataList) {
        if (clauseData.patterns.get(i) instanceof ConstructorPattern) {
          boolean found = false;
          Definition definition = ((ConstructorPattern) clauseData.patterns.get(i)).getDefinition();
          if (definition == Prelude.LEFT) {
            if (left == null) {
              found = true;
            }
          } else if (definition == Prelude.RIGHT) {
            if (right == null) {
              found = true;
            }
          } else {
            throw new IllegalStateException();
          }

          if (found) {
            myUnusedClauses.remove(clauseData.clause);

            ExprSubstitution substitution = new ExprSubstitution();
            DependentLink oldLink = new Patterns(clauseData.patterns).getFirstBinding();
            DependentLink newLink = parameters;
            for (int j = 0; newLink.hasNext(); j++, newLink = newLink.getNext()) {
              if (j == i) {
                continue;
              }
              substitution.add(oldLink, new ReferenceExpression(newLink));
              oldLink = oldLink.getNext();
            }

            if (definition == Prelude.LEFT) {
              left = clauseData.expression.subst(substitution);
            } else {
              right = clauseData.expression.subst(substitution);
            }
          }

          if (left != null && right != null) {
            break;
          }
        }
      }

      if (left != null && right == null || left == null && right != null) {
        List<Util.ClauseElem> missingClause = new ArrayList<>(clauseDataList.get(0).patterns.size());
        int j = 0;
        for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), j++) {
          missingClause.add(new Util.PatternClauseElem(j == i
            ? new ConstructorPattern(left == null ? Left() : Right(), new Patterns(Collections.emptyList()))
            : new BindingPattern(link)));
        }
        addMissingClause(missingClause, true);
      }

      result.add(new Pair<>(left, right));
    }
    return result;
  }

  private ElimTree clausesToElimTree(List<ExtClause> clauseDataList, int numberOfIntervals) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = 0;
      loop:
      for (; index < clauseDataList.get(0).patterns.size(); index++) {
        for (ExtClause clauseData : clauseDataList) {
          if (!(clauseData.patterns.get(index) instanceof BindingPattern)) {
            if (clauseDataList.get(0).patterns.get(index) instanceof BindingPattern && clauseData.patterns.get(index) instanceof ConstructorPattern) {
              Definition definition = ((ConstructorPattern) clauseData.patterns.get(index)).getDefinition();
              if (definition == Prelude.LEFT || definition == Prelude.RIGHT) {
                final int finalIndex = index;
                clauseDataList = clauseDataList.stream().filter(clauseData1 -> clauseData1.patterns.get(finalIndex) instanceof BindingPattern).collect(Collectors.toList());
                continue loop;
              }
            }
            break loop;
          }
        }
      }

      // If all patterns are variables
      if (index == clauseDataList.get(0).patterns.size()) {
        ExtClause clauseData = clauseDataList.get(0);
        myUnusedClauses.remove(clauseData.clause);
        DependentLink vars = clauseData.patterns.isEmpty() ? EmptyDependentLink.getInstance() : DependentLink.Helper.subst(((BindingPattern) clauseData.patterns.get(0)).getBinding(), clauseData.substitution, true);
        clauseData.substitution.subst(new ExprSubstitution(clauseData.substitution));
        return new LeafElimTree(vars, myCase ? SubstExpression.make(clauseData.expression, clauseData.substitution) : clauseData.expression.subst(clauseData.substitution));
      }

      // Make new list of variables
      DependentLink vars = index == 0 ? EmptyDependentLink.getInstance() : ((BindingPattern) clauseDataList.get(0).patterns.get(0)).getBinding().subst(new SubstVisitor(clauseDataList.get(0).substitution, LevelSubstitution.EMPTY), index, true);
      clauseDataList.get(0).substitution.subst(new ExprSubstitution(clauseDataList.get(0).substitution));
      for (DependentLink link = vars; link.hasNext(); link = link.getNext()) {
        myContext.push(new Util.PatternClauseElem(new BindingPattern(link)));
      }

      // Update substitution for each clause
      int j = 0;
      for (DependentLink link = vars; link.hasNext(); link = link.getNext(), j++) {
        Expression newRef = new ReferenceExpression(link);
        clauseDataList.get(0).substitution.remove(link);
        for (int i = 1; i < clauseDataList.size(); i++) {
          clauseDataList.get(i).substitution.addSubst(((BindingPattern) clauseDataList.get(i).patterns.get(j)).getBinding(), newRef);
        }
      }

      ExtClause conClauseData = null;
      for (ExtClause clauseData : clauseDataList) {
        Pattern pattern = clauseData.patterns.get(index);
        if (pattern instanceof EmptyPattern) {
          myUnusedClauses.remove(clauseData.clause);
          return new BranchElimTree(vars, Collections.emptyMap());
        }
        if (conClauseData == null && pattern instanceof ConstructorPattern) {
          conClauseData = clauseData;
        }
      }

      assert conClauseData != null;
      ConstructorPattern someConPattern = (ConstructorPattern) conClauseData.patterns.get(index);
      List<ConCallExpression> conCalls = null;
      List<Constructor> constructors;
      DataDefinition dataType;
      if (someConPattern.getDefinition() instanceof Constructor) {
        dataType = ((Constructor) someConPattern.getDefinition()).getDataType();
        if (dataType.hasIndexedConstructors()) {
          DataCallExpression dataCall = (DataCallExpression) new SubstVisitor(conClauseData.substitution, LevelSubstitution.EMPTY).visitConCall((ConCallExpression) someConPattern.getDataExpression(), null).accept(GetTypeVisitor.INSTANCE, null);
          conCalls = dataCall.getMatchedConstructors();
          if (conCalls == null) {
            myVisitor.getErrorReporter().report(new ImpossibleEliminationError(dataCall, conClauseData.clause));
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
        } else {
          constructors = Collections.singletonList(new TupleConstructor(someConPattern.getLength()));
        }
        dataType = null;
      }

      if (dataType == Prelude.INTERVAL) {
        myVisitor.getErrorReporter().report(new TypecheckingError("Pattern matching on the interval is not allowed here", conClauseData.clause));
        myOK = false;
        return null;
      }

      if (dataType != null && dataType.isSquashed()) {
        if (myActualLevel != null && !Level.compare(myActualLevel, dataType.getSort().getHLevel().add(myActualLevelSub), Equations.CMP.LE, myVisitor.getEquations(), conClauseData.clause)) {
          myVisitor.getErrorReporter().report(new SquashedDataError(dataType, myActualLevel, myActualLevelSub, conClauseData.clause));
        }

        boolean ok = !dataType.isTruncated() || myLevel != null && myLevel <= dataType.getSort().getHLevel().getConstant() + 1;
        if (!ok) {
          Expression type = myExpectedType.getType();
          if (type != null) {
            type = type.normalize(NormalizeVisitor.Mode.WHNF);
            UniverseExpression universe = type.cast(UniverseExpression.class);
            if (universe != null) {
              ok = Level.compare(universe.getSort().getHLevel(), dataType.getSort().getHLevel(), Equations.CMP.LE, myVisitor.getEquations(), conClauseData.clause);
            } else {
              InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, false, conClauseData.clause);
              myVisitor.getEquations().addVariable(pl);
              ok = type.isLessOrEquals(new UniverseExpression(new Sort(new Level(pl), dataType.getSort().getHLevel())), myVisitor.getEquations(), conClauseData.clause);
            }
          }
        }
        if (!ok) {
          myVisitor.getErrorReporter().report(new TruncatedDataError(dataType, myExpectedType, conClauseData.clause));
          myOK = false;
        }
      }

      if (myLevel != null && !constructors.isEmpty() && !(constructors.get(0) instanceof SingleConstructor)) {
        //noinspection ConstantConditions
        constructors.removeIf(constructor -> numberOfIntervals + (constructor.getBody() instanceof IntervalElim ? ((IntervalElim) constructor.getBody()).getNumberOfTotalElim() : 0) > myLevel);
      }

      boolean hasVars = false;
      Map<Constructor, List<ExtClause>> constructorMap = new LinkedHashMap<>();
      for (ExtClause clauseData : clauseDataList) {
        if (clauseData.patterns.get(index) instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clauseData);
          }
        } else {
          Definition def = ((ConstructorPattern) clauseData.patterns.get(index)).getDefinition();
          if (!(def instanceof Constructor) && !constructors.isEmpty() && constructors.get(0) instanceof SingleConstructor) {
            def = constructors.get(0);
          }
          if (def instanceof Constructor) {
            constructorMap.computeIfAbsent((Constructor) def, k -> new ArrayList<>()).add(clauseData);
          }
        }
      }

      if (myFlags.contains(PatternTypechecking.Flag.CHECK_COVERAGE) && !hasVars) {
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

      Map<Constructor, ElimTree> children = new HashMap<>();
      for (Constructor constructor : constructors) {
        List<ExtClause> conClauseDataList = constructorMap.get(constructor);
        if (conClauseDataList == null) {
          continue;
        }
        myContext.push(Util.makeDataClauseElem(constructor, someConPattern));

        for (int i = 0; i < conClauseDataList.size(); i++) {
          List<Pattern> patterns = new ArrayList<>();
          List<Pattern> oldPatterns = conClauseDataList.get(i).patterns;
          ExprSubstitution newSubstitution;
          if (oldPatterns.get(index) instanceof ConstructorPattern) {
            patterns.addAll(((ConstructorPattern) oldPatterns.get(index)).getArguments());
            newSubstitution = conClauseDataList.get(i).substitution;
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
              conParameters = constructor.getParameters();
              List<Expression> dataTypesArgs = conCall.getDataTypeArguments();
              substExpr = ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), dataTypesArgs, arguments);
              conParameters = DependentLink.Helper.subst(conParameters, DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), dataTypesArgs));
            } else {
              if (constructor instanceof SingleConstructor) {
                conParameters = someConPattern.getParameters();
                if (someConPattern.getDefinition() instanceof ClassDefinition) {
                  Map<ClassField, Expression> implementations = new HashMap<>();
                  ClassDefinition classDef = (ClassDefinition) someConPattern.getDefinition();
                  DependentLink link = conParameters;
                  for (ClassField field : classDef.getFields()) {
                    if (!classDef.isImplemented(field)) {
                      implementations.put(field, new ReferenceExpression(link));
                      link = link.getNext();
                    }
                  }
                  substExpr = new NewExpression(null, new ClassCallExpression(classDef, someConPattern.getSortArgument(), implementations, Sort.PROP, false));
                } else {
                  substExpr = new TupleExpression(arguments, (SigmaExpression) someConPattern.getDataExpression());
                  conParameters = DependentLink.Helper.copy(conParameters);
                }
              } else {
                conParameters = constructor.getParameters();
                List<Expression> dataTypesArgs = new ArrayList<>(someConPattern.getDataTypeArguments().size());
                for (Expression dataTypeArg : someConPattern.getDataTypeArguments()) {
                  dataTypesArgs.add(dataTypeArg.subst(conClauseData.substitution));
                }
                substExpr = ConCallExpression.make(constructor, someConPattern.getSortArgument(), dataTypesArgs, arguments);
                conParameters = DependentLink.Helper.subst(conParameters, DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), dataTypesArgs));
              }
            }

            for (DependentLink link = conParameters; link.hasNext(); link = link.getNext()) {
              patterns.add(new BindingPattern(link));
              arguments.add(new ReferenceExpression(link));
            }

            newSubstitution = new ExprSubstitution(conClauseDataList.get(i).substitution);
            newSubstitution.addSubst(((BindingPattern) oldPatterns.get(index)).getBinding(), substExpr);
          }
          List<Pattern> rest = oldPatterns.subList(index + 1, oldPatterns.size());
          DependentLink last = new Patterns(patterns).getLastBinding();
          DependentLink first = new Patterns(rest).getFirstBinding();
          if (last.hasNext() && first.hasNext()) {
            last.setNext(first);
          }
          patterns.addAll(rest);
          conClauseDataList.set(i, new ExtClause(patterns, conClauseDataList.get(i).expression, newSubstitution, conClauseDataList.get(i).clause));
        }

        ElimTree elimTree = clausesToElimTree(conClauseDataList, myLevel == null ? 0 : numberOfIntervals + (constructor.getBody() instanceof IntervalElim ? ((IntervalElim) constructor.getBody()).getNumberOfTotalElim() : 0));
        if (elimTree == null) {
          myOK = false;
        } else {
          children.put(constructor, elimTree);
        }

        myContext.pop();
      }

      return new BranchElimTree(vars, children);
    }
  }

  private void addMissingClause(List<Util.ClauseElem> clause, boolean isInterval) {
    if (myMissingClauses == null) {
      myMissingClauses = new ArrayList<>();
    }
    myMissingClauses.add(new Pair<>(clause, isInterval));
  }
}
