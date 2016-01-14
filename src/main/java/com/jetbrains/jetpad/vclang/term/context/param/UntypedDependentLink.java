package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;

import java.util.Map;

public class UntypedDependentLink implements DependentLink {
  private final String myName;
  private DependentLink myNext;

  public UntypedDependentLink(String name, DependentLink next) {
    assert next != null;
    myName = name;
    myNext = next;
  }

  @Override
  public boolean isExplicit() {
    return myNext.isExplicit();
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
    return myNext.getType();
  }

  @Override
  public boolean isInference() {
    return false;
  }

  @Override
  public UntypedDependentLink subst(Map<Binding, Expression> substs) {
    UntypedDependentLink result = new UntypedDependentLink(myName, null);
    substs.put(this, new ReferenceExpression(result));
    result.myNext = myNext.subst(substs);
    return result;
  }
}
