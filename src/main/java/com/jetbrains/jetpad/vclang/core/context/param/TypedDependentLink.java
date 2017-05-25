package com.jetbrains.jetpad.vclang.core.context.param;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.List;

public class TypedDependentLink implements DependentLink {
  private boolean myExplicit;
  private String myName;
  private Type myType;
  private DependentLink myNext;

  public TypedDependentLink(boolean isExplicit, String name, Type type, DependentLink next) {
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
  public void setType(Type type) {
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
  public DependentLink subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, int size) {
    if (size > 0) {
      TypedDependentLink result = new TypedDependentLink(myExplicit, myName, myType.subst(exprSubst, levelSubst), EmptyDependentLink.getInstance());
      exprSubst.add(this, new ReferenceExpression(result));
      result.myNext = myNext.subst(exprSubst, levelSubst, size - 1);
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
  public String toString() {
    return Binding.Helper.toString(this);
  }
}
