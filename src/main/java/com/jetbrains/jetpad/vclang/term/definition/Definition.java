package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.Callable;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition implements Referable, Callable {
  private final String myName;
  protected List<Binding> myPolyParams = new ArrayList<>();
  private Abstract.Definition.Precedence myPrecedence;
  private boolean myHasErrors;
  private ClassDefinition myThisClass;

  public Definition(String name, Abstract.Definition.Precedence precedence) {
    myName = name;
    myPrecedence = precedence;
    myHasErrors = true;
  }

  @Override
  public String getName() {
    return myName;
  }

  public abstract Type getType();

  @Override
  public Abstract.Definition.Precedence getPrecedence() {
    return myPrecedence;
  }

  public abstract DefCallExpression getDefCall();

  public DefCallExpression getDefCall(LevelSubstitution subst) {
    DefCallExpression defCall = getDefCall();
    if (!hasErrors()) {
      defCall.applyLevelSubst(subst);
    }
    return defCall;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Type getTypeWithThis() {
    return getType();
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  @Deprecated
  public ResolvedName getResolvedName() {
    throw new IllegalStateException();
  }

  public void setPolyParams(List<Binding> params) {
    myPolyParams = params;
  }

  public List<Binding> getPolyParams() {
    return myPolyParams;
  }

  public Binding getPolyParamByType(Definition typeDef) {
    for (Binding binding : myPolyParams) {
      if (binding.getType().toDefCall().getDefinition() == typeDef) {
        return binding;
      }
    }
    return null;
  }

  public boolean isPolymorphic() { return !myPolyParams.isEmpty(); }

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
