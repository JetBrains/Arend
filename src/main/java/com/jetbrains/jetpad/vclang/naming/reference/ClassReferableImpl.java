package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class ClassReferableImpl extends LocatedReferableImpl implements ClassReferable {
  private final List<ClassReferable> mySuperClassReferences;
  private final List<? extends GlobalReferable> myFieldReferables;

  public ClassReferableImpl(Precedence precedence, String name, List<ClassReferable> superClassReferences, List<? extends GlobalReferable> fieldReferables, LocatedReferable parent) {
    super(precedence, name, parent, true);
    mySuperClassReferences = superClassReferences;
    myFieldReferables = fieldReferables;
  }

  public ClassReferableImpl(Precedence precedence, String name, List<ClassReferable> superClassReferences, List<? extends GlobalReferable> fieldReferables, ModulePath parent) {
    super(precedence, name, parent);
    mySuperClassReferences = superClassReferences;
    myFieldReferables = fieldReferables;
  }

  @Nonnull
  @Override
  public List<ClassReferable> getSuperClassReferences() {
    return mySuperClassReferences;
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getFieldReferables() {
    return myFieldReferables;
  }
}
