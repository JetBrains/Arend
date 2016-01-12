package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.param.NamedBinding;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public abstract class Definition extends NamedBinding {
  private Abstract.Definition.Precedence myPrecedence;
  private Universe myUniverse;
  private boolean myHasErrors;
  private final Namespace myParentNamespace;
  private ClassDefinition myThisClass;

  public Definition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence) {
    super(name);
    myParentNamespace = parentNamespace;
    myPrecedence = precedence;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
  }

  public Abstract.Definition.Precedence getPrecedence() {
    return myPrecedence;
  }

  public Namespace getParentNamespace() {
    return myParentNamespace;
  }

  public abstract Expression getBaseType();

  public abstract DefCallExpression getDefCall();

  @Override
  public Expression getType() {
    Expression baseType = getBaseType();
    return myThisClass != null && baseType != null ? Pi("\\this", ClassCall(myThisClass), baseType) : baseType;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public void setThisClass(ClassDefinition thisClass) {
    myThisClass = thisClass;
  }

  public ResolvedName getResolvedName() {
    return new ResolvedName(myParentNamespace, getName());
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
