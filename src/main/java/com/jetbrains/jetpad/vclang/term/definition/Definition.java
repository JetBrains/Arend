package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Definition extends NamedBinding implements Referable {
  private Abstract.Definition.Precedence myPrecedence;
  private TypeUniverseNew myUniverse;
  private boolean myHasErrors;
  private final ResolvedName myResolvedName;
  private ClassDefinition myThisClass;

  public Definition(ResolvedName resolvedName, Abstract.Definition.Precedence precedence) {
    super(resolvedName.getName());
    myResolvedName = resolvedName;
    myPrecedence = precedence;
    myUniverse = TypeUniverseNew.PROP;
    myHasErrors = true;
  }

  public Definition(ResolvedName resolvedName, Abstract.Definition.Precedence precedence, TypeUniverseNew universe) {
    super(resolvedName.getName());
    myResolvedName = resolvedName;
    myPrecedence = precedence;
    myUniverse = universe;
    myHasErrors = true;
  }

  @Override
  public Abstract.Definition.Precedence getPrecedence() {
    return myPrecedence;
  }

  public Namespace getParentNamespace() {
    return myResolvedName.getParent() == null ? null : myResolvedName.getParent().toNamespace();
  }

  public abstract DefCallExpression getDefCall();

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Expression getTypeWithThis() {
    return getType();
  }

  public void setThisClass(ClassDefinition thisClass) {
    myThisClass = thisClass;
  }

  public ResolvedName getResolvedName() {
    return myResolvedName;
  }

  public TypeUniverseNew getUniverse() {
    return myUniverse;
  }

  public void setUniverse(TypeUniverseNew universe) {
    myUniverse = universe;
  }

  public boolean hasErrors() {
    return myHasErrors;
  }

  public void hasErrors(boolean has) {
    myHasErrors = has;
  }

  public boolean isAbstract() {
    return false;
  }
}
