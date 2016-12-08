package com.jetbrains.jetpad.vclang.term.context.binding;

public interface LevelVariable extends Variable {
  enum LvlType { PLVL, HLVL }
  LvlType getType();
}
