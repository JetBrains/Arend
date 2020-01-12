package org.arend.typechecking;

import org.arend.ext.DefinitionProvider;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;

import javax.annotation.Nonnull;
import java.util.Collections;

public class ExtDefinitionProvider implements DefinitionProvider {
  private boolean myEnabled = true;
  private final TypecheckingOrderingListener myTypechecking;

  public ExtDefinitionProvider(TypecheckingOrderingListener typechecking) {
    myTypechecking = typechecking;
  }

  public void disable() {
    myEnabled = false;
  }

  @Nonnull
  @Override
  public CoreDefinition getDefinition(@Nonnull RawRef ref) {
    if (!myEnabled) {
      throw new IllegalStateException("ExtDefinitionProvider was disabled");
    }
    Concrete.ReferableDefinition def = ref instanceof GlobalReferable ? myTypechecking.getConcreteProvider().getConcrete((GlobalReferable) ref) : null;
    if (!(def instanceof Concrete.Definition)) {
      throw new IllegalArgumentException("Expected a global definition");
    }
    myTypechecking.typecheckDefinitions(Collections.singletonList((Concrete.Definition) def), null);
    return myTypechecking.getTypecheckerState().getTypechecked(def.getData());
  }
}
