package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionSourceInfoVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class OneshotSourceInfoCollector {
  public final SimpleSourceInfoProvider sourceInfoProvider = new SimpleSourceInfoProvider();

  public void visitModule(ModuleID moduleID, Abstract.ClassDefinition module) {
    DefinitionSourceInfoVisitor vis = new DefinitionSourceInfoVisitor(sourceInfoProvider, moduleID);
    vis.visitClass(module, new FullName(module.getName()));
  }
}
