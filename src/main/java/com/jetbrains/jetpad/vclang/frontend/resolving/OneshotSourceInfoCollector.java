package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionSourceInfoVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class OneshotSourceInfoCollector<SourceIdT extends SourceId> {
  public final SimpleSourceInfoProvider<SourceIdT> sourceInfoProvider = new SimpleSourceInfoProvider<>();

  public void visitModule(SourceIdT moduleID, Abstract.ClassDefinition module) {
    DefinitionSourceInfoVisitor vis = new DefinitionSourceInfoVisitor<>(sourceInfoProvider, moduleID);
    FullName moduleFullName = new FullName(module.getName());
    sourceInfoProvider.registerDefinition(module, moduleFullName, moduleID);
    vis.visitClass(module, moduleFullName);
  }
}
