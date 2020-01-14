package org.arend.extImpl;

import org.arend.ext.DefinitionProvider;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;

import javax.annotation.Nonnull;
import java.util.Collections;

public class DefinitionProviderImpl extends Disableable implements DefinitionProvider {
  private final TypecheckingOrderingListener myTypechecking;

  public DefinitionProviderImpl(TypecheckingOrderingListener typechecking) {
    myTypechecking = typechecking;
  }

  @Nonnull
  @Override
  public CoreDefinition getDefinition(@Nonnull RawRef ref) {
    checkEnabled();
    Concrete.ReferableDefinition def = ref instanceof GlobalReferable ? myTypechecking.getConcreteProvider().getConcrete((GlobalReferable) ref) : null;
    if (!(def instanceof Concrete.Definition)) {
      throw new IllegalArgumentException("Expected a global definition");
    }
    myTypechecking.typecheckDefinitions(Collections.singletonList((Concrete.Definition) def), null);
    return myTypechecking.getTypecheckerState().getTypechecked(def.getData());
  }
}
