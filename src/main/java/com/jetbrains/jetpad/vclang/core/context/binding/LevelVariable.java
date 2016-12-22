package com.jetbrains.jetpad.vclang.core.context.binding;

public interface LevelVariable extends Variable {
  enum LvlType { PLVL, HLVL }
  LvlType getType();
}
