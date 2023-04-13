package org.arend.core.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class TopLevelDefinition extends CallableDefinition {
  private UniverseKind myUniverseKind = UniverseKind.NO_UNIVERSES;
  private List<? extends LevelVariable> myLevelParameters;
  private TCReferable myPLevelsParent;
  private TCReferable myHLevelsParent;
  private boolean myPLevelsDerived;
  private boolean myHLevelsDerived;
  private List<Pair<TCDefReferable,Integer>> myParametersOriginalDefinitions = Collections.emptyList();
  private Set<? extends FunctionDefinition> myAxioms = Collections.emptySet();
  private Set<? extends Definition> myGoals = Collections.emptySet();

  public TopLevelDefinition(TCDefReferable referable, TypeCheckingStatus status) {
    super(referable, status);
  }

  @Override
  public TopLevelDefinition getTopLevelDefinition() {
    return this;
  }

  @Override
  public UniverseKind getUniverseKind() {
    return myUniverseKind;
  }

  public void setUniverseKind(UniverseKind kind) {
    myUniverseKind = kind;
  }

  @Override
  public List<? extends LevelVariable> getLevelParameters() {
    return myLevelParameters;
  }

  public void setLevelParameters(List<LevelVariable> parameters) {
    myLevelParameters = parameters;
  }

  @Override
  public TCReferable getPLevelsParent() {
    return myPLevelsParent;
  }

  @Override
  public TCReferable getHLevelsParent() {
    return myHLevelsParent;
  }

  public void setPLevelsParent(TCReferable parent) {
    myPLevelsParent = parent;
  }

  public void setHLevelsParent(TCReferable parent) {
    myHLevelsParent = parent;
  }

  @Override
  public boolean arePLevelsDerived() {
    return myPLevelsDerived;
  }

  @Override
  public boolean areHLevelsDerived() {
    return myHLevelsDerived;
  }

  public void setPLevelsDerived(boolean derived) {
    myPLevelsDerived = derived;
  }

  public void setHLevelsDerived(boolean derived) {
    myHLevelsDerived = derived;
  }

  @Override
  public List<? extends Pair<TCDefReferable,Integer>> getParametersOriginalDefinitions() {
    return myParametersOriginalDefinitions;
  }

  public void setParametersOriginalDefinitions(List<Pair<TCDefReferable,Integer>> definitions) {
    myParametersOriginalDefinitions = definitions;
  }

  @Override
  public Set<? extends FunctionDefinition> getAxioms() {
    return myAxioms;
  }

  public boolean isAxiom() {
    return false;
  }

  public void setAxioms(Set<? extends FunctionDefinition> axioms) {
    myAxioms = axioms;
  }

  @Override
  public Set<? extends Definition> getGoals() {
    return myGoals;
  }

  public void setGoals(Set<? extends Definition> goals) {
    myGoals = goals;
  }
}
