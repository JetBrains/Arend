package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

public class StatementSourceInfoVisitor implements AbstractStatementVisitor<FullName, Void> {
  private final SimpleSourceInfoProvider myProvider;
  private final ModuleSourceId mySourceId;

  public StatementSourceInfoVisitor(SimpleSourceInfoProvider provider, ModuleSourceId sourceId) {
    myProvider = provider;
    mySourceId = sourceId;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, FullName params) {
    DefinitionSourceInfoVisitor myDefVisitor = new DefinitionSourceInfoVisitor(myProvider, mySourceId);
    Abstract.Definition def = stat.getDefinition();
    FullName defName = new FullName(params, def.getName());
    myProvider.registerDefinition(def, defName, mySourceId);
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
