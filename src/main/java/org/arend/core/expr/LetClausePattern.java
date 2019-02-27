package org.arend.core.expr;

import java.util.List;

public class LetClausePattern {
  private final List<LetClausePattern> myPatterns;

  public LetClausePattern(List<LetClausePattern> patterns) {
    myPatterns = patterns;
  }

  public LetClausePattern() {
    myPatterns = null;
  }

  public List<? extends LetClausePattern> getPatterns() {
    return myPatterns;
  }

  public boolean isMatching() {
    return myPatterns != null;
  }
}
