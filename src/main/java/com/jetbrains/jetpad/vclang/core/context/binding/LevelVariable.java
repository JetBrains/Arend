package com.jetbrains.jetpad.vclang.core.context.binding;

public interface LevelVariable extends Variable {
  enum LvlType { PLVL, HLVL }
  LvlType getType();

  LevelVariable PVAR = new LevelVariable() {
    @Override
    public String getName() {
      return "\\lp";
    }

    @Override
    public LvlType getType() {
      return LvlType.PLVL;
    }
  };

  LevelVariable HVAR = new LevelVariable() {
    @Override
    public String getName() {
      return "\\lh";
    }

    @Override
    public LvlType getType() {
      return LvlType.HLVL;
    }
  };
}
