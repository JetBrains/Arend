package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;

public class GroupNameResolver<T> extends GroupResolver<T> {
  private final TypecheckableProvider<T> myTypecheckableProvider;
  private final DefinitionResolveNameVisitor<T> myVisitor;

  public GroupNameResolver(NameResolver nameResolver, ParserInfoProvider infoProvider, ErrorReporter<T> errorReporter, TypecheckableProvider<T> provider) {
    super(nameResolver, errorReporter);
    myTypecheckableProvider = provider;
    myVisitor = new DefinitionResolveNameVisitor<>(nameResolver, infoProvider, errorReporter);
  }

  @Override
  protected void processReferable(GlobalReferable referable, Scope scope) {
    Concrete.ReferableDefinition<T> def = myTypecheckableProvider.getTypecheckable(referable);
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition<T>) def).accept(myVisitor, scope);
    }
  }
}
