package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.FullModulePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface TCClassReferable extends TCReferable, ClassReferable {
  @Override @NotNull List<? extends TCClassReferable> getSuperClassReferences();
  @Override @NotNull Collection<? extends TCFieldReferable> getFieldReferables();
  @Override @NotNull Collection<? extends Referable> getImplementedFields();

  TCClassReferable NULL_REFERABLE = new TCClassReferable() {
    @Nullable
    @Override
    public Object getData() {
      return null;
    }

    @NotNull
    @Override
    public List<? extends TCClassReferable> getSuperClassReferences() {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<? extends TCFieldReferable> getFieldReferables() {
      return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<? extends Referable> getImplementedFields() {
      return Collections.emptyList();
    }

    @Override
    public boolean isRecord() {
      return true;
    }

    @NotNull
    @Override
    public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
      return Collections.emptyList();
    }

    @Override
    public TCReferable getTypecheckable() {
      return this;
    }

    @Nullable
    @Override
    public FullModulePath getLocation() {
      return null;
    }

    @Nullable
    @Override
    public LocatedReferable getLocatedReferableParent() {
      return null;
    }

    @NotNull
    @Override
    public Precedence getPrecedence() {
      return Precedence.DEFAULT;
    }

    @NotNull
    @Override
    public String textRepresentation() {
      return "_";
    }
  };
}
