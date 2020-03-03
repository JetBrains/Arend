package org.arend.core.context.param;

import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.SubstVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UntypedSingleDependentLink extends UntypedDependentLink implements SingleDependentLink {
  public UntypedSingleDependentLink(String name, SingleDependentLink next) {
    super(name, next);
  }

  public UntypedSingleDependentLink(String name) {
    super(name);
  }

  @NotNull
  @Override
  public SingleDependentLink getNext() {
    return (SingleDependentLink) myNext;
  }

  @Override
  public TypedSingleDependentLink getNextTyped(List<String> names) {
    return (TypedSingleDependentLink) super.getNextTyped(names);
  }

  @Override
  public SingleDependentLink subst(SubstVisitor substVisitor, int size, boolean updateSubst) {
    if (size == 1) {
      TypedSingleDependentLink result = new TypedSingleDependentLink(isExplicit(), getName(), getType().subst(substVisitor));
      if (updateSubst) {
        substVisitor.getExprSubstitution().addSubst(this, new ReferenceExpression(result));
      } else {
        substVisitor.getExprSubstitution().add(this, new ReferenceExpression(result));
      }
      return result;
    } else
    if (size > 0) {
      UntypedSingleDependentLink result = new UntypedSingleDependentLink(getName());
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
  public void setNext(DependentLink next) {
    if (!(next instanceof SingleDependentLink)) {
      throw new IllegalStateException();
    }
    super.setNext(next);
  }
}
