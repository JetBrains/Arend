package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.Collections;
import java.util.List;

public class NamePattern extends Pattern implements Abstract.NamePattern{
  private final String myName;

  public NamePattern(String name, boolean isExplicit) {
    super(isExplicit);
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Utils.PatternMatchResult match(Expression expr, List<Binding> context) {
    return new Utils.PatternMatchResult(Collections.singletonList(expr));
  }
}
