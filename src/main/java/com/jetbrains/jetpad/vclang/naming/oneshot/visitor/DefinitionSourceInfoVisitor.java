package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

public class DefinitionSourceInfoVisitor implements AbstractDefinitionVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider myProvider;
  private final ModuleSourceId mySourceId;
  private final StatementSourceInfoVisitor myStVisitor;

  public DefinitionSourceInfoVisitor(SimpleSourceInfoProvider provider, ModuleSourceId sourceId) {
    myProvider = provider;
    mySourceId = sourceId;
    myStVisitor = new StatementSourceInfoVisitor(myProvider, mySourceId);
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, FullName params) {
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(myStVisitor, params);
    }
    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, FullName params) {
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, FullName params) {
    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitConstructor(constructor, params);
    }
    return null;

  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, FullName params) {
    myProvider.registerDefinition(def, new FullName(params, def.getName()), mySourceId);
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, FullName params) {
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(myStVisitor, params);
    }
    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, FullName params) {
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, FullName params) {
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, FullName params) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, FullName params) {
    return null;
  }
}
