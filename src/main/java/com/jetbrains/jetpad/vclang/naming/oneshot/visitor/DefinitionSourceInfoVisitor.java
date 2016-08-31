package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

public class DefinitionSourceInfoVisitor implements AbstractDefinitionVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider myProvider;
  private final ModuleID myModuleId;
  private final StatementSourceInfoVisitor myStVisitor;

  public DefinitionSourceInfoVisitor(SimpleSourceInfoProvider provider, ModuleID moduleId) {
    myProvider = provider;
    myModuleId = moduleId;
    myStVisitor = new StatementSourceInfoVisitor(myProvider, myModuleId);
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, FullName params) {
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(myStVisitor, params);
    }
    return null;
  }

  @Override
  public Void visitAbstract(Abstract.ClassViewField def, FullName params) {
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, FullName params) {
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, FullName params) {
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
  public Void visitImplement(Abstract.ImplementDefinition def, FullName params) {
    return null;
  }
}
