package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.GetTypeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.MissingClausesError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ElimTypechecking {
  private final CheckTypeVisitor myVisitor;
  private Set<Abstract.FunctionClause> myUnusedClauses;
  private final boolean myAllowInterval;
  private Expression myExpectedType;
  private boolean myOK;
  private Stack<Util.ClauseElem> myContext;

  private static final int MISSING_CLAUSES_LIST_SIZE = 10;
  private List<List<Util.ClauseElem>> myMissingClauses;

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, boolean allowInterval) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myAllowInterval = allowInterval;
  }

  public static List<DependentLink> getEliminatedParameters(List<? extends Abstract.ReferenceExpression> expressions, List<? extends Abstract.Clause> clauses, DependentLink parameters, CheckTypeVisitor visitor) {
    List<DependentLink> elimParams = Collections.emptyList();
    if (!expressions.isEmpty()) {
      int expectedNumberOfPatterns = expressions.size();
      for (Abstract.Clause clause : clauses) {
        if (clause.getPatterns() != null && clause.getPatterns().size() != expectedNumberOfPatterns) {
          visitor.getErrorReporter().report(new LocalTypeCheckingError("Expected " + expectedNumberOfPatterns + " patterns, but got " + clause.getPatterns().size(), clause));
          return null;
        }
      }

      DependentLink link = parameters;
      elimParams = new ArrayList<>(expressions.size());
      for (Abstract.ReferenceExpression expr : expressions) {
        DependentLink elimParam = (DependentLink) visitor.getContext().get(expr.getReferent());
        while (elimParam != link) {
          if (!link.hasNext()) {
            link = parameters;
            while (link.hasNext() && link != elimParam) {
              link = link.getNext();
            }
            visitor.getErrorReporter().report(new LocalTypeCheckingError(link == elimParam ? "Variable elimination must be in the order of variable introduction" : "Only parameters can be eliminated", expr));
            return null;
          }
          link = link.getNext();
        }
        elimParams.add(elimParam);
      }
    }
    return elimParams;
  }

  public ElimTree typecheckElim(Abstract.ElimBody body, List<? extends Abstract.Argument> abstractParameters, DependentLink parameters) {
    List<DependentLink> elimParams = getEliminatedParameters(body.getEliminatedReferences(), body.getClauses(), parameters, myVisitor);
    if (elimParams == null) {
      return null;
    }

    List<ClauseData> clauses = new ArrayList<>(body.getClauses().size());
    PatternTypechecking patternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), myAllowInterval);
    myOK = true;
    for (Abstract.FunctionClause clause : body.getClauses()) {
      Pair<List<Pattern>, CheckTypeVisitor.Result> result = patternTypechecking.typecheckClause(clause, abstractParameters, parameters, elimParams, myExpectedType, myVisitor);
      if (result == null) {
        myOK = false;
      } else {
        clauses.add(new ClauseData(result.proj1, result.proj2 == null ? null : result.proj2.expression, new ExprSubstitution(), clause));
      }
    }
    if (!myOK) {
      return null;
    }

    if (clauses.isEmpty()) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a clause list", body));
      return null;
    }

    myUnusedClauses = new HashSet<>(body.getClauses());
    myContext = new Stack<>();
    ElimTree elimTree = clausesToElimTree(clauses);

    if (myMissingClauses != null && !myMissingClauses.isEmpty()) {
      final List<DependentLink> finalElimParams = elimParams;
      myVisitor.getErrorReporter().report(new MissingClausesError(myMissingClauses.stream().map(missingClause -> Util.unflattenClauses(missingClause, parameters, finalElimParams)).collect(Collectors.toList()), body));
    }
    if (!myOK) {
      return null;
    }
    for (Abstract.FunctionClause clause : myUnusedClauses) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This clause is redundant", clause));
    }
    return elimTree;
  }

  private class ClauseData {
    List<Pattern> patterns;
    Expression expression;
    ExprSubstitution substitution;
    Abstract.FunctionClause clause;

    ClauseData(List<Pattern> patterns, Expression expression, ExprSubstitution substitution, Abstract.FunctionClause clause) {
      this.patterns = patterns;
      this.expression = expression;
      this.substitution = substitution;
      this.clause = clause;
    }
  }

  private ElimTree clausesToElimTree(List<ClauseData> clauseDataList) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = 0;
      loop:
      for (; index < clauseDataList.get(0).patterns.size(); index++) {
        for (ClauseData clauseData : clauseDataList) {
          if (!(clauseData.patterns.get(index) instanceof BindingPattern)) {
            break loop;
          }
        }
      }

      // If all patterns are variables
      if (index == clauseDataList.get(0).patterns.size()) {
        ClauseData clauseData = clauseDataList.get(0);
        myUnusedClauses.remove(clauseData.clause);
        return new LeafElimTree(clauseData.patterns.isEmpty() ? EmptyDependentLink.getInstance() : ((BindingPattern) clauseData.patterns.get(0)).getBinding(), clauseData.expression.subst(clauseData.substitution));
      }

      // Make new list of variables
      DependentLink vars = index == 0 ? EmptyDependentLink.getInstance() : ((BindingPattern) clauseDataList.get(0).patterns.get(0)).getBinding().subst(clauseDataList.get(0).substitution, LevelSubstitution.EMPTY, index);
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

      ClauseData conClauseData = null;
      for (ClauseData clauseData : clauseDataList) {
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
        DataCallExpression dataCall = new GetTypeVisitor().visitConCall(new SubstVisitor(conClauseData.substitution, LevelSubstitution.EMPTY).visitConCall(someConPattern.toExpression(), null), null);
        conCalls = dataCall.getMatchedConstructors();
        if (conCalls == null) {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", conClauseData.clause));
          return null;
        }
        constructors = conCalls.stream().map(ConCallExpression::getDefinition).collect(Collectors.toList());
      } else {
        constructors = someConPattern.getConstructor().getDataType().getConstructors();
      }

      DataDefinition dataType = someConPattern.getConstructor().getDataType();
      if (someConPattern.getConstructor().getDataType().isTruncated()) {
        if (!myExpectedType.getType().isLessOrEquals(new UniverseExpression(dataType.getSort()), myVisitor.getEquations(), conClauseData.clause)) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Data " + dataType.getName() + " is truncated to the universe "
            + dataType.getSort() + " which does not fit in the universe of " +
            myExpectedType + " - the type of eliminator", conClauseData.clause);
          myVisitor.getErrorReporter().report(error);
        }
      }

      boolean hasVars = false;
      Map<Constructor, List<ClauseData>> constructorMap = new LinkedHashMap<>();
      for (ClauseData clauseData : clauseDataList) {
        if (clauseData.patterns.get(index) instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clauseData);
          }
        } else {
          constructorMap.computeIfAbsent(((ConstructorPattern) clauseData.patterns.get(index)).getConstructor(), k -> new ArrayList<>()).add(clauseData);
        }
      }

      if (!hasVars && constructors.size() > constructorMap.size()) {
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

            myContext.push(new Util.ConstructorClauseElem(constructor));
            addMissingClause(new ArrayList<>(myContext));
            myContext.pop();
          }
        }
      }

      Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
      for (Constructor constructor : constructors) {
        List<ClauseData> conClauseDataList = constructorMap.get(constructor);
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
            DependentLink conParameters = DependentLink.Helper.subst(constructor.getParameters(), new ExprSubstitution());
            for (DependentLink link = conParameters; link.hasNext(); link = link.getNext()) {
              patterns.add(new BindingPattern(link));
            }

            Expression substExpr;
            List<Expression> arguments = new ArrayList<>(patterns.size());
            for (DependentLink link = conParameters; link.hasNext(); link = link.getNext()) {
              arguments.add(new ReferenceExpression(link));
            }
            if (conCalls != null) {
              ConCallExpression conCall = null;
              for (ConCallExpression conCall1 : conCalls) {
                if (conCall1.getDefinition() == constructor) {
                  conCall = conCall1;
                  break;
                }
              }
              assert conCall != null;
              substExpr = new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
            } else {
              substExpr = new ConCallExpression(constructor, someConPattern.getSortArgument(), someConPattern.getDataTypeArguments(), arguments);
            }
            conClauseDataList.get(i).substitution.add(((BindingPattern) oldPatterns.get(index)).getBinding(), substExpr);
          }
          patterns.addAll(oldPatterns.subList(index + 1, oldPatterns.size()));
          conClauseDataList.set(i, new ClauseData(patterns, conClauseDataList.get(i).expression, conClauseDataList.get(i).substitution, conClauseDataList.get(i).clause));
        }

        ElimTree elimTree = clausesToElimTree(conClauseDataList);
        if (elimTree == null) {
          myOK = false;
        } else {
          children.put(constructor, elimTree);
        }

        myContext.pop();
      }

      if (hasVars && constructors.size() > constructorMap.size()) {
        List<ClauseData> varClauseDataList = new ArrayList<>();
        for (ClauseData clauseData : clauseDataList) {
          if (clauseData.patterns.get(index) instanceof BindingPattern) {
            varClauseDataList.add(clauseData);
            clauseData.substitution.remove(((BindingPattern) clauseData.patterns.get(index)).getBinding());
          }
        }

        myContext.push(new Util.PatternClauseElem(varClauseDataList.get(0).patterns.get(index)));
        ElimTree elimTree = clausesToElimTree(varClauseDataList);
        if (elimTree == null) {
          myOK = false;
        } else {
          children.put(BranchElimTree.Pattern.ANY, elimTree);
        }
        myContext.pop();
      }

      return new BranchElimTree(vars, children);
    }
  }

  private void addMissingClause(List<Util.ClauseElem> clause) {
    myOK = false;
    if (myMissingClauses == null) {
      myMissingClauses = new ArrayList<>(MISSING_CLAUSES_LIST_SIZE);
    }
    if (myMissingClauses.size() == MISSING_CLAUSES_LIST_SIZE) {
      myMissingClauses.set(MISSING_CLAUSES_LIST_SIZE - 1, null);
    } else {
      myMissingClauses.add(clause);
    }
  }
}
