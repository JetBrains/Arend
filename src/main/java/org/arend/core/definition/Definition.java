package org.arend.core.definition;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.TCReferable;

import java.util.Collections;
import java.util.List;

public abstract class Definition implements Variable {
  private TCReferable myReferable;
  private TypeCheckingStatus myStatus;
  private boolean myHasUniverses;

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

  public List<Integer> getParametersTypecheckingOrder() {
    return null;
  }

  public void setParametersTypecheckingOrder(List<Integer> order) {

  }

  public List<Boolean> getGoodThisParameters() {
    return Collections.emptyList();
  }

  public boolean isGoodParameter(int index) {
    List<Boolean> goodParameters = getGoodThisParameters();
    return index < goodParameters.size() && goodParameters.get(index);
  }

  public void setGoodThisParameters(List<Boolean> goodThisParameters) {

  }

  public boolean hasUniverses() {
    return myHasUniverses;
  }

  public void setHasUniverses(boolean hasUniverses) {
    myHasUniverses = hasUniverses;
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
      return this != HEADER_NEEDS_TYPE_CHECKING && this != BODY_NEEDS_TYPE_CHECKING;
    }

    public boolean needsTypeChecking() {
      return this == HEADER_HAS_ERRORS || this == HEADER_NEEDS_TYPE_CHECKING || this == BODY_NEEDS_TYPE_CHECKING || this == MAY_BE_TYPE_CHECKED_WITH_ERRORS || this == MAY_BE_TYPE_CHECKED_WITH_WARNINGS;
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
}
