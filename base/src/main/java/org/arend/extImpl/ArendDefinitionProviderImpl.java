package org.arend.extImpl;

import org.arend.core.definition.Definition;
import org.arend.ext.ArendDefinitionProvider;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.library.Library;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class ArendDefinitionProviderImpl extends Disableable implements ArendDefinitionProvider {
  private final TypecheckingOrderingListener myTypechecking;
  private final ModuleScopeProvider myModuleScopeProvider;
  private final DefinitionRequester myDefinitionRequester;
  private final Library myLibrary;

  public ArendDefinitionProviderImpl(TypecheckingOrderingListener typechecking, ModuleScopeProvider moduleScopeProvider, DefinitionRequester definitionRequester, Library library) {
    myTypechecking = typechecking;
    myModuleScopeProvider = moduleScopeProvider;
    myDefinitionRequester = definitionRequester;
    myLibrary = library;
  }

  @NotNull
  @Override
  public <T extends CoreDefinition> T getDefinition(@NotNull ModulePath module, @NotNull LongName name, Class<T> clazz) {
    checkEnabled();
    Scope scope = myModuleScopeProvider.forModule(module);
    Referable ref = scope == null ? null : Scope.Utils.resolveName(scope, name.toList());
    Concrete.ReferableDefinition def = ref instanceof GlobalReferable ? myTypechecking.getConcreteProvider().getConcrete((GlobalReferable) ref) : null;
    if (def == null) {
      throw new IllegalArgumentException("Cannot find definition '" + name + "'");
    }
    myTypechecking.typecheckDefinitions(Collections.singletonList(def.getRelatedDefinition()), null);
    Definition result = myTypechecking.getTypecheckerState().getTypechecked(def.getData());
    if (!clazz.isInstance(result)) {
      throw new IllegalArgumentException(result == null ? "Cannot find definition '" + ref.getRefName() + "'" : "Cannot cast '" + result.getClass() + "' to '" + clazz + "'");
    }
    myDefinitionRequester.request(result, myLibrary);
    return clazz.cast(result);
  }
}
