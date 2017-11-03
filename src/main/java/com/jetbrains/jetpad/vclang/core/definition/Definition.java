package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.List;

public abstract class Definition implements Variable {
  private ClassDefinition myThisClass;
  private GlobalReferable myReferable;
  private TypeCheckingStatus myStatus;

  public Definition(GlobalReferable referable, TypeCheckingStatus status) {
    myReferable = referable;
    myStatus = status;
  }

  @Override
  public String getName() {
    return myReferable.textRepresentation();
  }

  public GlobalReferable getReferable() {
    return myReferable;
  }

  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public abstract Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument);

  public abstract Expression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args);

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  public enum TypeCheckingStatus {
    HEADER_HAS_ERRORS, BODY_HAS_ERRORS, HEADER_NEEDS_TYPE_CHECKING, BODY_NEEDS_TYPE_CHECKING, HAS_ERRORS, NO_ERRORS;

    public boolean bodyIsOK() {
      return this == HAS_ERRORS || this == NO_ERRORS;
    }

    public boolean headerIsOK() {
      return this != HEADER_HAS_ERRORS && this != HEADER_NEEDS_TYPE_CHECKING;
    }

    public boolean needsTypeChecking() {
      return this != NO_ERRORS;
    }
  }

  public TypeCheckingStatus status() {
    return myStatus;
  }

  public void setStatus(TypeCheckingStatus status) {
    myStatus = status;
  }

  @Override
  public String toString() {
    return myReferable.toString();
  }

  public static Definition newDefinition(Concrete.Definition definition) {
    if (definition instanceof Concrete.DataDefinition) {
      return new DataDefinition(definition.getData());
    }
    if (definition instanceof Concrete.FunctionDefinition || definition instanceof Concrete.Instance) {
      return new FunctionDefinition(definition.getData());
    }
    if (definition instanceof Concrete.ClassDefinition) {
      return new ClassDefinition(definition.getData());
    }
    if (definition instanceof Concrete.ClassView) {
      return new ClassDefinition(definition.getData()); // TODO[classes]
    }
    throw new IllegalStateException();
  }
}
