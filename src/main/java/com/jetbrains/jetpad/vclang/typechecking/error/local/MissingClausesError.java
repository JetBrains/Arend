package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class MissingClausesError extends LocalTypeCheckingError {
  private final List<List<ClauseElem>> myMissingClauses;

  public MissingClausesError(List<List<ClauseElem>> missingClauses, Abstract.SourceNode cause) {
    super("Some clauses are missing", cause);
    myMissingClauses = missingClauses;
  }

  public List<List<ClauseElem>> getMissingClauses() {
    return myMissingClauses;
  }

  public interface ClauseElem {
  }

  public static class PatternClauseElem implements ClauseElem {
    public Abstract.Pattern pattern;

    public PatternClauseElem(Abstract.Pattern pattern) {
      this.pattern = pattern;
    }
  }

  public static class ConstructorClauseElem implements ClauseElem {
    public Abstract.Constructor constructor;

    public ConstructorClauseElem(Abstract.Constructor constructor) {
      this.constructor = constructor;
    }
  }

  public static class SkipClauseElem implements ClauseElem {
    public SkipClauseElem() {}
  }
}
