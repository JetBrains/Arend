package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.List;

public class UntypedDependentLink implements DependentLink {
  private final String myName;
  private DependentLink myNext;

  public UntypedDependentLink(String name, DependentLink next) {
    assert next instanceof UntypedDependentLink || next instanceof TypedDependentLink;
    myName = name;
    myNext = next;
  }

  private UntypedDependentLink(String name) {
    myName = name;
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
    while (last.getNext().hasNext()) {
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
  public boolean hasNext() {
    return true;
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
  public UntypedDependentLink subst(Substitution subst) {
    UntypedDependentLink result = new UntypedDependentLink(myName);
    subst.addMapping(this, new ReferenceExpression(result));
    result.myNext = myNext.subst(subst);
    return result;
  }
}
