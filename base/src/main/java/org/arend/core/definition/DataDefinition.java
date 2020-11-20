package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DataDefinition extends Definition implements CoreDataDefinition {
  private final List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Sort mySort = Sort.SET0;
  private int myTruncatedLevel = -2;
  private boolean mySquashed;
  private FunctionDefinition mySquasher;
  private final Set<Integer> myCovariantParameters = new HashSet<>();
  private final CoerceData myCoerce = new CoerceData(this);
  private List<Integer> myParametersTypecheckingOrder;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();
  private final ParametersLevels<ParametersLevel> myParametersLevels = new ParametersLevels<>();
  private Set<Definition> myRecursiveDefinitions = Collections.emptySet();
  private boolean myHasEnclosingClass;

  public DataDefinition(TCDefReferable referable) {
    super(referable, TypeCheckingStatus.NEEDS_TYPE_CHECKING);
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
  public @NotNull Set<? extends Definition> getRecursiveDefinitions() {
    return myRecursiveDefinitions;
  }

  public void setRecursiveDefinitions(Set<Definition> recursiveDefinitions) {
    myRecursiveDefinitions = recursiveDefinitions;
  }

  public boolean isHIT() {
    for (Constructor constructor : myConstructors) {
      if (constructor.getBody() instanceof IntervalElim) {
        return true;
      }
    }
    return false;
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
  public int getTruncatedLevel() {
    return myTruncatedLevel;
  }

  public void setTruncatedLevel(int level) {
    myTruncatedLevel = level;
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

  @NotNull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  @Override
  protected boolean hasEnclosingClass() {
    return myHasEnclosingClass;
  }

  public void setHasEnclosingClass(boolean hasEnclosingClass) {
    myHasEnclosingClass = hasEnclosingClass;
  }

  @NotNull
  @Override
  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  @Override
  public CoreConstructor findConstructor(@NotNull String name) {
    return getConstructor(name);
  }

  public Constructor getConstructor(GlobalReferable referable) {
    var constructors = this == Prelude.FIN ? Prelude.NAT.getConstructors() : myConstructors;
    for (Constructor constructor : constructors) {
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

    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, new ExprSubstitution(), polySubst)));
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
