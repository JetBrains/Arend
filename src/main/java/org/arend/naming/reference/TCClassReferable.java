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
  @Override @Nullable TCClassReferable getUnderlyingReference();

  default @Override @Nullable TCClassReferable getUnderlyingTypecheckable() {
    TCClassReferable underlyingRef = getUnderlyingReference();
    return underlyingRef == null ? this : underlyingRef.isSynonym() ? null : underlyingRef;
  }

  @Override
  default boolean isSynonym() {
    return getUnderlyingReference() != null;
  }

  TCClassReferable NULL_REFERABLE = new TCClassReferable() {
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

    @Nullable
    @Override
    public TCClassReferable getUnderlyingReference() {
      return null;
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

    @Nullable
    @Override
    public Reference getUnresolvedUnderlyingReference() {
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
