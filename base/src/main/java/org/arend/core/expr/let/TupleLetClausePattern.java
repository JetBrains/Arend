package org.arend.core.expr.let;

import org.arend.core.definition.ClassField;

import java.util.List;

public class TupleLetClausePattern implements LetClausePattern {
  private final List<LetClausePattern> myPatterns;

  public TupleLetClausePattern(List<LetClausePattern> patterns) {
    myPatterns = patterns;
  }

  @Override
  public boolean isMatching() {
    return true;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public List<? extends ClassField> getFields() {
    return null;
  }

  public List<? extends LetClausePattern> getPatterns() {
    return myPatterns;
  }
}
