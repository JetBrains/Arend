package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;

import javax.annotation.Nonnull;

public interface Reference extends DataContainer {
  @Nonnull Referable getReferent();
}
