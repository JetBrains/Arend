package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

public class LevelSubstitution {
  public Binding[] LevelVars = new Binding[2];
  public LevelExpression[] Values = new LevelExpression[2];
}
