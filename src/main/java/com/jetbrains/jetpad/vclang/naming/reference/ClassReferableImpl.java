package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements TCClassReferable {
  private final List<TCClassReferable> mySuperClassReferences;
  private final List<? extends TCReferable> myFieldReferables;

  public ClassReferableImpl(Precedence precedence, String name, List<TCClassReferable> superClassReferences, List<? extends TCReferable> fieldReferables, ModulePath parent) {
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
  public Collection<? extends TCReferable> getFieldReferables() {
    return myFieldReferables;
  }

  @Nullable
  @Override
  public TCClassReferable getUnderlyingReference() {
    return null;
  }
}
