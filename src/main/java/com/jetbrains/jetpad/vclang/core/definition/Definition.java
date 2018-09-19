package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.List;

public abstract class Definition implements Variable {
  private TCReferable myReferable;
  private TypeCheckingStatus myStatus;

  public Definition(TCReferable referable, TypeCheckingStatus status) {
    myReferable = referable;
    myStatus = status;
  }

  @Override
  public String getName() {
    return myReferable.textRepresentation();
  }

  public TCReferable getReferable() {
    return myReferable;
  }

  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public abstract Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument);

  public abstract Expression getDefCall(Sort sortArgument, List<Expression> args);

  public CoerceData getCoerceData() {
    return null;
  }

  public enum TypeCheckingStatus {
    HEADER_NEEDS_TYPE_CHECKING, HEADER_HAS_ERRORS, BODY_NEEDS_TYPE_CHECKING, BODY_HAS_ERRORS, MAY_BE_TYPE_CHECKED_WITH_ERRORS, HAS_ERRORS, MAY_BE_TYPE_CHECKED_WITH_WARNINGS, HAS_WARNINGS, NO_ERRORS;

    public boolean bodyIsOK() {
      return headerIsOK() && this != BODY_HAS_ERRORS && this != BODY_NEEDS_TYPE_CHECKING;
    }

    public boolean headerIsOK() {
      return this != HEADER_HAS_ERRORS && this != HEADER_NEEDS_TYPE_CHECKING;
    }

    public boolean isTypeChecked() {
      return this != HEADER_HAS_ERRORS && this != HEADER_NEEDS_TYPE_CHECKING && this != BODY_NEEDS_TYPE_CHECKING;
    }

    public boolean needsTypeChecking() {
      return this == HEADER_NEEDS_TYPE_CHECKING || this == BODY_NEEDS_TYPE_CHECKING || this == MAY_BE_TYPE_CHECKED_WITH_ERRORS || this == MAY_BE_TYPE_CHECKED_WITH_WARNINGS;
    }

    public TypeCheckingStatus max(TypeCheckingStatus status) {
      return ordinal() <= status.ordinal() ? this : status;
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
      return new ClassDefinition((TCClassReferable) definition.getData());
    }
    throw new IllegalStateException();
  }
}
