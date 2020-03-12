package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteLevel;

public class IgnoredLevelsError extends TypecheckingError {
  public ConcreteLevel pLevel;
  public ConcreteLevel hLevel;

  public IgnoredLevelsError(ConcreteLevel pLevel, ConcreteLevel hLevel) {
    super(Level.WARNING_UNUSED, "Levels are ignored", pLevel != null ? pLevel : hLevel);
    this.pLevel = pLevel;
    this.hLevel = hLevel;
  }
}
