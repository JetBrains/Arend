package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public abstract class Definition extends NamedBinding implements Referable {
  private final Abstract.Definition.Fixity myFixity;
  private Abstract.Definition.Precedence myPrecedence;
  private Universe myUniverse;
  private boolean myHasErrors;
  private final ResolvedName myResolvedName;
  private ClassDefinition myThisClass;

  public Definition(ResolvedName resolvedName, Abstract.Definition.Precedence precedence) {
    super(resolvedName.getName());
    myResolvedName = resolvedName;
    myFixity = new Name(resolvedName.getName()).fixity;
    myPrecedence = precedence;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
  }

  public Abstract.Definition.Fixity getFixity() {
    return myFixity;
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

  public Universe getUniverse() {
    return myUniverse;
  }

  public void setUniverse(Universe universe) {
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
