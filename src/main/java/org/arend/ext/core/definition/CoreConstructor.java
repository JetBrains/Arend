package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.elimtree.CoreBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreConstructor extends CoreDefinition {
  @Nonnull CoreDataDefinition getDataType();
  @Nonnull CoreParameter getParameters();
  @Nullable CoreBody getBody();
}
