package org.arend.extImpl;

import org.arend.core.definition.Definition;
import org.arend.ext.DefinitionProvider;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.reference.RawRef;
import org.arend.library.Library;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class DefinitionProviderImpl extends Disableable implements DefinitionProvider {
  private final TypecheckingOrderingListener myTypechecking;
  private final DefinitionRequester myDefinitionRequester;
  private final Library myLibrary;

  public DefinitionProviderImpl(TypecheckingOrderingListener typechecking, DefinitionRequester definitionRequester, Library library) {
    myTypechecking = typechecking;
    myDefinitionRequester = definitionRequester;
    myLibrary = library;
  }

  @NotNull
  @Override
  public <T extends CoreDefinition> T getDefinition(@NotNull RawRef ref, Class<T> clazz) {
    checkEnabled();

    Concrete.ReferableDefinition def = ref instanceof GlobalReferable ? myTypechecking.getConcreteProvider().getConcrete((GlobalReferable) ref) : null;
    if (!(def instanceof Concrete.Definition)) {
      throw new IllegalArgumentException("Expected a global definition");
    }
    myTypechecking.typecheckDefinitions(Collections.singletonList((Concrete.Definition) def), null);
    Definition result = myTypechecking.getTypecheckerState().getTypechecked(def.getData());
    if (!clazz.isInstance(result)) {
      throw new IllegalArgumentException(result == null ? "Cannot find definition '" + ref.getRefName() + "'" : "Cannot cast '" + result.getClass() + "' to '" + clazz + "'");
    }
    myDefinitionRequester.request(result, myLibrary);
    return clazz.cast(result);
  }
}
