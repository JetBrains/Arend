package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.GetTypeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.MissingClausesError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TruncatedDataError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ElimTypechecking<T> {
  private final CheckTypeVisitor<T> myVisitor;
  private Set<Concrete.FunctionClause<T>> myUnusedClauses;
  private final EnumSet<PatternTypechecking.Flag> myFlags;
  private final Expression myExpectedType;
  private boolean myOK;
  private Stack<Util.ClauseElem> myContext;

  private static final int MISSING_CLAUSES_LIST_SIZE = 10;
  private List<Pair<List<Util.ClauseElem>, Boolean>> myMissingClauses;

  public ElimTypechecking(CheckTypeVisitor<T> visitor, Expression expectedType, EnumSet<PatternTypechecking.Flag> flags) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myFlags = flags;
  }

  public static <T> List<DependentLink> getEliminatedParameters(List<? extends Concrete.ReferenceExpression<T>> expressions, List<? extends Concrete.Clause<T>> clauses, DependentLink parameters, CheckTypeVisitor<T> visitor) {
    List<DependentLink> elimParams = Collections.emptyList();
    if (!expressions.isEmpty()) {
      int expectedNumberOfPatterns = expressions.size();
      for (Concrete.Clause<T> clause : clauses) {
        if (clause.getPatterns() != null && clause.getPatterns().size() != expectedNumberOfPatterns) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError<>("Expected " + expectedNumberOfPatterns + " patterns, but got " + clause.getPatterns().size(), clause));
          return null;
        }
      }

      DependentLink link = parameters;
      elimParams = new ArrayList<>(expressions.size());
      for (Concrete.ReferenceExpression<T> expr : expressions) {
        DependentLink elimParam = (DependentLink) visitor.getContext().get(expr.getReferent());
        while (elimParam != link) {
          if (!link.hasNext()) {
            link = parameters;
            while (link.hasNext() && link != elimParam) {
              link = link.getNext();
            }
            visitor.getErrorReporter().report(new LocalTypeCheckingError<>(link == elimParam ? "Variable elimination must be in the order of variable introduction" : "Only parameters can be eliminated", expr));
            return null;
          }
          link = link.getNext();
        }
        elimParams.add(elimParam);
      }
    }
    return elimParams;
  }

  public ElimTree typecheckElim(List<? extends Concrete.FunctionClause<T>> funClauses, Concrete.SourceNode<T> sourceNode, DependentLink parameters, List<Clause<T>> resultClauses) {
    assert !myFlags.contains(PatternTypechecking.Flag.ALLOW_INTERVAL);
    return (ElimTree) typecheckElim(funClauses, sourceNode, null, parameters, Collections.emptyList(), resultClauses);
  }

  public Body typecheckElim(List<? extends Concrete.FunctionClause<T>> funClauses, Concrete.SourceNode<T> sourceNode, List<? extends Concrete.Parameter<T>> abstractParameters, DependentLink parameters, List<DependentLink> elimParams, List<Clause<T>> resultClauses) {
    List<ExtClause<T>> clauses = new ArrayList<>(funClauses.size());
    PatternTypechecking<T> patternTypechecking = new PatternTypechecking<>(myVisitor.getErrorReporter(), myFlags);
    myOK = true;
    for (Concrete.FunctionClause<T> clause : funClauses) {
      Pair<List<Pattern>, CheckTypeVisitor.Result> result = patternTypechecking.typecheckClause(clause, abstractParameters, parameters, elimParams, myExpectedType, myVisitor);
      if (result == null) {
        myOK = false;
      } else {
        clauses.add(new ExtClause<>(result.proj1, result.proj2 == null ? null : result.proj2.expression, new ExprSubstitution(), clause));
      }
    }
    if (!myOK) {
      return null;
    }

    myUnusedClauses = new HashSet<>(funClauses);

    List<ExtClause<T>> intervalClauses = Collections.emptyList();
    List<ExtClause<T>> nonIntervalClauses = clauses;
    if (myFlags.contains(PatternTypechecking.Flag.ALLOW_INTERVAL)) {
      intervalClauses = new ArrayList<>();
      nonIntervalClauses = new ArrayList<>();
      for (ExtClause<T> clause : clauses) {
        boolean hasNonIntervals = false;
        int intervals = 0;
        for (Pattern pattern : clause.patterns) {
          if (pattern instanceof BindingPattern) {
            continue;
          }
          if (pattern instanceof ConstructorPattern) {
            Constructor constructor = ((ConstructorPattern) pattern).getConstructor();
            if (constructor == Prelude.LEFT || constructor == Prelude.RIGHT) {
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
            resultClauses.add(new Clause<>(clause.patterns, clause.expression, clause.clause));
          }
        } else {
          if (intervals > 1) {
            myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Only a single interval pattern per row is allowed", clause.clause));
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

      for (ExtClause<T> clause : nonIntervalClauses) {
        for (int j = i; j < clause.patterns.size(); j++) {
          if (!(clause.patterns.get(j) instanceof BindingPattern)) {
            myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("A pattern matching on a data type is allowed only before the pattern matching on the interval", clause.clause));
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
          DataCallExpression dataCall = link.getTypeExpr().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class);
          if (dataCall != null) {
            List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
            if (conCalls != null && conCalls.isEmpty()) {
              emptyLink = link;
              break;
            }
          }
        }
      } else {
        for (DependentLink link : elimParams) {
          DataCallExpression dataCall = link.getTypeExpr().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class);
          if (dataCall != null) {
            List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
            if (conCalls != null && conCalls.isEmpty()) {
              emptyLink = link;
              break;
            }
          }
        }
      }

      if (emptyLink == null && myFlags.contains(PatternTypechecking.Flag.CHECK_COVERAGE)) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Coverage check failed", sourceNode));
      }

      ElimTree elimTree = null;
      if (emptyLink != null) {
        int index = 0;
        for (DependentLink link = parameters; link != emptyLink; link = link.getNext()) {
          index++;
        }
        elimTree = new BranchElimTree(parameters.subst(new ExprSubstitution(), LevelSubstitution.EMPTY, index), Collections.emptyMap());
      }

      return cases == null ? elimTree : new IntervalElim(parameters, cases, elimTree);
    }

    myContext = new Stack<>();
    ElimTree elimTree = clausesToElimTree(nonIntervalClauses);

    if (myMissingClauses != null && !myMissingClauses.isEmpty()) {
      List<List<Expression>> missingClauses = new ArrayList<>(myMissingClauses.size());
      loop:
      for (Pair<List<Util.ClauseElem>, Boolean> missingClause : myMissingClauses) {
        List<Expression> expressions = Util.unflattenClauses(missingClause.proj1);
        if (!missingClause.proj2) {
          if (elimTree != null && NormalizeVisitor.INSTANCE.doesEvaluate(elimTree, expressions)) {
            continue;
          }

          Util.addArguments(expressions, parameters);

          int i = expressions.size() - 1;
          for (; i >= 0; i--) {
            if (!(expressions.get(i) instanceof ReferenceExpression)) {
              break;
            }
          }
          DependentLink link = parameters;
          ExprSubstitution substitution = new ExprSubstitution();
          for (int j = 0; j < i + 1; j++) {
            substitution.add(link, expressions.get(j));
            link = link.getNext();
          }
          for (; link.hasNext(); link = link.getNext()) {
            link = link.getNextTyped(null);
            DataCallExpression dataCall = link.getTypeExpr().subst(substitution).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class);
            if (dataCall != null) {
              List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
              if (conCalls != null && conCalls.isEmpty()) {
                continue loop;
              }
            }
          }

          myOK = false;
        }

        Util.removeArguments(expressions, parameters, elimParams);
        missingClauses.add(expressions);
      }

      if (!missingClauses.isEmpty()) {
        myVisitor.getErrorReporter().report(new MissingClausesError<>(missingClauses, sourceNode));
      }
    }

    if (myOK) {
      for (Concrete.FunctionClause<T> clause : myUnusedClauses) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>(Error.Level.WARNING, "This clause is redundant", clause));
      }
    }
    return cases == null ? elimTree : new IntervalElim(parameters, cases, elimTree);
  }

  private static class ExtClause<T> extends Clause<T> {
    final ExprSubstitution substitution;

    ExtClause(List<Pattern> patterns, Expression expression, ExprSubstitution substitution, Concrete.FunctionClause<T> clause) {
      super(patterns, expression, clause);
      this.substitution = substitution;
    }
  }

  private List<Pair<Expression, Expression>> clausesToIntervalElim(List<ExtClause<T>> clauseDataList, DependentLink parameters) {
    List<Pair<Expression, Expression>> result = new ArrayList<>(clauseDataList.get(0).patterns.size());
    for (int i = 0; i < clauseDataList.get(0).patterns.size(); i++) {
      Expression left = null;
      Expression right = null;

      for (ExtClause clauseData : clauseDataList) {
        if (clauseData.patterns.get(i) instanceof ConstructorPattern) {
          boolean found = false;
          Constructor constructor = ((ConstructorPattern) clauseData.patterns.get(i)).getConstructor();
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

            if (constructor == Prelude.LEFT) {
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
            ? new ConstructorPattern(new ConCallExpression(left == null ? Prelude.LEFT : Prelude.RIGHT, Sort.STD, Collections.emptyList(), Collections.emptyList()), new Patterns(Collections.emptyList()))
            : new BindingPattern(link)));
        }
        addMissingClause(missingClause, true);
      }

      result.add(new Pair<>(left, right));
    }
    return result;
  }

  private ElimTree clausesToElimTree(List<ExtClause<T>> clauseDataList) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = 0;
      loop:
      for (; index < clauseDataList.get(0).patterns.size(); index++) {
        for (ExtClause clauseData : clauseDataList) {
          if (!(clauseData.patterns.get(index) instanceof BindingPattern)) {
            if (clauseDataList.get(0).patterns.get(index) instanceof BindingPattern && clauseData.patterns.get(index) instanceof ConstructorPattern) {
              Constructor constructor = ((ConstructorPattern) clauseData.patterns.get(index)).getConstructor();
              if (constructor == Prelude.LEFT || constructor == Prelude.RIGHT) {
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
        DependentLink vars = clauseData.patterns.isEmpty() ? EmptyDependentLink.getInstance() : DependentLink.Helper.subst(((BindingPattern) clauseData.patterns.get(0)).getBinding(), clauseData.substitution);
        clauseData.substitution.subst(clauseData.substitution);
        return new LeafElimTree(vars, clauseData.expression.subst(clauseData.substitution));
      }

      // Make new list of variables
      DependentLink vars = index == 0 ? EmptyDependentLink.getInstance() : ((BindingPattern) clauseDataList.get(0).patterns.get(0)).getBinding().subst(clauseDataList.get(0).substitution, LevelSubstitution.EMPTY, index);
      clauseDataList.get(0).substitution.subst(clauseDataList.get(0).substitution);
      for (DependentLink link = vars; link.hasNext(); link = link.getNext()) {
        myContext.push(new Util.PatternClauseElem(new BindingPattern(link)));
      }

      // Update substitution for each clause
      int j = 0;
      for (DependentLink link = vars; link.hasNext(); link = link.getNext(), j++) {
        Expression newRef = new ReferenceExpression(link);
        clauseDataList.get(0).substitution.remove(link);
        for (int i = 1; i < clauseDataList.size(); i++) {
          clauseDataList.get(i).substitution.add(((BindingPattern) clauseDataList.get(i).patterns.get(j)).getBinding(), newRef);
        }
      }

      ExtClause<T> conClauseData = null;
      for (ExtClause<T> clauseData : clauseDataList) {
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
      if (someConPattern.getConstructor().getDataType().hasIndexedConstructors()) {
        conCalls = GetTypeVisitor.INSTANCE.visitConCall(new SubstVisitor(conClauseData.substitution, LevelSubstitution.EMPTY).visitConCall(someConPattern.getConCall(), null), null).getMatchedConstructors();
        if (conCalls == null) {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Elimination is not possible here, cannot determine the set of eligible constructors", conClauseData.clause));
          myOK = false;
          return null;
        }
        constructors = new ArrayList<>(conCalls.size());
        for (ConCallExpression conCall : conCalls) {
          constructors.add(conCall.getDefinition());
        }
      } else {
        constructors = someConPattern.getConstructor().getDataType().getConstructors();
      }

      DataDefinition dataType = someConPattern.getConstructor().getDataType();
      if (dataType == Prelude.INTERVAL) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Pattern matching on the interval is not allowed here", conClauseData.clause));
        myOK = false;
        return null;
      }

      if (someConPattern.getConstructor().getDataType().isTruncated()) {
        if (!myExpectedType.getType().isLessOrEquals(new UniverseExpression(dataType.getSort()), myVisitor.getEquations(), conClauseData.clause)) {
          LocalTypeCheckingError<T> error = new TruncatedDataError<>(dataType, myExpectedType, conClauseData.clause);
          myVisitor.getErrorReporter().report(error);
          myOK = false;
        }
      }

      boolean hasVars = false;
      Map<Constructor, List<ExtClause<T>>> constructorMap = new LinkedHashMap<>();
      for (ExtClause<T> clauseData : clauseDataList) {
        if (clauseData.patterns.get(index) instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clauseData);
          }
        } else {
          constructorMap.computeIfAbsent(((ConstructorPattern) clauseData.patterns.get(index)).getConstructor(), k -> new ArrayList<>()).add(clauseData);
        }
      }

      if (myFlags.contains(PatternTypechecking.Flag.CHECK_COVERAGE) && !hasVars && constructors.size() > constructorMap.size()) {
        for (Constructor constructor : constructors) {
          if (!constructorMap.containsKey(constructor)) {
            if (constructor == Prelude.PROP_TRUNC_PATH_CON) {
              Sort sort = myExpectedType.getType().toSort();
              if (sort != null && sort.isProp()) {
                continue;
              }
            } else if (constructor == Prelude.SET_TRUNC_PATH_CON) {
              Sort sort = myExpectedType.getType().toSort();
              if (sort != null && sort.isSet()) {
                continue;
              }
            }

            try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
              myContext.push(new Util.ConstructorClauseElem(constructor));
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
        List<ExtClause<T>> conClauseDataList = constructorMap.get(constructor);
        if (conClauseDataList == null) {
          continue;
        }
        myContext.push(new Util.ConstructorClauseElem(constructor));

        for (int i = 0; i < conClauseDataList.size(); i++) {
          List<Pattern> patterns = new ArrayList<>();
          List<Pattern> oldPatterns = conClauseDataList.get(i).patterns;
          if (oldPatterns.get(index) instanceof ConstructorPattern) {
            patterns.addAll(((ConstructorPattern) oldPatterns.get(index)).getArguments());
          } else {
            Expression substExpr;
            List<Expression> dataTypesArgs;
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
              dataTypesArgs = conCall.getDataTypeArguments();
              substExpr = new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), dataTypesArgs, arguments);
            } else {
              dataTypesArgs = new ArrayList<>(someConPattern.getDataTypeArguments().size());
              for (Expression dataTypeArg : someConPattern.getDataTypeArguments()) {
                dataTypesArgs.add(dataTypeArg.subst(conClauseData.substitution));
              }
              substExpr = new ConCallExpression(constructor, someConPattern.getSortArgument(), dataTypesArgs, arguments);
            }

            DependentLink conParameters = DependentLink.Helper.subst(constructor.getParameters(), DependentLink.Helper.toSubstitution(constructor.getDataTypeParameters(), dataTypesArgs));
            for (DependentLink link = conParameters; link.hasNext(); link = link.getNext()) {
              patterns.add(new BindingPattern(link));
              arguments.add(new ReferenceExpression(link));
            }

            conClauseDataList.get(i).substitution.add(((BindingPattern) oldPatterns.get(index)).getBinding(), substExpr);
          }
          patterns.addAll(oldPatterns.subList(index + 1, oldPatterns.size()));
          conClauseDataList.set(i, new ExtClause<>(patterns, conClauseDataList.get(i).expression, conClauseDataList.get(i).substitution, conClauseDataList.get(i).clause));
        }

        ElimTree elimTree = clausesToElimTree(conClauseDataList);
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
      myMissingClauses = new ArrayList<>(MISSING_CLAUSES_LIST_SIZE);
    }
    if (myMissingClauses.size() == MISSING_CLAUSES_LIST_SIZE) {
      myMissingClauses.set(MISSING_CLAUSES_LIST_SIZE - 1, null);
    } else {
      myMissingClauses.add(new Pair<>(clause, isInterval));
    }
  }
}
