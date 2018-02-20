package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class SimpleClassReferable extends SimpleGlobalReferable implements ClassReferable {
  private final List<ClassReferable> mySuperClassReferences;
  private final List<? extends GlobalReferable> myFieldReferables;

  public SimpleClassReferable(Precedence precedence, String name, List<ClassReferable> superClassReferences, List<? extends GlobalReferable> fieldReferables) {
    super(precedence, name, null);
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
