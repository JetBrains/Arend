package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;

import java.util.List;
import java.util.Map;

public class UntypedDependentLink implements DependentLink {
  private final String myName;
  private DependentLink myNext;

  public UntypedDependentLink(String name, DependentLink next) {
    assert next instanceof UntypedDependentLink || next instanceof TypedDependentLink;
    myName = name;
    myNext = next;
  }

  @Override
  public boolean isExplicit() {
    return myNext.isExplicit();
  }

  @Override
  public void setExplicit(boolean isExplicit) {

  }

  @Override
  public void setType(Expression type) {

  }

  @Override
  public DependentLink getNext() {
    return myNext;
  }

  @Override
  public void setNext(DependentLink next) {
    DependentLink last = myNext;
    while (last.getNext() != null) {
      last = last.getNext();
    }
    last.setNext(next);
  }

  @Override
  public DependentLink getNextTyped(List<String> names) {
    DependentLink link = this;
    for (; link instanceof UntypedDependentLink; link = link.getNext()) {
      if (names != null) {
        names.add(link.getName());
      }
    }
    if (names != null) {
      names.add(link.getName());
    }
    return link;
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
