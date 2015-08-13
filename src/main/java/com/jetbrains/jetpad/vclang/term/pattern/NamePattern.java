package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class NamePattern extends Pattern implements Abstract.NamePattern{
  private final String myName;

  public NamePattern(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return null;
  }
}
