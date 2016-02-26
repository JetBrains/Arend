package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.List;

public class EmptyDependentLink implements DependentLink {
  private final static EmptyDependentLink INSTANCE = new EmptyDependentLink();

  private EmptyDependentLink() {}

  public static EmptyDependentLink getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean isExplicit() {
    return true;
  }

  @Override
  public void setExplicit(boolean isExplicit) {
    throw new IllegalStateException();
  }

  @Override
  public void setType(Expression type) {
    throw new IllegalStateException();
  }

  @Override
  public DependentLink getNext() {
    throw new IllegalStateException();
  }

  @Override
  public void setNext(DependentLink next) {
    throw new IllegalStateException();
  }

  @Override
  public void setName(String name) {
    throw new IllegalStateException();
  }

  @Override
  public DependentLink subst(Substitution subst, int size) {
    return this;
  }

  @Override
  public DependentLink getNextTyped(List<String> names) {
    throw new IllegalStateException();
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public String getName() {
    throw new IllegalStateException();
  }

  @Override
  public Expression getType() {
    throw new IllegalStateException();
  }

}
