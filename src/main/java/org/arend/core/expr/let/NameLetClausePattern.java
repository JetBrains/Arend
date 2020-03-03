package org.arend.core.expr.let;

import org.arend.core.definition.ClassField;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NameLetClausePattern implements LetClausePattern {
  private final String myName;

  public NameLetClausePattern(String name) {
    myName = name;
  }

  @Override
  public boolean isMatching() {
    return false;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public List<? extends ClassField> getFields() {
    return null;
  }

  @Override
  public List<? extends LetClausePattern> getPatterns() {
    return null;
  }
}
