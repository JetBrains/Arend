package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.MissingClausesError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class TypecheckingElim {
  private final CheckTypeVisitor myVisitor;
  private Set<Abstract.Clause> myUnusedClauses;
  private final boolean myFinal;
  private final boolean myAllowInterval;
  private final Expression myExpectedType;
  private boolean myOK = true;

  private static final int MISSING_CLAUSES_LIST_SIZE = 10;
  private List<List<MissingClausesError.ClauseElem>> myMissingClauses;

  public TypecheckingElim(CheckTypeVisitor visitor, Expression expectedType, boolean isFinal, boolean allowInterval) {
    myVisitor = visitor;
    myExpectedType = expectedType;
    myFinal = isFinal;
    myAllowInterval = allowInterval;
  }

  public ElimTree typecheckClauses(List<? extends Abstract.Clause> clauses, DependentLink patternTypes, List<DependentLink> elimParams, Abstract.SourceNode sourceNode) {
    assert elimParams.size() > 0;

    // Check that the number of patterns in each clause equals to the number of eliminated parameters
    boolean ok = true;
    for (Abstract.Clause clause : clauses) {
      if (clause.getPatterns().size() != elimParams.size()) {
        myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected " + elimParams.size() + " patterns, but got " + clause.getPatterns().size(), clause));
        ok = false;
      }
    }
    if (!ok) {
      return null;
    }

    // Put patterns in the correct order
    // If some parameters are not eliminated (i.e. absent in elimParams), then we put null in corresponding patterns
    int patternsNumber = DependentLink.Helper.size(patternTypes);
    assert elimParams.size() <= patternsNumber;
    List<ClauseData> clauseDataList = clauses.stream().map(clause -> new ClauseData(new ArrayList<>(patternsNumber), new HashMap<>(), clause)).collect(Collectors.toList());
    for (DependentLink link = patternTypes; link.hasNext(); link = link.getNext()) {
      int index = elimParams.indexOf(link);
      for (int i = 0; i < clauses.size(); i++) {
        clauseDataList.get(i).patterns.add(index < 0 ? null : clauses.get(i).getPatterns().get(index));
      }
    }

    // Used clauses are sifted out in typecheckClauses
    myUnusedClauses = new HashSet<>(clauses);
    ExprSubstitution substitution = new ExprSubstitution();
    patternTypes = DependentLink.Helper.subst(patternTypes, substitution);
    ElimTree result = typecheckClauses(clauseDataList, patternTypes, substitution, new Stack<>());
    if (result == null) {
      return null;
    }

    if (!myMissingClauses.isEmpty()) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Some patterns are missing: " + myMissingClauses, sourceNode));
    }
    for (Abstract.Clause clause : myUnusedClauses) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This clause is redundant", clause));
    }
    return myOK ? result : null;
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

  private static class ClauseData {
    List<Abstract.Pattern> patterns;
    Map<Abstract.ReferableSourceNode, Binding> context;
    Abstract.Clause clause;

    ClauseData(List<Abstract.Pattern> patterns, Map<Abstract.ReferableSourceNode, Binding> context, Abstract.Clause clause) {
      this.patterns = patterns;
      this.context = context;
      this.clause = clause;
    }
  }

  private ElimTree typecheckClauses(List<ClauseData> clauseDataList, DependentLink parameters, ExprSubstitution substitution, Stack<MissingClausesError.ClauseElem> context) {
    assert !clauseDataList.isEmpty();

    // Skip pattern columns which exclusively consists of variables
    DependentLink vars = parameters;
    for (; parameters.hasNext(); parameters = parameters.getNext()) {
      boolean allVars = true;
      for (ClauseData clauseData : clauseDataList) {
        if (!(clauseData.patterns.get(0) == null || clauseData.patterns.get(0) instanceof Abstract.NamePattern)) {
          allVars = false;
        }
      }

      if (allVars) {
        Abstract.Pattern pattern = clauseDataList.get(0).patterns.get(0);
        context.push(pattern == null ? new MissingClausesError.SkipClauseElem() : new MissingClausesError.PatternClauseElem(pattern));
        for (ClauseData clauseData : clauseDataList) {
          pattern = clauseData.patterns.get(0);
          if (pattern != null) {
            clauseData.context.put(((Abstract.NamePattern) pattern).getReferent(), parameters);
          }
          clauseData.patterns = clauseData.patterns.subList(1, clauseData.patterns.size());
        }
      } else {
        break;
      }
    }

    // If we reach the end of pattern columns, then typecheck expression in the first clause
    if (!parameters.hasNext()) {
      Abstract.Clause clause = clauseDataList.get(0).clause;
      myUnusedClauses.remove(clause);
      myVisitor.getContext().putAll(clauseDataList.get(0).context);
      Expression expectedType = myExpectedType.subst(substitution);
      CheckTypeVisitor.Result result = myFinal ? myVisitor.finalCheckExpr(clause.getExpression(), expectedType) : myVisitor.checkExpr(clause.getExpression(), expectedType);
      myVisitor.getContext().keySet().removeAll(clauseDataList.get(0).context.keySet());
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

    // Get the set of constructors on which we do not pattern match
    Set<Abstract.Constructor> missingConstructors = conCalls.stream().map(conCall -> conCall.getDefinition().getAbstractDefinition()).collect(Collectors.toSet());
    Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
    boolean hasVars = false;
    for (ClauseData clauseData : clauseDataList) {
      Abstract.Pattern pattern = clauseData.patterns.get(0);
      if (pattern instanceof Abstract.ConstructorPattern) {
        missingConstructors.remove(((Abstract.ConstructorPattern) pattern).getConstructor());
      } else {
        hasVars = true;
      }
    }

    for (ConCallExpression conCall : conCalls) {
      context.push(new MissingClausesError.ConstructorClauseElem(conCall.getDefinition().getAbstractDefinition()));

      // If we do not pattern match on the constructor and there is no clause with a variable, then report an error
      if (missingConstructors.contains(conCall.getDefinition().getAbstractDefinition())) {
        if (!hasVars) {
          addMissingClause(new ArrayList<>(context));
        }
      } else {
        // Collect clauses in which we pattern match on the constructor
        List<ClauseData> conClauseDataList = new ArrayList<>();
        for (ClauseData clauseData : clauseDataList) {
          Abstract.Pattern pattern = clauseData.patterns.get(0);
          if (!(pattern instanceof Abstract.ConstructorPattern) || ((Abstract.ConstructorPattern) pattern).getConstructor() == conCall.getDefinition().getAbstractDefinition()) {
            conClauseDataList.add(clauseData);
          }
        }

        // Check that there is a correct number of patterns and construct new lists of patterns for each clause
        for (ClauseData clauseData : conClauseDataList) {
          Abstract.Pattern pattern = clauseData.patterns.get(0);
          List<Abstract.Pattern> newPatterns;
          if (pattern instanceof Abstract.ConstructorPattern) {
            newPatterns = new ArrayList<>();
            List<? extends Abstract.PatternArgument> patternArgs = ((Abstract.ConstructorPattern) pattern).getArguments();
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
                myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Not enough patterns, expected " + DependentLink.Helper.size(conCall.getDefinition().getParameters()) + " more", pattern));
                return null;
              }
              if (link.isExplicit() && !patternArgs.get(i).isExplicit()) {
                myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected an explicit pattern", patternArgs.get(i)));
                return null;
              }
              newPatterns.add(patternArgs.get(i).getPattern());
            }
            if (i < patternArgs.size()) {
              myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Too many patterns", patternArgs.get(i)));
            }
          } else {
            newPatterns = new ArrayList<>(Collections.nCopies(DependentLink.Helper.size(conCall.getDefinition().getParameters()), null));
          }
          newPatterns.addAll(clauseData.patterns.subList(1, clauseData.patterns.size()));
          clauseData.patterns = newPatterns;
        }

        // Construct new list of parameters and new substitution
        DependentLink newParameters = DependentLink.Helper.subst(conCall.getDefinition().getParameters(), new ExprSubstitution());
        for (DependentLink link = newParameters; link.hasNext(); link = link.getNext()) {
          conCall.addArgument(new ReferenceExpression(link));
        }
        ExprSubstitution newSubstitution = new ExprSubstitution();
        for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
          newSubstitution.add(entry.getKey(), entry.getValue().subst(new ExprSubstitution(parameters, conCall)));
        }
        DependentLink.Helper.getLast(newParameters).setNext(parameters.getNext());

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

    return new BranchElimTree(vars, children);
  }
}
