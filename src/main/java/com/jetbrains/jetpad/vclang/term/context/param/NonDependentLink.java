package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.List;

public class NonDependentLink implements DependentLink {
  private boolean myExplicit;
  private Expression myType;
  private DependentLink myNext;

  public NonDependentLink(Expression type, DependentLink next) {
    assert next != null;
    myExplicit = true;
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
    return null;
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
  public NonDependentLink subst(Substitution subst) {
    return new NonDependentLink(myType.subst(subst), myNext.subst(subst));
  }

  @Override
  public DependentLink getNextTyped(List<String> names) {
    return this;
  }

  @Override
  public boolean hasNext() {
    return true;
  }
}
