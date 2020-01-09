package org.arend.ext.core.elimtree;

import org.arend.ext.core.context.CoreParameter;

import javax.annotation.Nonnull;

public interface CoreElimTree extends CoreBody {
  @Nonnull CoreParameter getParameters();
}
