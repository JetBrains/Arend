package com.jetbrains.jetpad.vclang.term.context.param;

import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

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
  public void setType(Type type) {
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
  public DependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size) {
    return this;
  }

  @Override
  public TypedDependentLink getNextTyped(List<String> names) {
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
  public Type getType() {
    throw new IllegalStateException();
  }

}
