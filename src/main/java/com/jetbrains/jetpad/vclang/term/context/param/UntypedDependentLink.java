package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.List;

public class UntypedDependentLink implements DependentLink {
  private String myName;
  private DependentLink myNext;

  public UntypedDependentLink(String name, DependentLink next) {
    assert next instanceof UntypedDependentLink || next instanceof TypedDependentLink;
    myName = name;
    myNext = next;
  }

  private UntypedDependentLink(String name) {
    myName = name;
    myNext = EmptyDependentLink.getInstance();
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
  public void setName(String name) {
    myName = name;
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
  public DependentLink subst(Substitution subst, int size) {
    if (size == 1) {
      TypedDependentLink result = new TypedDependentLink(isExplicit(), myName, getType(), EmptyDependentLink.getInstance());
      subst.add(this, new ReferenceExpression(result));
      return result;
    } else
    if (size > 0) {
      UntypedDependentLink result = new UntypedDependentLink(myName);
      subst.add(this, new ReferenceExpression(result));
      result.myNext = myNext.subst(subst, size - 1);
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }

  @Override
  public String toString() {
    return Binding.Helper.toString(this);
  }
}
