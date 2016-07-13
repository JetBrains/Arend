package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Definition extends NamedBinding implements Referable {
  private Abstract.Definition.Precedence myPrecedence;
  private TypeUniverse myUniverse;
  private boolean myHasErrors;
  private ClassDefinition myThisClass;

  public Definition(String name, Abstract.Definition.Precedence precedence) {
    this(name, precedence, TypeUniverse.PROP);
  }

  public Definition(String name, Abstract.Definition.Precedence precedence, TypeUniverse universe) {
    super(name);
    myPrecedence = precedence;
    myUniverse = universe;
    myHasErrors = true;
  }

  @Override
  public Abstract.Definition.Precedence getPrecedence() {
    return myPrecedence;
  }

  public abstract DefCallExpression getDefCall();

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Expression getTypeWithThis() {
    return getType();
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  @Deprecated
  public ResolvedName getResolvedName() {
    throw new IllegalStateException();
  }

  public TypeUniverse getUniverse() {
    return myUniverse;
  }

  public void setUniverse(TypeUniverse universe) {
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

  public Namespace getOwnNamespace() {
    return EmptyNamespace.INSTANCE;
  }

  public Namespace getInstanceNamespace() {
    return EmptyNamespace.INSTANCE;
  }
}
