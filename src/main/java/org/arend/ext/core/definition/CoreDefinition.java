package org.arend.ext.core.definition;

import org.arend.ext.concrete.ArendRef;

import javax.annotation.Nonnull;

public interface CoreDefinition {
  @Nonnull ArendRef getRef();
}
