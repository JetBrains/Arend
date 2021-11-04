package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements ClassReferable {
  private final boolean myRecord;
  private Abstract.LevelParameters myPLevelParameters;
  private Abstract.LevelParameters myHLevelParameters;
  private final List<ClassReferable> mySuperClassReferences;
  private final List<Boolean> mySuperLevels;
  private final List<? extends TCFieldReferable> myFieldReferables;
  private final List<GlobalReferable> myDynamicReferables;

  public ClassReferableImpl(Precedence precedence, String name, boolean isRecord, Abstract.LevelParameters pLevelParameters, Abstract.LevelParameters hLevelParameters, List<ClassReferable> superClassReferences, List<Boolean> superLevels, List<? extends TCFieldReferable> fieldReferables, List<GlobalReferable> dynamicReferables, LocatedReferable parent) {
    super(precedence, name, parent, Kind.CLASS);
    myRecord = isRecord;
    myPLevelParameters = pLevelParameters;
    myHLevelParameters = hLevelParameters;
    mySuperClassReferences = superClassReferences;
    mySuperLevels = superLevels;
    myFieldReferables = fieldReferables;
    myDynamicReferables = dynamicReferables;
  }

  @NotNull
  @Override
  public List<ClassReferable> getSuperClassReferences() {
    return mySuperClassReferences;
  }

  @Override
  public boolean hasLevels(int index) {
    return index < mySuperLevels.size() && mySuperLevels.get(index);
  }

  public void addSuperLevels(boolean has) {
    mySuperLevels.add(has);
  }

  @NotNull
  @Override
  public Collection<? extends TCFieldReferable> getFieldReferables() {
    return myFieldReferables;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends GlobalReferable> getDynamicReferables() {
    return myDynamicReferables;
  }

  @Override
  public boolean isRecord() {
    return myRecord;
  }

  @Override
  public @Nullable Abstract.LevelParameters getPLevelParameters() {
    return myPLevelParameters;
  }

  @Override
  public @Nullable Abstract.LevelParameters getHLevelParameters() {
    return myHLevelParameters;
  }

  public void setPLevelParameters(Abstract.LevelParameters params) {
    myPLevelParameters = params;
  }

  public void setHLevelParameters(Abstract.LevelParameters params) {
    myHLevelParameters = params;
  }
}
