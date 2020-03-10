package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UntypedDependentLink implements DependentLink {
  private String myName;
  DependentLink myNext;

  public UntypedDependentLink(String name, DependentLink next) {
    assert next instanceof UntypedDependentLink || next instanceof TypedDependentLink;
    myName = name;
    myNext = next;
  }

  public UntypedDependentLink(String name) {
    myName = name;
    myNext = EmptyDependentLink.getInstance();
  }

  @Override
  public @NotNull Type getType() {
    return myNext.getType();
  }

  @Override
  public boolean isExplicit() {
    return myNext.isExplicit();
  }

  @Override
  public void setExplicit(boolean isExplicit) {

  }

  @Override
  public void setType(Type type) {

  }

  @NotNull
  @Override
  public DependentLink getNext() {
    return myNext;
  }

  @Override
  public void setNext(DependentLink next) {
    myNext = next;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public TypedDependentLink getNextTyped(List<String> names) {
    DependentLink link = this;
    for (; link instanceof UntypedDependentLink; link = link.getNext()) {
      if (names != null) {
        names.add(link.getName());
      }
    }
    if (names != null) {
      names.add(link.getName());
    }
    return (TypedDependentLink) link;
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
  public DependentLink subst(@NotNull SubstVisitor substVisitor, int size, boolean updateSubst) {
    if (size == 1) {
      TypedDependentLink result = new TypedDependentLink(isExplicit(), myName, getType().subst(substVisitor), EmptyDependentLink.getInstance());
      if (updateSubst) {
        substVisitor.getExprSubstitution().addSubst(this, new ReferenceExpression(result));
      } else {
        substVisitor.getExprSubstitution().add(this, new ReferenceExpression(result));
      }
      return result;
    } else
    if (size > 0) {
      UntypedDependentLink result = new UntypedDependentLink(myName);
      if (updateSubst) {
        substVisitor.getExprSubstitution().addSubst(this, new ReferenceExpression(result));
      } else {
        substVisitor.getExprSubstitution().add(this, new ReferenceExpression(result));
      }
      result.myNext = myNext.subst(substVisitor, size - 1, updateSubst);
      return result;
    } else {
      return EmptyDependentLink.getInstance();
    }
  }

  @Override
  public String toString() {
    return DependentLink.toString(this);
  }

  @Override
  public void strip(StripVisitor stripVisitor) {

  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {

  }
}
