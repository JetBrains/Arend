package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Binding implements Abstract.Binding {
  private final Name myName;

  public Binding(Name name) {
    myName = name;
  }

  public Binding(String name) {
    myName = new Name(name, Abstract.Definition.Fixity.PREFIX);
  }

  @Override
  public Name getName() {
    return myName;
  }

  public void setFixity(Abstract.Definition.Fixity fixity) {
    myName.fixity = fixity;
  }

  public abstract Expression getType();

  public abstract Binding lift(int on);

  public boolean isInference() {
    return false;
  }
}
