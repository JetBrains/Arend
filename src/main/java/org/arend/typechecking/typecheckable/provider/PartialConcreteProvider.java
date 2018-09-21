package org.arend.typechecking.typecheckable.provider;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Reference;

import javax.annotation.Nullable;

public interface PartialConcreteProvider {
  @Nullable Reference getInstanceTypeReference(GlobalReferable instance);
  boolean isRecord(ClassReferable classRef);
  boolean isInstance(GlobalReferable ref);
  boolean isCoerce(GlobalReferable ref);
  boolean isData(GlobalReferable ref);
}
