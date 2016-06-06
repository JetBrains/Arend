package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

public class StatementSourceInfoVisitor implements AbstractStatementVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider myProvider;
  private final ModuleID myModuleId;
  private final DefinitionSourceInfoVisitor myDefVisitor;

  public StatementSourceInfoVisitor(SimpleSourceInfoProvider provider, ModuleID moduleId) {
    myProvider = provider;
    myModuleId = moduleId;
    myDefVisitor = new DefinitionSourceInfoVisitor(provider, myModuleId);
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, FullName params) {
    Abstract.Definition def = stat.getDefinition();
    FullName defName = new FullName(params, def.getName());
    myProvider.registerDefinition(def, defName, myModuleId);
    def.accept(myDefVisitor, defName);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, FullName params) {
    return null;
  }

  @Override
  public Void visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, FullName params) {
    return null;
  }
}
