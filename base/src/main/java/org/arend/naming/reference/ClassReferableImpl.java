package org.arend.naming.reference;

import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements TCClassReferable {
  private final boolean myRecord;
  private final List<TCClassReferable> mySuperClassReferences;
  private final List<? extends TCFieldReferable> myFieldReferables;

  public ClassReferableImpl(Precedence precedence, String name, boolean isRecord, List<TCClassReferable> superClassReferences, List<? extends TCFieldReferable> fieldReferables, ModulePath parent) {
    super(precedence, name, parent, Kind.TYPECHECKABLE);
    myRecord = isRecord;
    mySuperClassReferences = superClassReferences;
    myFieldReferables = fieldReferables;
  }

  @NotNull
  @Override
  public List<TCClassReferable> getSuperClassReferences() {
    return mySuperClassReferences;
  }

  @NotNull
  @Override
  public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
    return Collections.emptyList();
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
  public boolean isRecord() {
    return myRecord;
  }
}
