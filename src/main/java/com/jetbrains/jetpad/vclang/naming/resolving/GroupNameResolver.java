package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

public class GroupNameResolver extends GroupResolver {
  private final ConcreteProvider myConcreteProvider;
  private final DefinitionResolveNameVisitor myVisitor;

  public GroupNameResolver(NameResolver nameResolver, ErrorReporter errorReporter, ConcreteProvider provider) {
    super(nameResolver, errorReporter);
    myConcreteProvider = provider;
    myVisitor = new DefinitionResolveNameVisitor(errorReporter);
  }

  @Override
  protected void processReferable(GlobalReferable referable, Scope scope) {
    Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(referable);
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition) def).accept(myVisitor, scope);
    }
  }
}
