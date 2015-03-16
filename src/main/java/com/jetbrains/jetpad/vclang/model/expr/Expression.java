package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public abstract class Expression extends Node implements Abstract.Expression {
  private Property<com.jetbrains.jetpad.vclang.term.expr.Expression> myWellTypedExpr = new ValueProperty<>();

  public Property<com.jetbrains.jetpad.vclang.term.expr.Expression> wellTypedExpr() {
    return myWellTypedExpr;
  }
}
