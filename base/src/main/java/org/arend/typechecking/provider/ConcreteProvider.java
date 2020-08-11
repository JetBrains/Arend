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

  @Nullable default TCReferable getTCReferable(GlobalReferable referable) {
    if (referable instanceof TCReferable) {
      return (TCReferable) referable;
    }
    var def = getConcrete(referable);
    return def instanceof Concrete.ReferableDefinition ? ((Concrete.ReferableDefinition) def).getData() : null;
  }
}
