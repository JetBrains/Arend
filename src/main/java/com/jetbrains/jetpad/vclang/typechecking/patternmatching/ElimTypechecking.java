package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.GetTypeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;
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
    List<DependentLink> elimParams = new ArrayList<>(elimExpr.getExpressions().size());
    for (Abstract.Expression expr : elimExpr.getExpressions()) {
      if (expr instanceof Abstract.ReferenceExpression) {
        elimParams.add((DependentLink) myVisitor.getContext().remove(((Abstract.ReferenceExpression) expr).getReferent()));
      } else {
        // TODO[newElim]: report an error
      }
    }

    List<ClauseData> clauses = new ArrayList<>(elimExpr.getClauses().size());
    PatternTypechecking patternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), myAllowInterval);
    myOK = true;
    for (Abstract.Clause clause : elimExpr.getClauses()) {
      Pair<List<Pattern>, Expression> result = patternTypechecking.typecheckClause(clause, patternTypes, elimParams, myExpectedType, myVisitor, true);
      if (result == null) {
        myOK = false;
      } else {
        Stack<Pattern> patterns = new Stack<>();
        for (int i = result.proj1.size() - 1; i >= 0; i--) {
          patterns.push(result.proj1.get(i));
        }
        clauses.add(new ClauseData(patterns, result.proj2, new ExprSubstitution(), clause));
      }
    }
    if (!myOK) {
      return null;
    }

    myUnusedClauses = new HashSet<>(elimExpr.getClauses());
    myContext = new Stack<>();
    ElimTree elimTree = clausesToElimTree(clauses);

    if (myMissingClauses != null && !myMissingClauses.isEmpty()) {
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
    Stack<Pattern> patterns;
    Expression expression;
    ExprSubstitution substitution;
    Abstract.Clause clause;

    ClauseData(Stack<Pattern> patterns, Expression expression, ExprSubstitution substitution, Abstract.Clause clause) {
      this.patterns = patterns;
      this.expression = expression;
      this.substitution = substitution;
      this.clause = clause;
    }
  }

  private ElimTree clausesToElimTree(List<ClauseData> clauseDataList) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = clauseDataList.get(0).patterns.size() - 1;
      loop:
      for (; index >= 0; index--) {
        for (ClauseData clauseData : clauseDataList) {
          if (!(clauseData.patterns.get(index) instanceof BindingPattern)) {
            break loop;
          }
        }
      }

      // If all patterns are variables
      if (index < 0) {
        ClauseData clauseData = clauseDataList.get(0);
        myUnusedClauses.remove(clauseData.clause);
        return new LeafElimTree(((BindingPattern) clauseData.patterns.peek()).getBinding(), clauseData.expression.subst(clauseData.substitution));
      }

      // Make new list of variables
      DependentLink vars = ((BindingPattern) clauseDataList.get(0).patterns.peek()).getBinding().subst(clauseDataList.get(0).substitution, LevelSubstitution.EMPTY, clauseDataList.get(0).patterns.size() - 1 - index);
      for (DependentLink link = vars; link.hasNext(); link = link.getNext()) {
        myContext.push(new MissingClausesError.PatternClauseElem(new BindingPattern(link)));
      }

      // Update substitution and patterns for each clause
      for (DependentLink link = vars; link.hasNext(); link = link.getNext()) {
        Expression newRef = new ReferenceExpression(link);
        clauseDataList.get(0).patterns.pop();
        for (int i = 1; i < clauseDataList.size(); i++) {
          clauseDataList.get(i).substitution.add(((BindingPattern) clauseDataList.get(i).patterns.pop()).getBinding(), newRef);
        }
      }

      ClauseData conClauseData = null;
      for (ClauseData clauseData : clauseDataList) {
        Pattern pattern = clauseData.patterns.peek();
        if (pattern instanceof EmptyPattern) {
          myUnusedClauses.remove(clauseData.clause);
          return new BranchElimTree(vars, Collections.emptyMap());
        }
        if (conClauseData == null && pattern instanceof ConstructorPattern) {
          conClauseData = clauseData;
        }
      }

      assert conClauseData != null;
      ConstructorPattern conPattern = (ConstructorPattern) conClauseData.patterns.peek();
      List<Constructor> constructors;
      if (conPattern.getConstructor().getDataType().hasIndexedConstructors()) {
        DataCallExpression dataCall = new GetTypeVisitor().visitConCall(conPattern.getExpression(), null);
        List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
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
        if (clauseData.patterns.peek() instanceof BindingPattern) {
          hasVars = true;
          for (Constructor constructor : constructors) {
            constructorMap.computeIfAbsent(constructor, k -> new ArrayList<>()).add(clauseData);
          }
        } else {
          constructorMap.computeIfAbsent(((ConstructorPattern) clauseData.patterns.peek()).getConstructor(), k -> new ArrayList<>()).add(clauseData);
        }
      }

      if (constructors.size() > constructorMap.size()) {
        if (hasVars) {
          List<ClauseData> varClauseDataList = new ArrayList<>();
          for (ClauseData clauseData : clauseDataList) {
            if (clauseData.patterns.peek() instanceof BindingPattern) {
              varClauseDataList.add(clauseData);
            }
          }
          constructorMap.put(BranchElimTree.Pattern.ANY, varClauseDataList);
        } else {
          for (Constructor constructor : constructors) {
            if (!constructorMap.containsKey(constructor)) {
              List<MissingClausesError.ClauseElem> missingClause = unflattenMissingClause(myContext);
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
      }

      Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
      for (Map.Entry<BranchElimTree.Pattern, List<ClauseData>> entry : constructorMap.entrySet()) {
        List<ClauseData> conClauseDataList = entry.getValue();
        myContext.push(entry.getKey() instanceof Constructor ? new MissingClausesError.ConstructorClauseElem((Constructor) entry.getKey()) : new MissingClausesError.PatternClauseElem(conClauseDataList.get(0).patterns.peek()));

        if (entry.getKey() instanceof Constructor) {
          for (int i = 0; i < conClauseDataList.size(); i++) {
            Stack<Pattern> patterns = new Stack<>();
            patterns.addAll(conClauseDataList.get(i).patterns);
            if (patterns.peek() instanceof ConstructorPattern) {
              Pattern pattern = patterns.pop();
              for (int j = ((ConstructorPattern) pattern).getPatterns().size() - 1; j >= 0; j--) {
                patterns.push(((ConstructorPattern) pattern).getPatterns().get(j));
              }
            }
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

  /*
  public ElimTree typecheckClauses(List<? extends Abstract.Clause> clauses, DependentLink patternTypes, List<DependentLink> elimParams, Abstract.SourceNode sourceNode) {
    assert !elimParams.isEmpty();
    myMissingClauses = null;

    // Check that the number of patterns in each clause equals to the number of eliminated parameters
    myOK = true;
    for (Abstract.Clause clause : clauses) {
      if (clause.getPatterns().size() != elimParams.size()) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected " + elimParams.size() + " patterns, but got " + clause.getPatterns().size(), clause));
        myOK = false;
      }
    }
    if (!myOK) {
      return null;
    }

    // Put patterns in the correct order
    // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
    List<ClauseData> clauseDataList = clauses.stream().map(clause -> new ClauseData(new ArrayList<>(patternsNumber), new HashMap<>(), myFinal ? new HashSet<>() : null, clause)).collect(Collectors.toList());
    for (DependentLink link = patternTypes; link.hasNext(); link = link.getNext()) {
      int index = elimParams.indexOf(link);
      for (int i = 0; i < clauses.size(); i++) {
        clauseDataList.get(i).patterns.add(index < 0 ? null : clauses.get(i).getPatterns().get(index));
      }
    }

    int patternsNumber = DependentLink.Helper.size(patternTypes);
    assert elimParams.size() <= patternsNumber;
    List<ClauseData> clauseDataList = clauses.stream().map(clause -> new ClauseData(new ArrayList<>(patternsNumber), null, clause)).collect(Collectors.toList());
    PatternTypechecking patternTypechecking = new PatternTypechecking(myVisitor.getErrorReporter(), myAllowInterval);

    // Used clauses are sifted out in typecheckClauses
    myUnusedClauses = new HashSet<>(clauses);
    ExprSubstitution substitution = new ExprSubstitution();
    patternTypes = DependentLink.Helper.subst(patternTypes, substitution);

    // Update the context and \this binding in myVisitor
    for (Map.Entry<Abstract.ReferableSourceNode, Binding> entry : myVisitor.getContext().entrySet()) {
      Expression newValue = substitution.get(entry.getValue());
      if (newValue != null) {
        entry.setValue(((ReferenceExpression) newValue).getBinding());
      }
    }
    Expression newThis = substitution.get(myVisitor.getTypeCheckingDefCall().getThisBinding());
    if (newThis != null) {
      myVisitor.setThis(myVisitor.getTypeCheckingDefCall().getThisClass(), ((ReferenceExpression) newThis).getBinding());
    }
    myExpectedType = myExpectedType.subst(substitution);

    ExprSubstitution newSubstitution = new ExprSubstitution();
    for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
      newSubstitution.add(entry.getValue().toReference().getBinding(), entry.getValue());
    }

    ElimTree result = typecheckClauses(clauseDataList, patternTypes, newSubstitution, new Stack<>());
    if (result == null) {
      return null;
    }

    if (myMissingClauses != null && !myMissingClauses.isEmpty()) {
      myVisitor.getErrorReporter().report(new MissingClausesError(myMissingClauses, sourceNode));
    }
    if (!myOK) {
      return null;
    }
    for (Abstract.Clause clause : myUnusedClauses) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This clause is redundant", clause));
    }
    return result;
  }

  private static class ClauseData {
    List<Pattern> patterns;
    Expression expression;
    Abstract.Clause clause;

    ClauseData(List<Pattern> patterns, Expression expression, Abstract.Clause clause) {
      this.patterns = patterns;
      this.expression = expression;
      this.clause = clause;
    }
  }

  private ElimTree typecheckClauses(List<ClauseData> clauseDataList, DependentLink parameters, ExprSubstitution substitution, Stack<MissingClausesError.ClauseElem> context) {
    assert !clauseDataList.isEmpty();

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(context)) {
      // Skip pattern columns which exclusively consists of variables
      DependentLink vars = parameters;
      loop:
      for (; parameters.hasNext(); parameters = parameters.getNext()) {
        for (ClauseData clauseData : clauseDataList) {
          if (!(clauseData.patterns.get(0) == null || clauseData.patterns.get(0) instanceof Abstract.NamePattern)) {
            break loop;
          }
        }

        Abstract.Pattern pattern = clauseDataList.get(0).patterns.get(0);
        context.push(pattern == null ? new MissingClausesError.SkipClauseElem() : new MissingClausesError.PatternClauseElem(pattern));
        for (ClauseData clauseData : clauseDataList) {
          pattern = clauseData.patterns.get(0);
          if (pattern != null) {
            clauseData.context.put(((Abstract.NamePattern) pattern).getReferent(), parameters);
          }
          if (clauseData.freeBindings != null) {
            clauseData.freeBindings.add(parameters);
          }
          clauseData.patterns = clauseData.patterns.subList(1, clauseData.patterns.size());
        }
      }

      // If we reach the end of pattern columns, then typecheck expression in the first clause
      if (!parameters.hasNext()) {
        Abstract.Clause clause = clauseDataList.get(0).clause;
        myUnusedClauses.remove(clause);
        myVisitor.getContext().putAll(clauseDataList.get(0).context);
        if (clauseDataList.get(0).freeBindings != null) {
          myVisitor.getFreeBindings().addAll(clauseDataList.get(0).freeBindings);
        }
        Expression expectedType = myExpectedType.subst(substitution);
        CheckTypeVisitor.Result result = myFinal ? myVisitor.finalCheckExpr(clause.getExpression(), expectedType) : myVisitor.checkExpr(clause.getExpression(), expectedType);
        myVisitor.getContext().keySet().removeAll(clauseDataList.get(0).context.keySet());
        if (clauseDataList.get(0).freeBindings != null) {
          myVisitor.getFreeBindings().removeAll(clauseDataList.get(0).freeBindings);
        }
        return result == null ? null : new LeafElimTree(vars, result.expression);
      }

      // Get the first empty clause and the first constructor clause
      ClauseData emptyClause = clauseDataList.stream().filter(clauseData -> clauseData.patterns.get(0) instanceof Abstract.EmptyPattern).findFirst().orElse(null);
      Abstract.Pattern firstPattern = (emptyClause != null ? emptyClause : clauseDataList.stream().filter(clauseData -> clauseData.patterns.get(0) instanceof Abstract.ConstructorPattern).findFirst().orElse(null)).patterns.get(0);

      // Check that the type of the first parameter is a data type
      Expression type = parameters.getType().getExpr().subst(substitution, LevelSubstitution.EMPTY).normalize(NormalizeVisitor.Mode.WHNF);
      DataCallExpression dataCall = type.toDataCall();
      if (dataCall == null) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a data type, actual type: " + type, firstPattern));
        return null;
      }
      if (!myAllowInterval && dataCall.getDefinition() == Prelude.INTERVAL) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Pattern matching on the interval is not allowed here", firstPattern));
        return null;
      }
      List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
      if (conCalls == null) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Elimination is not possible here, cannot determine the set of eligible constructors", firstPattern));
        return null;
      }
      vars = DependentLink.Helper.slice(vars, parameters);

      // If we have an empty pattern in the first column,
      // then check that the data type is empty and the rest of the corresponding clause consists of variables
      if (emptyClause != null) {
        if (!conCalls.isEmpty()) {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Data type " + type + " is not empty", firstPattern));
          return null;
        }
        myUnusedClauses.remove(emptyClause.clause);

        boolean ok = emptyClause.clause.getExpression() == null;
        if (ok) {
          for (Abstract.Pattern pattern : emptyClause.patterns) {
            if (!(pattern == null || pattern instanceof Abstract.NamePattern)) {
              ok = false;
              break;
            }
          }
        }
        if (!ok) {
          myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "The rest of the clause is ignored", firstPattern));
          return null;
        }

        return new BranchElimTree(vars, Collections.emptyMap());
      }

      // Collect clauses in which we pattern match on constructors and variables
      Map<BranchElimTree.Pattern, List<ClauseData>> clauseDataLists = new HashMap<>();
      for (ConCallExpression conCall : conCalls) {
        clauseDataLists.put(conCall.getDefinition(), clauseDataList.stream().filter(clauseData -> {
          Abstract.Pattern pattern = clauseData.patterns.get(0);
          return !(pattern instanceof Abstract.ConstructorPattern) || ((Abstract.ConstructorPattern) pattern).getConstructor() == conCall.getDefinition().getAbstractDefinition();
        }).collect(Collectors.toList()));
      }
      List<ClauseData> varClauseDataList = clauseDataList.stream().filter(clauseData -> !(clauseData.patterns.get(0) instanceof Abstract.ConstructorPattern)).collect(Collectors.toList());

      boolean hasMissingConstructors = false;
      Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
      for (ConCallExpression conCall : conCalls) {
        context.push(new MissingClausesError.ConstructorClauseElem(conCall.getDefinition().getAbstractDefinition()));

        List<ClauseData> conClauseDataList = clauseDataLists.get(conCall.getDefinition());
        // If we do not pattern match on the constructor and there is no clause with a variable, then report an error
        if (conClauseDataList.isEmpty()) {
          boolean ok = false;
          if (conCall.getDefinition() == Prelude.PROP_TRUNC_PATH_CON) {
            Sort sort = myExpectedType.subst(substitution).getType().toSort();
            if (sort != null && sort.isProp()) {
              ok = true;
            }
          } else if (conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
            Sort sort = myExpectedType.subst(substitution).getType().toSort();
            if (sort != null && sort.isSet()) {
              ok = true;
            }
          }

          if (!ok) {
            hasMissingConstructors = true;
            if (varClauseDataList.isEmpty()) {
              List<MissingClausesError.ClauseElem> missingClause = new ArrayList<>(context);
              boolean moreArguments = clauseDataList.get(0).patterns.size() > 1;
              if (!moreArguments) {
                for (DependentLink link = conCall.getDefinition().getParameters(); link.hasNext(); link = link.getNext()) {
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
        } else {
          // Check that there is a correct number of patterns and construct new lists of patterns for each clause
          for (int j = 0; j < conClauseDataList.size(); j++) {
            ClauseData clauseData = conClauseDataList.get(j);
            Abstract.Pattern pattern = clauseData.patterns.get(0);
            List<Abstract.Pattern> newPatterns;
            boolean isConPattern = pattern instanceof Abstract.ConstructorPattern;
            if (isConPattern) {
              newPatterns = new ArrayList<>();
              List<? extends Abstract.Pattern> patternArgs = ((Abstract.ConstructorPattern) pattern).getArguments();
              int i = 0;
              for (DependentLink link = conCall.getDefinition().getParameters(); link.hasNext(); link = link.getNext(), i++) {
                if (i >= patternArgs.size() || patternArgs.get(i).isExplicit()) {
                  while (link.hasNext() && !link.isExplicit()) {
                    newPatterns.add(null);
                    link = link.getNext();
                  }
                  if (!link.hasNext()) {
                    break;
                  }
                }
                if (i >= patternArgs.size()) {
                  myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Not enough patterns, expected " + DependentLink.Helper.size(link) + " more", pattern));
                  return null;
                }
                if (link.isExplicit() && !patternArgs.get(i).isExplicit()) {
                  myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected an explicit pattern", patternArgs.get(i)));
                  return null;
                }
                newPatterns.add(patternArgs.get(i));
              }
              if (i < patternArgs.size()) {
                myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Too many patterns", patternArgs.get(i)));
              }
            } else {
              newPatterns = new ArrayList<>(Collections.nCopies(DependentLink.Helper.size(conCall.getDefinition().getParameters()), null));
            }
            newPatterns.addAll(clauseData.patterns.subList(1, clauseData.patterns.size()));
            conClauseDataList.set(j, new ClauseData(newPatterns, isConPattern ? clauseData.context : new HashMap<>(clauseData.context), isConPattern || clauseData.freeBindings == null ? clauseData.freeBindings : new HashSet<>(clauseData.freeBindings), clauseData.clause));
          }

          // Construct new list of parameters and new substitution
          DependentLink newParameters = conCall.getConstructorParameters();
          for (DependentLink link = newParameters; link.hasNext(); link = link.getNext()) {
            conCall.addArgument(new ReferenceExpression(link));
          }
          ExprSubstitution newSubstitution = new ExprSubstitution();
          for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
            newSubstitution.add(entry.getKey(), entry.getValue().subst(new ExprSubstitution(parameters, conCall)));
          }
          if (newParameters.hasNext()) {
            DependentLink.Helper.getLast(newParameters).setNext(parameters.getNext());
          } else {
            newParameters = parameters.getNext();
          }

          // Recursively typecheck clauses
          ElimTree elimTree = typecheckClauses(conClauseDataList, newParameters, newSubstitution, context);
          if (elimTree != null) {
            children.put(conCall.getDefinition(), elimTree);
          } else {
            myOK = false;
          }
        }
        context.pop();
      }

      // Check patterns that do not pattern match on constructors
      if (hasMissingConstructors && !varClauseDataList.isEmpty()) {
        context.push(varClauseDataList.get(0).patterns.get(0) != null ? new MissingClausesError.PatternClauseElem(varClauseDataList.get(0).patterns.get(0)) : new MissingClausesError.SkipClauseElem());

        for (ClauseData clauseData : varClauseDataList) {
          Abstract.Pattern pattern = clauseData.patterns.get(0);
          if (pattern instanceof Abstract.NamePattern) {
            clauseData.context.put(((Abstract.NamePattern) pattern).getReferent(), parameters);
          }
          if (clauseData.freeBindings != null) {
            clauseData.freeBindings.add(parameters);
          }
          clauseData.patterns = clauseData.patterns.subList(1, clauseData.patterns.size());
        }

        ElimTree elimTree = typecheckClauses(varClauseDataList, parameters.getNext(), substitution, context);
        if (elimTree != null) {
          children.put(BranchElimTree.Pattern.ANY, elimTree);
        } else {
          myOK = false;
        }

        context.pop();
      }

      return new BranchElimTree(vars, children);
    }
  }
  */
}
