package org.arend.typechecking.typecheckable.provider;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Reference;

import javax.annotation.Nullable;

public interface PartialConcreteProvider {
  @Nullable Reference getInstanceTypeReference(GlobalReferable instance);
  boolean isInstance(GlobalReferable ref);
  boolean isUse(GlobalReferable ref);
  boolean isData(GlobalReferable ref);
  boolean isFunction(GlobalReferable ref);
}
