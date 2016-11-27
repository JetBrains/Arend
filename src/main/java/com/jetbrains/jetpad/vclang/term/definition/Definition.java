package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition implements Referable {
  protected List<TypedBinding> myPolyParams = new ArrayList<>();
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

  public void setPolyParams(List<TypedBinding> params) {
    myPolyParams = params;
  }

  public List<TypedBinding> getPolyParams() {
    return myPolyParams;
  }

  public TypedBinding getPolyParamByType(Definition typeDef) {
    for (TypedBinding binding : myPolyParams) {
      if (binding.getType().toExpression().toDefCall().getDefinition() == typeDef) {
        return binding;
      }
    }
    return null;
  }

  public enum TypeCheckingStatus { HAS_ERRORS, NO_ERRORS, TYPE_CHECKING }

  // typeHasErrors should imply hasErrors == HAS_ERRORS
  public abstract boolean typeHasErrors();
  public abstract TypeCheckingStatus hasErrors();
  public abstract void hasErrors(TypeCheckingStatus status);
}
