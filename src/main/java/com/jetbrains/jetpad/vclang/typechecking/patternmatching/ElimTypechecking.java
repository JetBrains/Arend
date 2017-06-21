package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.GetTypeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
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
  private Set<Abstract.Clause> myUnusedClauses;
  private final boolean myAllowInterval;
  private Expression myExpectedType;
  private boolean myOK;
  private Stack<MissingClausesError.ClauseElem> myContext;

  private static final int MISSING_CLAUSES_LIST_SIZE = 10;
  private List<List<MissingClausesError.ClauseElem>> myMissingClauses;

  public ElimTypechecking(CheckTypeVisitor visitor, Expression expectedType, boolean allowInterval) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myAllowInterval = allowInterval;
  }

  public ElimTree typecheckElim(Abstract.ElimExpression elimExpr, DependentLink patternTypes) {
    List<DependentLink> elimParams = null;
    if (elimExpr.getExpressions() != null) {
      DependentLink link = patternTypes;
      elimParams = new ArrayList<>(elimExpr.getExpressions().size());
      for (Abstract.Expression expr : elimExpr.getExpressions()) {
        if (expr instanceof Abstract.ReferenceExpression) {
          DependentLink elimParam = (DependentLink) myVisitor.getContext().remove(((Abstract.ReferenceExpression) expr).getReferent());
          while (elimParam != link) {
            if (!link.hasNext()) {
              myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Variable elimination must be in the order of variable introduction", expr));
              return null;
            }
            link = link.getNext();
          }
          elimParams.add(elimParam);
        } else {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError("\\elim can be applied only to a local variable", expr));
          return null;
        }
      }
    }

    List<ClauseData> clauses = new ArrayList<>(elimExpr.getClauses().size());
    PatternTypechecking patternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), myAllowInterval);
    myOK = true;
    for (Abstract.Clause clause : elimExpr.getClauses()) {
      Map<Abstract.ReferableSourceNode, Binding> originalContext = new HashMap<>(myVisitor.getContext());
      Pair<List<Pattern>, CheckTypeVisitor.Result> result = patternTypechecking.typecheckClause(clause, patternTypes, elimParams, myExpectedType, myVisitor, true);
      myVisitor.setContext(originalContext);

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
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a pattern list", elimExpr));
      return null;
    }

    myUnusedClauses = new HashSet<>(elimExpr.getClauses());
    myContext = new Stack<>();
    ElimTree elimTree = clausesToElimTree(clauses);

    if (myMissingClauses != null && !myMissingClauses.isEmpty()) {
      if (elimParams != null) {
        for (List<MissingClausesError.ClauseElem> missingClause : myMissingClauses) {
          DependentLink link = patternTypes;
          loop:
          for (int i = 0; i < elimParams.size(); i++) {
            while (link != elimParams.get(i)) {
              if (i >= missingClause.size() || missingClause.get(i) == null || missingClause.get(i) instanceof MissingClausesError.ConstructorClauseElem) {
                break loop;
              }
              missingClause.remove(i);
              link = link.getNext();
            }
          }
        }
      }
      myVisitor.getErrorReporter().report(new MissingClausesError(myMissingClauses, elimExpr));
    }
    if (!myOK) {
      return null;
    }
    for (Abstract.Clause clause : myUnusedClauses) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This clause is redundant", clause));
    }
    return elimTree;
  }

  private class ClauseData {
    List<Pattern> patterns;
    Expression expression;
    ExprSubstitution substitution;
    Abstract.Clause clause;

    ClauseData(List<Pattern> patterns, Expression expression, ExprSubstitution substitution, Abstract.Clause clause) {
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
        myContext.push(new MissingClausesError.PatternClauseElem(new BindingPattern(link)));
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

      // Update patterns for each clause
      if (index > 0) {
        for (ClauseData clauseData : clauseDataList) {
          clauseData.patterns = clauseData.patterns.subList(index, clauseData.patterns.size());
        }
      }

      ClauseData conClauseData = null;
      for (ClauseData clauseData : clauseDataList) {
        Pattern pattern = clauseData.patterns.get(0);
        if (pattern instanceof EmptyPattern) {
          myUnusedClauses.remove(clauseData.clause);
          return new BranchElimTree(vars, Collections.emptyMap());
        }
        if (conClauseData == null && pattern instanceof ConstructorPattern) {
          conClauseData = clauseData;
        }
      }

      assert conClauseData != null;
      ConstructorPattern conPattern = (ConstructorPattern) conClauseData.patterns.get(0);
      List<ConCallExpression> conCalls = null;
      List<Constructor> constructors;
      if (conPattern.getConstructor().getDataType().hasIndexedConstructors()) {
        DataCallExpression dataCall = new GetTypeVisitor().visitConCall(conPattern.getExpression(), null);
        conCalls = dataCall.getMatchedConstructors();
        if (conCalls == null) {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", conClauseData.clause));
          return null;
        }
        constructors = conCalls.stream().map(ConCallExpression::getDefinition).collect(Collectors.toList());
      } else {
        constructors = conPattern.getConstructor().getDataType().getConstructors();
      }

      boolean hasVars = false;
      Map<BranchElimTree.Pattern, List<ClauseData>> constructorMap = new HashMap<>();
      for (ClauseData clauseData : clauseDataList) {
        if (clauseData.patterns.get(0) instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clauseData);
          }
        } else {
          constructorMap.computeIfAbsent(((ConstructorPattern) clauseData.patterns.get(0)).getConstructor(), k -> new ArrayList<>()).add(clauseData);
        }
      }

      if (hasVars) {
        List<ClauseData> varClauseDataList = new ArrayList<>();
        for (ClauseData clauseData : clauseDataList) {
          if (clauseData.patterns.get(0) instanceof BindingPattern) {
            varClauseDataList.add(clauseData);
            clauseData.substitution.remove(((BindingPattern) clauseData.patterns.get(0)).getBinding());
          }
        }
        constructorMap.put(BranchElimTree.Pattern.ANY, varClauseDataList);
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

            myContext.push(new MissingClausesError.ConstructorClauseElem(constructor));
            List<MissingClausesError.ClauseElem> missingClause = unflattenMissingClause(myContext);
            myContext.pop();
            boolean moreArguments = clauseDataList.get(0).patterns.size() > 1;
            if (!moreArguments) {
              for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext()) {
                if (link.isExplicit()) {
                  moreArguments = true;
                  break;
                }
              }
            }
            if (moreArguments) {
              missingClause.add(null);
            }
            addMissingClause(missingClause);
          }
        }
      }

      Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
      for (Map.Entry<BranchElimTree.Pattern, List<ClauseData>> entry : constructorMap.entrySet()) {
        List<ClauseData> conClauseDataList = entry.getValue();
        myContext.push(entry.getKey() instanceof Constructor ? new MissingClausesError.ConstructorClauseElem((Constructor) entry.getKey()) : new MissingClausesError.PatternClauseElem(conClauseDataList.get(0).patterns.get(0)));

        if (entry.getKey() instanceof Constructor) {
          for (int i = 0; i < conClauseDataList.size(); i++) {
            List<Pattern> patterns = new ArrayList<>();
            List<Pattern> oldPatterns = conClauseDataList.get(i).patterns;
            if (oldPatterns.get(0) instanceof ConstructorPattern) {
              patterns.addAll(((ConstructorPattern) oldPatterns.get(0)).getPatterns());
            } else {
              DependentLink conParameters = DependentLink.Helper.subst(((Constructor) entry.getKey()).getParameters(), new ExprSubstitution());
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
                  if (conCall1.getDefinition() == entry.getKey()) {
                    conCall = conCall1;
                    break;
                  }
                }
                assert conCall != null;
                substExpr = new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
              } else {
                substExpr = new ConCallExpression(conPattern.getConstructor(), conPattern.getSortArgument(), conPattern.getDataTypeArguments(), arguments);
              }
              conClauseDataList.get(i).substitution.add(((BindingPattern) oldPatterns.get(0)).getBinding(), substExpr);
            }
            patterns.addAll(oldPatterns.subList(1, oldPatterns.size()));
            conClauseDataList.set(i, new ClauseData(patterns, conClauseDataList.get(i).expression, conClauseDataList.get(i).substitution, conClauseDataList.get(i).clause));
          }
        }

        ElimTree elimTree = clausesToElimTree(conClauseDataList);
        if (elimTree == null) {
          myOK = false;
        } else {
          children.put(entry.getKey(), elimTree);
        }

        myContext.pop();
      }

      return new BranchElimTree(vars, children);
    }
  }

  private List<MissingClausesError.ClauseElem> unflattenMissingClause(List<MissingClausesError.ClauseElem> clause) {
    List<MissingClausesError.ClauseElem> result = new ArrayList<>();
    int expectedParams = -1;
    for (MissingClausesError.ClauseElem elem : clause) {
      while (expectedParams == 0) {
        int i = result.size() - 1;
        while (!(result.get(i) instanceof MissingClausesError.ConstructorClauseElem)) {
          i--;
        }
        Pattern pattern = new ConstructorPattern(new ConCallExpression(((MissingClausesError.ConstructorClauseElem) result.get(i)).constructor, Sort.STD, Collections.emptyList(), Collections.emptyList()), result.subList(i + 1, result.size()).stream().map(elem1 -> ((MissingClausesError.PatternClauseElem) elem1).pattern).collect(Collectors.toList()));
        result = result.subList(0, i);
        result.add(new MissingClausesError.PatternClauseElem(pattern));
        for (i = result.size() - 2; i >= 0; i--) {
          if (result.get(i) instanceof MissingClausesError.ConstructorClauseElem) {
            break;
          }
        }
        if (i == -1) {
          expectedParams = -1;
        } else {
          expectedParams = DependentLink.Helper.size(((MissingClausesError.ConstructorClauseElem) result.get(i)).constructor.getParameters()) - (result.size() - 1 - i);
        }
      }

      result.add(elem);
      if (elem instanceof MissingClausesError.ConstructorClauseElem) {
        expectedParams = DependentLink.Helper.size(((MissingClausesError.ConstructorClauseElem) elem).constructor.getParameters());
      } else {
        expectedParams--;
      }
    }
    return result;
  }

  private void addMissingClause(List<MissingClausesError.ClauseElem> clause) {
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
