package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.List;

public class TypedDependentLink implements DependentLink {
  private boolean myExplicit;
  private final String myName;
  private Expression myType;
  private DependentLink myNext;

  public TypedDependentLink(boolean isExplicit, String name, Expression type, DependentLink next) {
    assert next != null;
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
  public void setExplicit(boolean isExplicit) {
    myExplicit = isExplicit;
  }

  @Override
  public void setType(Expression type) {
    myType = type;
  }

  @Override
  public DependentLink getNext() {
    return myNext;
  }

  @Override
  public void setNext(DependentLink next) {
    myNext = next;
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
  public TypedDependentLink subst(Substitution subst) {
    TypedDependentLink result = new TypedDependentLink(myExplicit, myName, myType.subst(subst), EmptyDependentLink.getInstance());
    subst.add(this, new ReferenceExpression(result));
    result.myNext = myNext.subst(subst);
    return result;
  }

  @Override
  public DependentLink getNextTyped(List<String> names) {
    if (names != null) {
      names.add(myName);
    }
    return this;
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public String toString() {
    return Binding.Helper.toString(this);
  }
}
