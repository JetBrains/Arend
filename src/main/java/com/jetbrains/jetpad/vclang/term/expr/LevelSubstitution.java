package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LevelSubstitution {
  private Map<TypedBinding, LevelExpression> mySubstExprs = new HashMap<>();

  public LevelSubstitution() {}

  public Set<TypedBinding> getDomain() {
    return mySubstExprs.keySet();
  }

  public LevelExpression get(TypedBinding binding)  {
    return mySubstExprs.get(binding);
  }
}
