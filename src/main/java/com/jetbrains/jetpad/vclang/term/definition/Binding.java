package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public abstract class Binding implements Abstract.Binding {
  private final Utils.Name myName;

  public Binding(Utils.Name name) {
    myName = name;
  }

  public Binding(String name) {
    myName = new Utils.Name(name, Abstract.Definition.Fixity.PREFIX);
  }

  @Override
  public Utils.Name getName() {
    return myName;
  }

  public void setFixity(Abstract.Definition.Fixity fixity) {
    myName.fixity = fixity;
  }

  public abstract Expression getType();

  public abstract Binding lift(int on);
}
