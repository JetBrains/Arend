package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;

import javax.annotation.Nonnull;
import java.util.*;

public class DataDefinition extends Definition implements CoreDataDefinition {
  private final List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Sort mySort;
  private boolean myTruncated;
  private boolean mySquashed;
  private FunctionDefinition mySquasher;
  private final Set<Integer> myCovariantParameters = new HashSet<>();
  private final CoerceData myCoerce = new CoerceData(this);
  private List<Integer> myParametersTypecheckingOrder;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();
  private ParametersLevels<ParametersLevel> myParametersLevels = new ParametersLevels<>();

  public DataDefinition(TCReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
    myConstructors = new ArrayList<>();
    myParameters = EmptyDependentLink.getInstance();
  }

  @Override
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
    return myCovariantParameters.contains(index);
  }

  public void setCovariant(int index, boolean covariant) {
    if (covariant) {
      myCovariantParameters.add(index);
    } else {
      myCovariantParameters.remove(index);
    }
  }

  @Override
  public boolean isTruncated() {
    return myTruncated;
  }

  public void setTruncated(boolean value) {
    myTruncated = value;
  }

  public boolean isSquashed() {
    return mySquashed;
  }

  public void setSquashed(boolean value) {
    mySquashed = value;
  }

  public FunctionDefinition getSquasher() {
    return mySquasher;
  }

  public void setSquasher(FunctionDefinition squasher) {
    mySquasher = squasher;
  }

  @Nonnull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  @Nonnull
  @Override
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
  public List<TypeClassParameterKind> getTypeClassParameters() {
    return myTypeClassParameters;
  }

  @Override
  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  @Override
  public List<? extends ParametersLevel> getParametersLevels() {
    return myParametersLevels.getList();
  }

  public void addParametersLevel(ParametersLevel parametersLevel) {
    myParametersLevels.add(parametersLevel);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    if (this == Prelude.PATH && sortArgument.isProp()) {
      Sort sort = Sort.SetOfLevel(sortArgument.getPLevel());
      TypedDependentLink param = new TypedDependentLink(true, myParameters.getName(), new PiExpression(sort.succ(), new TypedSingleDependentLink(true, null, ExpressionFactory.Interval()), new UniverseExpression(sort)), EmptyDependentLink.getInstance());
      TypedDependentLink param3 = new TypedDependentLink(true, myParameters.getNext().getNext().getName(), new TypeExpression(AppExpression.make(new ReferenceExpression(param), ExpressionFactory.Right()), sort), EmptyDependentLink.getInstance());
      TypedDependentLink param2 = new TypedDependentLink(true, myParameters.getNext().getName(), new TypeExpression(AppExpression.make(new ReferenceExpression(param), ExpressionFactory.Left()), sort), param3);
      param.setNext(param2);

      params.add(param);
      params.add(param2);
      params.add(param3);
      return new UniverseExpression(Sort.PROP);
    }

    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return new UniverseExpression(mySort.subst(polySubst));
  }

  @Override
  public DataCallExpression getDefCall(Sort sortArgument, List<Expression> arguments) {
    return new DataCallExpression(this, sortArgument, arguments);
  }

  @Override
  public void fill() {
    if (myParameters == null) {
      myParameters = EmptyDependentLink.getInstance();
    }
    if (mySort == null) {
      mySort = Sort.PROP;
    }
    for (Constructor constructor : myConstructors) {
      constructor.fill();
    }
  }
}
