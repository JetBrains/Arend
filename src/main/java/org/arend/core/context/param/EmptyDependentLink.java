package org.arend.core.context.param;

import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;

import javax.annotation.Nonnull;
import java.util.List;

public class EmptyDependentLink implements SingleDependentLink {
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

  @Nonnull
  @Override
  public SingleDependentLink getNext() {
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
  public EmptyDependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
    return this;
  }

  @Override
  public TypedSingleDependentLink getNextTyped(List<String> names) {
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

  @Override
  public void strip(StripVisitor stripVisitor) {
    throw new IllegalStateException();
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    throw new IllegalStateException();
  }
}
