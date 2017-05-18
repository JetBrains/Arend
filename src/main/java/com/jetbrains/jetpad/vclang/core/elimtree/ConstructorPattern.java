package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;

import java.util.List;

public class ConstructorPattern implements Pattern {
  private final Constructor myConstructor;
  private final List<Pattern> myPatterns;

  public ConstructorPattern(Constructor constructor, List<Pattern> patterns) {
    myConstructor = constructor;
    myPatterns = patterns;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  public List<Pattern> getPatterns() {
    return myPatterns;
  }
}
