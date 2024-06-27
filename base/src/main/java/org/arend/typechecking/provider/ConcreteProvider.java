package org.arend.typechecking.provider;

import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.Nullable;

public interface ConcreteProvider {
  @Nullable Concrete.GeneralDefinition getConcrete(GlobalReferable referable);

  default @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable) {
    Concrete.GeneralDefinition def = getConcrete(referable);
    return def instanceof Concrete.FunctionDefinition ? (Concrete.FunctionDefinition) def : null;
  }

  default @Nullable Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable) {
    Concrete.FunctionDefinition def = getConcreteFunction(referable);
    return def != null && def.getKind() == FunctionKind.INSTANCE ? def : null;
  }

  default @Nullable Concrete.ClassDefinition getConcreteClass(GlobalReferable referable) {
    Concrete.GeneralDefinition def = getConcrete(referable);
    return def instanceof Concrete.ClassDefinition ? (Concrete.ClassDefinition) def : null;
  }

  default @Nullable Concrete.DataDefinition getConcreteData(GlobalReferable referable) {
    Concrete.GeneralDefinition def = getConcrete(referable);
    return def instanceof Concrete.DataDefinition ? (Concrete.DataDefinition) def : null;
  }
}
