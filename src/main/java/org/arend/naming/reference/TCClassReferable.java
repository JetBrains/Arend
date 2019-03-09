package org.arend.naming.reference;

import org.arend.module.ModulePath;
import org.arend.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface TCClassReferable extends TCReferable, ClassReferable {
  @Override @Nonnull List<? extends TCClassReferable> getSuperClassReferences();
  @Override @Nonnull Collection<? extends TCFieldReferable> getFieldReferables();
  @Override @Nonnull Collection<? extends Referable> getImplementedFields();

  TCClassReferable NULL_REFERABLE = new TCClassReferable() {
    @Nullable
    @Override
    public Object getData() {
      return null;
    }

    @Nonnull
    @Override
    public List<? extends TCClassReferable> getSuperClassReferences() {
      return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<? extends TCFieldReferable> getFieldReferables() {
      return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<? extends Referable> getImplementedFields() {
      return Collections.emptyList();
    }

    @Nonnull
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
    public ModulePath getLocation() {
      return null;
    }

    @Nullable
    @Override
    public LocatedReferable getLocatedReferableParent() {
      return null;
    }

    @Nonnull
    @Override
    public Precedence getPrecedence() {
      return Precedence.DEFAULT;
    }

    @Nonnull
    @Override
    public String textRepresentation() {
      return "_";
    }
  };
}
