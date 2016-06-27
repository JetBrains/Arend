package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.NamedBinding;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelSubstitution;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition extends NamedBinding implements Referable {
  protected List<Binding> myPolyParams = new ArrayList<>();
  private Abstract.Definition.Precedence myPrecedence;
  private TypeUniverse myUniverse;
  private boolean myHasErrors;
  private final ResolvedName myResolvedName;
  private ClassDefinition myThisClass;

  public Definition(ResolvedName resolvedName, Abstract.Definition.Precedence precedence) {
    super(resolvedName.getName());
    myResolvedName = resolvedName;
    myPrecedence = precedence;
    myUniverse = TypeUniverse.PROP;
    myHasErrors = true;
  }

  public Definition(ResolvedName resolvedName, Abstract.Definition.Precedence precedence, TypeUniverse universe) {
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

  public DefCallExpression getDefCall(LevelSubstitution subst) {
    DefCallExpression defCall = getDefCall();
    defCall.applyLevelSubst(subst);
    return defCall;
  }

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

  public TypeUniverse getUniverse() {
    return myUniverse;
  }

  public void setUniverse(TypeUniverse universe) {
    myUniverse = universe;
  }

  public void setPolyParams(List<Binding> params) {
    myPolyParams = params;
  }

  public List<Binding> getPolyParams() {
    return myPolyParams;
  }

  public Binding getPolyParamByType(Definition typeDef) {
    for (Binding binding : myPolyParams) {
      if (binding.getType().toDefCall().getDefinition().getResolvedName().getFullName().equals(typeDef.getResolvedName().getFullName())) {
        return binding;
      }
    }
    return null;
  }

  public boolean isPolymorphic() { return !myPolyParams.isEmpty(); }

  public abstract Definition substPolyParams(LevelSubstitution subst);

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
