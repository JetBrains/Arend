package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;

public class GroupNameResolver extends GroupResolver {
  private final TypecheckableProvider myTypecheckableProvider;
  private final DefinitionResolveNameVisitor myVisitor;

  public GroupNameResolver(NameResolver nameResolver, ErrorReporter errorReporter, TypecheckableProvider provider) {
    super(nameResolver, errorReporter);
    myTypecheckableProvider = provider;
    myVisitor = new DefinitionResolveNameVisitor(nameResolver, errorReporter);
  }

  @Override
  protected void processReferable(GlobalReferable referable, Scope scope) {
    Concrete.ReferableDefinition def = myTypecheckableProvider.getTypecheckable(referable);
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition) def).accept(myVisitor, scope);
    }
  }
}
