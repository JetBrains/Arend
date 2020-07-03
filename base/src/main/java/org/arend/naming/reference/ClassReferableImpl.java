package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements ClassReferable {
  private final boolean myRecord;
  private final List<ClassReferable> mySuperClassReferences;
  private final List<? extends TCFieldReferable> myFieldReferables;

  public ClassReferableImpl(Precedence precedence, String name, boolean isRecord, List<ClassReferable> superClassReferences, List<? extends TCFieldReferable> fieldReferables, ModuleLocation parent) {
    super(precedence, name, parent, Kind.TYPECHECKABLE);
    myRecord = isRecord;
    mySuperClassReferences = superClassReferences;
    myFieldReferables = fieldReferables;
  }

  @NotNull
  @Override
  public List<ClassReferable> getSuperClassReferences() {
    return mySuperClassReferences;
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
