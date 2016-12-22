package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition implements Referable {
  protected List<LevelBinding> myPolyParams = new ArrayList<>();
  private ClassDefinition myThisClass;
  private Abstract.Definition myAbstractDefinition;

  public Definition(Abstract.Definition abstractDef) {
    myAbstractDefinition = abstractDef;
  }

  public String getName() {
    return myAbstractDefinition.getName();
  }

  public Abstract.Definition getAbstractDefinition() {
    return myAbstractDefinition;
  }

  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public abstract TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments);

  public abstract DefCallExpression getDefCall(LevelArguments polyArguments);

  public abstract Expression getDefCall(LevelArguments polyArguments, List<Expression> args);

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  public void setPolyParams(List<LevelBinding> params) {
    myPolyParams = params;
  }

  public List<LevelBinding> getPolyParams() {
    return myPolyParams;
  }

  public enum TypeCheckingStatus { HAS_ERRORS, NO_ERRORS, TYPE_CHECKING }

  // typeHasErrors should imply hasErrors != NO_ERRORS
  public abstract boolean typeHasErrors();
  public abstract TypeCheckingStatus hasErrors();
  public abstract void hasErrors(TypeCheckingStatus status);
}
