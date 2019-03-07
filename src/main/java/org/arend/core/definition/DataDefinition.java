package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCReferable;

import java.util.*;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Sort mySort;
  private boolean myMatchesOnInterval;
  private boolean myIsTruncated;
  private Set<Integer> myCovariantParameters;
  private final CoerceData myCoerce = new CoerceData(this);
  private List<Integer> myParametersTypecheckingOrder;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<Boolean> myTypeClassParameters = Collections.emptyList();
  private List<ParametersLevel> myParametersLevels = Collections.emptyList();

  public DataDefinition(TCReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myConstructors = new ArrayList<>();
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  @Override
  public CoerceData getCoerceData() {
    return myCoerce;
  }

  public boolean isCovariant(int index) {
    return myCovariantParameters != null && myCovariantParameters.contains(index);
  }

  public void setCovariant(int index) {
    if (myCovariantParameters == null) {
      myCovariantParameters = new HashSet<>();
    }
    myCovariantParameters.add(index);
  }

  public boolean isTruncated() {
    return myIsTruncated;
  }

  public void setIsTruncated(boolean value) {
    myIsTruncated = value;
  }

  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public Constructor getConstructor(GlobalReferable referable) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getReferable().equals(referable)) {
        return constructor;
      }
    }
    return null;
  }

  public Constructor getConstructor(String name) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getName().equals(name)) {
        return constructor;
      }
    }
    return null;
  }

  public void addConstructor(Constructor constructor) {
    myConstructors.add(constructor);
  }

  public boolean matchesOnInterval() { return myMatchesOnInterval; }

  public void setMatchesOnInterval() { myMatchesOnInterval = true; }

  public boolean hasIndexedConstructors() {
    for (Constructor constructor : myConstructors) {
      if (constructor.getPatterns() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<Integer> getParametersTypecheckingOrder() {
    return myParametersTypecheckingOrder;
  }

  @Override
  public void setParametersTypecheckingOrder(List<Integer> order) {
    myParametersTypecheckingOrder = order;
  }

  @Override
  public List<Boolean> getGoodThisParameters() {
    return myGoodThisParameters;
  }

  @Override
  public void setGoodThisParameters(List<Boolean> goodThisParameters) {
    myGoodThisParameters = goodThisParameters;
  }

  @Override
  public List<Boolean> getTypeClassParameters() {
    return myTypeClassParameters;
  }

  @Override
  public void setTypeClassParameters(List<Boolean> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  @Override
  public List<? extends ParametersLevel> getParametersLevels() {
    return myParametersLevels;
  }

  public void addLevelParameters(ParametersLevel parametersLevel) {
    if (myParametersLevels.isEmpty()) {
      myParametersLevels = new ArrayList<>();
    }
    myParametersLevels.add(parametersLevel);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return new UniverseExpression(mySort.subst(polySubst));
  }

  @Override
  public DataCallExpression getDefCall(Sort sortArgument, List<Expression> arguments) {
    return new DataCallExpression(this, sortArgument, arguments);
  }
}
