package org.arend.typechecking.provider;

import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.Nullable;

public interface ConcreteProvider {
  @Nullable Concrete.GeneralDefinition getConcrete(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable);
  @Nullable Concrete.ClassDefinition getConcreteClass(GlobalReferable referable);
  @Nullable Concrete.DataDefinition getConcreteData(GlobalReferable referable);

  default @Nullable Concrete.ClassDefinition getConcreteClassHeader(GlobalReferable referable) {
    return getConcreteClass(referable);
  }
}
