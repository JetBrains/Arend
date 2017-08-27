package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

// TODO[abstract]: Delete this?
public class DefinitionSourceInfoVisitor<SourceIdT extends SourceId, T> { /* implements ConcreteDefinitionVisitor<T, FullName, Void> {
  private final SimpleSourceInfoProvider<SourceIdT> myProvider;
  private final SourceIdT mySourceId;

  public DefinitionSourceInfoVisitor(SimpleSourceInfoProvider<SourceIdT> provider, SourceIdT sourceId) {
    myProvider = provider;
    mySourceId = sourceId;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, FullName fullName) {
    reg(def, fullName);

    for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
      definition.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, FullName fullName) {
    reg(def, fullName);

    for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
      for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
        reg(constructor, fullName);
      }
    }
    return null;

  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, FullName fullName) {
    reg(def, fullName);

    for (Concrete.ClassField<T> field : def.getFields()) {
      reg(field, fullName);
    }
    for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
      definition.accept(this, null);
    }
    for (Concrete.Definition<T> definition : def.getInstanceDefinitions()) {
      definition.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView<T> def, FullName fullName) {
    return null;
  }

  @Override
  public Void visitClassViewField(Concrete.ClassViewField<T> def, FullName fullName) {
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance<T> def, FullName fullName) {
    return null;
  }

  private void reg(GlobalReferable def, FullName fullName) {
    myProvider.registerDefinition(def, new FullName(fullName, def.textRepresentation()), mySourceId);
  }
  */
}
