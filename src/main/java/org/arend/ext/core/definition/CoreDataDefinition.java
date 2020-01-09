package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.level.CoreSort;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface CoreDataDefinition extends CoreDefinition {
  boolean isTruncated();
  @Nonnull CoreParameter getParameters();
  CoreSort getSort();
  @Nonnull Collection<? extends CoreConstructor> getConstructors();
}
