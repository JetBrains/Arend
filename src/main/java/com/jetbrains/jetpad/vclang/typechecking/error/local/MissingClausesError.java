package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class MissingClausesError extends LocalTypeCheckingError {
  private final List<List<Expression>> myMissingClauses;

  public MissingClausesError(List<List<Expression>> missingClauses, Abstract.SourceNode cause) {
    super("Some clauses are missing", cause);
    myMissingClauses = missingClauses;
  }

  public List<List<Expression>> getMissingClauses() {
    return myMissingClauses;
  }
}
