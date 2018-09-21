package org.arend.naming.reference;

import org.arend.module.ModulePath;
import org.arend.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements TCClassReferable {
  private final List<TCClassReferable> mySuperClassReferences;
  private final List<? extends TCFieldReferable> myFieldReferables;

  public ClassReferableImpl(Precedence precedence, String name, List<TCClassReferable> superClassReferences, List<? extends TCFieldReferable> fieldReferables, ModulePath parent) {
    super(precedence, name, parent);
    mySuperClassReferences = superClassReferences;
    myFieldReferables = fieldReferables;
  }

  @Nonnull
  @Override
  public List<TCClassReferable> getSuperClassReferences() {
    return mySuperClassReferences;
  }

  @Nonnull
  @Override
  public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends TCFieldReferable> getFieldReferables() {
    return myFieldReferables;
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public TCClassReferable getUnderlyingReference() {
    return null;
  }
}
