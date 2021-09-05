package org.arend.core.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.core.subst.ListLevels;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.extImpl.userData.UserDataHolderImpl;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class Definition extends UserDataHolderImpl implements CoreDefinition {
  private final TCDefReferable myReferable;
  private TypeCheckingStatus myStatus;
  private UniverseKind myUniverseKind = UniverseKind.NO_UNIVERSES;

  public Definition(TCDefReferable referable, TypeCheckingStatus status) {
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
  public TCDefReferable getRef() {
    return myReferable;
  }

  public TCDefReferable getReferable() {
    return myReferable;
  }

  @Override
  public @NotNull Set<? extends Definition> getRecursiveDefinitions() {
    return Collections.emptySet();
  }

  public abstract List<? extends LevelVariable> getLevelParameters();

  public int getNumberOfPLevelParameters() {
    List<? extends LevelVariable> vars = getLevelParameters();
    if (vars == null) return 1;
    int result = 0;
    for (LevelVariable param : vars) {
      if (param.getType() != LevelVariable.LvlType.PLVL) {
        break;
      }
      result++;
    }
    return result;
  }

  public boolean isIdLevels(Levels levels) {
    LevelSubstitution subst = levels.makeSubstitution(this);
    List<? extends LevelVariable> vars = getLevelParameters();
    if (vars == null) {
      Level pLevel = (Level) subst.get(LevelVariable.PVAR);
      Level hLevel = (Level) subst.get(LevelVariable.HVAR);
      return pLevel != null && pLevel.isVarOnly() && pLevel.getVar().equals(LevelVariable.PVAR) && hLevel != null && hLevel.isVarOnly() && hLevel.getVar().equals(LevelVariable.HVAR);
    } else {
      for (LevelVariable var : vars) {
        Level level = (Level) subst.get(var);
        if (!(level != null && level.isVarOnly() && level.getVar().equals(var))) {
          return false;
        }
      }
      return true;
    }
  }

  public Levels makeIdLevels() {
    List<? extends LevelVariable> vars = getLevelParameters();
    if (vars == null) return LevelPair.STD;
    List<Level> result = new ArrayList<>(vars.size());
    for (LevelVariable var : vars) {
      result.add(new Level(var));
    }
    return new ListLevels(result);
  }

  public Levels makeMinLevels() {
    List<? extends LevelVariable> vars = getLevelParameters();
    if (vars == null) return LevelPair.PROP;
    List<Level> result = new ArrayList<>(vars.size());
    for (LevelVariable var : vars) {
      result.add(new Level(var.getMinValue()));
    }
    return new ListLevels(result);
  }

  public Levels makeLevelsFromList(List<Level> levels) {
    return getLevelParameters() == null ? new LevelPair(levels.get(0), levels.get(1)) : new ListLevels(levels);
  }

  public Levels generateInferVars(Equations equations, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    List<? extends LevelVariable> vars = getLevelParameters();
    if (vars == null) return LevelPair.generateInferVars(equations, isUniverseLike, sourceNode);
    List<Level> result = new ArrayList<>(vars.size());
    for (LevelVariable var : vars) {
      InferenceLevelVariable infVar = new InferenceLevelVariable(var.getType(), isUniverseLike, sourceNode);
      equations.addVariable(infVar);
      result.add(new Level(infVar));
    }
    return new ListLevels(result);
  }

  public Levels generateInferVars(Equations equations, Concrete.SourceNode sourceNode) {
    return generateInferVars(equations, myUniverseKind != UniverseKind.NO_UNIVERSES, sourceNode);
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

  public abstract Expression getTypeWithParams(List<? super DependentLink> params, Levels levels);

  public abstract Expression getDefCall(Levels levels, List<Expression> args);

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
      return this == NEEDS_TYPE_CHECKING || this == TYPE_CHECKING;
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

  @Override
  public String toString() {
    return myReferable.toString();
  }
}
