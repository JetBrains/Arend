package com.jetbrains.jetpad.vclang.term.expr.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;

import java.util.Map;

public class TypedDependentLink implements DependentLink {
  private final boolean myExplicit;
  private final String myName;
  private final Expression myType;
  private DependentLink myNext;

  public TypedDependentLink(boolean isExplicit, String name, Expression type, DependentLink next) {
    myExplicit = isExplicit;
    myName = name;
    myType = type;
    myNext = next;
  }

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  @Override
  public DependentLink getNext() {
    return myNext;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Expression getType() {
    return myType;
  }

  @Override
  public boolean isInference() {
    return false;
  }

  @Override
  public TypedDependentLink copy(Map<Binding, Expression> substs) {
    TypedDependentLink result = new TypedDependentLink(isExplicit(), myName, myType.subst(substs), null);
    substs.put(this, new ReferenceExpression(result));
    if (myNext != null) {
      result.myNext = myNext.copy(substs);
    }
    return result;
  }
}
