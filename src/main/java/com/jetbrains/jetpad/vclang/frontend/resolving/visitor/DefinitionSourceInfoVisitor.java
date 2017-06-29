package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.frontend.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;

public class DefinitionSourceInfoVisitor<SourceIdT extends SourceId> implements AbstractDefinitionVisitor<FullName, Void>, AbstractStatementVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider<SourceIdT> myProvider;
  private final SourceIdT mySourceId;

  public DefinitionSourceInfoVisitor(SimpleSourceInfoProvider<SourceIdT> provider, SourceIdT sourceId) {
    myProvider = provider;
    mySourceId = sourceId;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, FullName fullName) {
    for (Abstract.Statement statement : def.getGlobalStatements()) {
      statement.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, FullName fullName) {
    myProvider.registerDefinition(def, new FullName(fullName, def.getName()), mySourceId);
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, FullName fullName) {
    for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
      for (Abstract.Constructor constructor : clause.getConstructors()) {
        visitConstructor(constructor, fullName);
      }
    }
    return null;

  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, FullName fullName) {
    myProvider.registerDefinition(def, new FullName(fullName, def.getName()), mySourceId);
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, FullName fullName) {
    for (Abstract.ClassField field : def.getFields()) {
      visitClassField(field, fullName);
    }
    for (Abstract.Implementation implementation : def.getImplementations()) {
      visitImplement(implementation, fullName);
    }
    for (Abstract.Statement statement : def.getGlobalStatements()) {
      statement.accept(this, null);
    }
    for (Abstract.Definition definition : def.getInstanceDefinitions()) {
      definition.accept(this, fullName);
    }
    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, FullName fullName) {
    myProvider.registerDefinition(def, new FullName(fullName, def.getName()), mySourceId);
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

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, FullName fullName) {
    Abstract.Definition def = stat.getDefinition();
    FullName defName = new FullName(fullName, def.getName());
    myProvider.registerDefinition(def, defName, mySourceId);
    def.accept(this, defName);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, FullName fullName) {
    return null;
  }
}
