package org.arend.core.expr;

import org.arend.core.definition.Definition;
import org.arend.core.expr.visitor.GetTypeVisitor;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.error.IncorrectExpressionException;
import org.arend.ext.core.level.LevelSubstitution;
import org.jetbrains.annotations.NotNull;

public abstract class LeveledDefCallExpression extends DefCallExpression {
  private Levels myLevels;

  public LeveledDefCallExpression(Definition definition, Levels levels) {
    super(definition);
    assert definition.status().needsTypeChecking() || (definition.getLevelParameters() == null) == (levels instanceof LevelPair);
    myLevels = levels;
  }

  @NotNull
  public Levels getLevels() {
    return myLevels;
  }

  public void setLevels(Levels levels) {
    myLevels = levels;
  }

  public LevelSubstitution getLevelSubstitution() {
    return myLevels.makeSubstitution(getDefinition());
  }

  public void substSort(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  public Levels minimizeLevels() {
    try {
      return GetTypeVisitor.MIN_INSTANCE.minimizeLevels(this);
    } catch (IncorrectExpressionException e) {
      return null;
    }
  }
}
