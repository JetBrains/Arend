package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.frontend.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class DefinitionSourceInfoVisitor<SourceIdT extends SourceId> implements AbstractDefinitionVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider<SourceIdT> myProvider;
  private final SourceIdT mySourceId;

  public DefinitionSourceInfoVisitor(SimpleSourceInfoProvider<SourceIdT> provider, SourceIdT sourceId) {
    myProvider = provider;
    mySourceId = sourceId;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, FullName fullName) {
    reg(def, fullName);

    for (Abstract.Definition definition : def.getGlobalDefinitions()) {
      definition.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, FullName fullName) {
    reg(def, fullName);
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, FullName fullName) {
    reg(def, fullName);

    for (Concrete.ConstructorClause<?> clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitConstructor(constructor, fullName);
      }
    }
    return null;

  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, FullName fullName) {
    reg(def, fullName);
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, FullName fullName) {
    reg(def, fullName);

    for (Abstract.ClassField field : def.getFields()) {
      visitClassField(field, fullName);
    }
    for (Abstract.Implementation implementation : def.getImplementations()) {
      visitImplement(implementation, fullName);
    }
    for (Abstract.Definition definition : def.getGlobalDefinitions()) {
      definition.accept(this, null);
    }
    for (Abstract.Definition definition : def.getInstanceDefinitions()) {
      definition.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, FullName fullName) {
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, FullName fullName) {
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, FullName fullName) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, FullName fullName) {
    return null;
  }

  private void reg(Abstract.Definition def, FullName fullName) {
    myProvider.registerDefinition(def, new FullName(fullName, def.getName()), mySourceId);
  }

}
