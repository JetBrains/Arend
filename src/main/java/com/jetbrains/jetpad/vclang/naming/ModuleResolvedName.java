package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.Root;

public class ModuleResolvedName extends ResolvedName {
  private final ModuleID myModuleID;

  public ModuleResolvedName(ModuleID moduleID) {
    this.myModuleID = moduleID;
  }

  @Override
  public String getName() {
    return myModuleID.getModulePath().getName();
  }

  @Override
  public ResolvedName getParent() {
    return null;
  }

  @Override
  public NamespaceMember toNamespaceMember() {
    return Root.getModule(myModuleID);
  }

  public ModuleID getModuleID() {
    return myModuleID;
  }

  @Override
  public String getFullName() {
    return myModuleID.getModulePath().toString();
  }
}
