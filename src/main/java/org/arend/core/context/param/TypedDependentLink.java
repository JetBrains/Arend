package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;

import javax.annotation.Nonnull;
import java.util.List;

public class TypedDependentLink implements DependentLink {
  private boolean myExplicit;
  private String myName;
  private Type myType;
  private DependentLink myNext;
  private final boolean myHidden;

  public TypedDependentLink(boolean isExplicit, String name, Type type, boolean isHidden, DependentLink next) {
    assert next != null;
    myExplicit = isExplicit;
    myName = name;
    myType = type;
    myNext = next;
    myHidden = isHidden;
  }

  public TypedDependentLink(boolean isExplicit, String name, Type type, DependentLink next) {
    assert next != null;
    myExplicit = isExplicit;
    myName = name;
    myType = type;
    myNext = next;
    myHidden = false;
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
  public void setType(Type type) {
    myType = type;
  }

  @Nonnull
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
  public String getName() {
    return myName;
  }

  @Override
  public Type getType() {
    return myType;
  }

  @Override
  public DependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
    if (size > 0) {
      TypedDependentLink result = new TypedDependentLink(myExplicit, myName, myType.subst(substVisitor), myHidden, EmptyDependentLink.getInstance());
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
  public TypedDependentLink getNextTyped(List<String> names) {
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
  public void strip(StripVisitor stripVisitor) {
    myType = myType.strip(stripVisitor);
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    myType.subst(substVisitor);
  }

  @Override
  public boolean isHidden() {
    return myHidden;
  }

  @Override
  public String toString() {
    return DependentLink.toString(this);
  }
}
