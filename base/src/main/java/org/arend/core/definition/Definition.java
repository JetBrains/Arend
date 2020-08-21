package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.extImpl.userData.UserDataHolderImpl;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class Definition extends UserDataHolderImpl implements CoreDefinition {
  private final TCReferable myReferable;
  private TypeCheckingStatus myStatus;
  private UniverseKind myUniverseKind = UniverseKind.NO_UNIVERSES;

  public Definition(TCReferable referable, TypeCheckingStatus status) {
    myReferable = referable;
    myStatus = status;
  }

  @NotNull
  @Override
  public String getName() {
    return myReferable.textRepresentation();
  }

  @NotNull
  @Override
  public TCReferable getRef() {
    return myReferable;
  }

  public TCReferable getReferable() {
    return myReferable;
  }

  @Override
  public @NotNull Set<? extends Definition> getRecursiveDefinitions() {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public boolean hasStrictParameters() {
    return false;
  }

  public boolean isStrict(int parameter) {
    return false;
  }

  protected boolean hasEnclosingClass() {
    return false;
  }

  public ClassDefinition getEnclosingClass() {
    if (hasEnclosingClass()) {
      DependentLink parameters = getParameters();
      if (!parameters.hasNext()) {
        return null;
      }
      Expression type = parameters.getTypeExpr();
      return type instanceof ClassCallExpression ? ((ClassCallExpression) type).getDefinition() : null;
    } else {
      return null;
    }
  }

  public abstract Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument);

  public abstract Expression getDefCall(Sort sortArgument, List<Expression> args);

  public CoerceData getCoerceData() {
    return null;
  }

  public int getVisibleParameter() {
    return -1;
  }

  public boolean isHideable() {
    return getVisibleParameter() >= 0;
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

  public enum TypeClassParameterKind { NO, YES, ONLY_LOCAL }

  public List<TypeClassParameterKind> getTypeClassParameters() {
    return Collections.emptyList();
  }

  public TypeClassParameterKind getTypeClassParameterKind(int index) {
    List<TypeClassParameterKind> typeClassParameters = getTypeClassParameters();
    return index < typeClassParameters.size() ? typeClassParameters.get(index) : TypeClassParameterKind.NO;
  }

  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {

  }

  public UniverseKind getUniverseKind() {
    return myUniverseKind;
  }

  public void setUniverseKind(UniverseKind kind) {
    myUniverseKind = kind;
  }

  public List<? extends ParametersLevel> getParametersLevels() {
    return Collections.emptyList();
  }

  public enum TypeCheckingStatus {
    HAS_ERRORS, HAS_WARNINGS, DEP_PROBLEMS, NO_ERRORS, TYPE_CHECKING, NEEDS_TYPE_CHECKING;

    public boolean isOK() {
      return this.ordinal() >= DEP_PROBLEMS.ordinal();
    }

    public boolean headerIsOK() {
      return this != NEEDS_TYPE_CHECKING;
    }

    public boolean hasErrors() {
      return this == HAS_ERRORS;
    }

    public boolean hasDepProblems() {
      return hasErrors() || this == HAS_WARNINGS || this == DEP_PROBLEMS;
    }

    public boolean needsTypeChecking() {
      return this == NEEDS_TYPE_CHECKING;
    }

    public boolean withoutErrors() {
      return this.ordinal() >= HAS_WARNINGS.ordinal();
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

  public void addStatus(TypeCheckingStatus status) {
    myStatus = myStatus.needsTypeChecking() && !status.needsTypeChecking() ? status : myStatus.max(status);
  }

  public abstract void fill();

  @Override
  public String toString() {
    return myReferable.toString();
  }
}
