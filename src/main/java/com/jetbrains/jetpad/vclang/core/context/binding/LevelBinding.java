package com.jetbrains.jetpad.vclang.core.context.binding;

public class LevelBinding implements LevelVariable {
  private final LvlType myType;

  public final static LevelBinding PLVL_BND = new LevelBinding(LvlType.PLVL);
  public final static LevelBinding HLVL_BND = new LevelBinding(LvlType.HLVL);

  private LevelBinding(LvlType type) {
    myType = type;
  }

  @Override
  public String getName() {
    return myType == LvlType.PLVL ? "\\lp" : "\\lh";
  }

  @Override
  public LvlType getType() {
    return myType;
  }

  @Override
  public String toString() {
    return getName();
  }
}
