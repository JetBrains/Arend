package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public abstract class Definition extends Binding implements Abstract.Definition {
  private Precedence myPrecedence;
  private Universe myUniverse;
  private boolean myHasErrors;
  private final Namespace myParentNamespace;
  private ClassDefinition myThisClass;

  public Definition(Namespace parentNamespace, Name name, Precedence precedence) {
    super(name);
    myParentNamespace = parentNamespace;
    myPrecedence = precedence;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
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
    return new ResolvedName(myParentNamespace, getName().name);
  }

  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
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

  @Override
  public Abstract.DefineStatement getParentStatement() {
    throw new IllegalStateException();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }

  @Override
  public Definition lift(int on) {
    return this;
  }
}
