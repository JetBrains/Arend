package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionSourceInfoVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class OneshotSourceInfoCollector {
  public final SimpleSourceInfoProvider sourceInfoProvider = new SimpleSourceInfoProvider();

  public void visitModule(ModuleID moduleID, Abstract.ClassDefinition module) {
    DefinitionSourceInfoVisitor vis = new DefinitionSourceInfoVisitor(sourceInfoProvider, moduleID);
    FullName moduleFullName = new FullName(module.getName());
    sourceInfoProvider.registerDefinition(module, moduleFullName, moduleID);
    vis.visitClass(module, moduleFullName);
  }
}
