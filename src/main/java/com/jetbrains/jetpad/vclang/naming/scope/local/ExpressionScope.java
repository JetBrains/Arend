package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpressionScope {
  public static Scope localScope(LocalScope parentScope, Object parentExpr, Object thisExpr) {
    if (parentExpr instanceof Abstract.ParametersHolder) {
      Collection<? extends Abstract.Parameter> parameters = ((Abstract.ParametersHolder) parentExpr).getParameters();
      List<Abstract.Parameter> parameters1;
      if (thisExpr instanceof Abstract.Parameter) {
        parameters1 = new ArrayList<>(parameters.size());
        for (Abstract.Parameter parameter : parameters) {
          if (thisExpr.equals(parameter)) {
            break;
          }
          parameters1.add(parameter);
        }
      } else {
        parameters1 = new ArrayList<>(parameters);
      }
      return new TelescopeScope(parentScope, parameters1);
    }

    if (parentExpr instanceof Abstract.LetClausesHolder) {
      Collection<? extends Abstract.LetClause> clauses = ((Abstract.LetClausesHolder) parentExpr).getLetClauses();
      List<Abstract.LetClause> clauses1;
      if (thisExpr instanceof Abstract.LetClause) {
        clauses1 = new ArrayList<>(clauses.size());
        for (Abstract.LetClause clause : clauses) {
          if (thisExpr.equals(clause)) {
            break;
          }
          clauses1.add(clause);
        }
      } else {
        clauses1 = new ArrayList<>(clauses);
      }
      return new LetScope(parentScope, clauses1);
    }

    if (parentExpr instanceof Abstract.FunctionClause && thisExpr instanceof Abstract.Expression) {
      return new PatternScope(parentScope, ((Abstract.FunctionClause) parentExpr).getPatterns());
    }

    if (thisExpr instanceof Abstract.Pattern) {
      return parentScope.getGlobalScope();
    }

    return parentScope;
  }
}
