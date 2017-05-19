package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class TypecheckingElim {
  private final CheckTypeVisitor myVisitor;
  private Set<Abstract.Clause> myUnusedClauses;
  private final boolean myFinal;
  private final Expression myExpectedType;

  public TypecheckingElim(CheckTypeVisitor visitor, boolean isFinal, Expression expectedType) {
    myVisitor = visitor;
    myFinal = isFinal;
    myExpectedType = expectedType;
  }

  public ElimTree typecheckClauses(List<? extends Abstract.Clause> clauses, DependentLink patternTypes, List<DependentLink> elimParams) {
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
    List<ClauseData> clauseDataList = clauses.stream().map(clause -> new ClauseData(new ArrayList<>(patternsNumber), null, null, new HashMap<>(), clause)).collect(Collectors.toList());
    for (DependentLink link = patternTypes; link.hasNext(); link = link.getNext()) {
      int index = elimParams.indexOf(link);
      for (int i = 0; i < clauses.size(); i++) {
        clauseDataList.get(i).patterns.add(index < 0 ? null : clauses.get(i).getPatterns().get(index));
      }
    }

    // Used clauses are sifted out in typecheckClauses
    myUnusedClauses = new HashSet<>(clauses);
    ElimTree result = typecheckClauses(clauseDataList, DependentLink.Helper.toList(patternTypes));
    if (result == null) {
      return null;
    }

    for (Abstract.Clause clause : myUnusedClauses) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError(Error.Level.WARNING, "This clause is redundant", clause));
    }
    return result;
  }

  private static class ClauseData {
    final List<Pattern> patterns;
    final DependentLink parameters;
    final Map<Abstract.ReferableSourceNode, Binding> context;

    ClauseData(List<Pattern> patterns, DependentLink parameters, Map<Abstract.ReferableSourceNode, Binding> context) {
      this.patterns = patterns;
      this.parameters = parameters;
      this.context = context;
    }

    DependentLink move(int steps) {
      if (steps == 0) {
        return parameters;
      }

      DependentLink result = parameters;
      DependentLink link = parameters;
      for (int i = 0; i < steps - 1; i++) {
        link = link.getNext();
      }
      parameters = link.getNext();
      link.setNext(EmptyDependentLink.getInstance());

      return result;
    }
  }

  private ElimTree typecheckClauses(List<ClauseData> clauseDataList, List<DependentLink> parameters) {
    // assert clauseDataList.stream().allMatch(clause -> clause.patterns.size() == parameters.size());
    assert !clauseDataList.isEmpty();

    if (parameters.isEmpty()) {
      Abstract.Clause clause = clauseDataList.get(0).clause;
      myUnusedClauses.remove(clause);
      myVisitor.getContext().putAll(clauseDataList.get(0).context);
      Expression expectedType = myExpectedType.subst();
      CheckTypeVisitor.Result result = myFinal ? myVisitor.finalCheckExpr(clause.getExpression(), expectedType) : myVisitor.checkExpr(clause.getExpression(), expectedType);
      myVisitor.getContext().keySet().removeAll(clauseDataList.get(0).context.keySet());
      return result == null ? null : new LeafElimTree(context, result.expression);
    }

    ClauseData constructorClause = clauseDataList.stream().filter(clause -> clause.patterns.get(0) instanceof Abstract.ConstructorPattern || clause.patterns.get(0) instanceof Abstract.AnyConstructorPattern).findFirst().orElse(null);
    if (constructorClause == null) {
      for (int i = 0; i < clauseDataList.size(); i++) {
        ClauseData clauseData = clauseDataList.get(i);
        if (clauseData.patterns.get(0) instanceof Abstract.NamePattern) {
          clauseData.context.put(((Abstract.NamePattern) clauseData.patterns.get(0)).getReferent(), index); // TODO: do not modify it here
        }
        clauseDataList.set(i, new ClauseData(clauseData.patterns.subList(1, clauseData.patterns.size()), clauseData.parameters, clauseData.context, clauseData.clause));
      }
      return typecheckClauses(clauseDataList, context, index.getNext());
    }

    Expression paramType = index.getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF);
    if (paramType.toDataCall() == null) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected a data type, actual type: " + paramType, constructorClause.patterns.get(0)));
      return null;
    }
    DataCallExpression dataCall = paramType.toDataCall();

    Map<Constructor, List<ClauseData>> constructorMap = new HashMap<>();
    for (ClauseData clauseData : clauseDataList) {
      if (clauseData.patterns.get(0) instanceof Abstract.ConstructorPattern) {
        Constructor constructor = (Constructor) myVisitor.getTypecheckingState().getTypechecked(((Abstract.ConstructorPattern) clauseData.patterns.get(0)).getConstructor());
        if (constructor != null && constructor.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
          constructorMap.putIfAbsent(constructor, new ArrayList<>()).add(clauseData);
        }
      } else {

      }
    }

    return null;
  }
}
