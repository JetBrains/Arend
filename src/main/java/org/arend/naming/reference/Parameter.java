package org.arend.naming.reference;

import javax.annotation.Nonnull;
import java.util.List;

public interface Parameter {
  boolean isExplicit();
  @Nonnull List<? extends Referable> getReferableList();
}

