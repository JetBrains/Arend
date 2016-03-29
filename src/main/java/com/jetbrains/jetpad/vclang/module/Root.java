package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.module.ModuleResolver;

import java.util.HashMap;
import java.util.Map;

public class Root {
  private static Map<ModuleID, NamespaceMember> myModules = new HashMap<>();
  private static Map<ModulePath, ModuleID> myResolveAux = new HashMap<>();

  public static void initialize() {
    myModules.clear();
    myResolveAux.clear();
    addModule(Prelude.moduleID, new NamespaceMember(Prelude.PRELUDE, null, Prelude.PRELUDE_CLASS));
    addModule(Preprelude.moduleID, new NamespaceMember(Preprelude.PRE_PRELUDE, null, Preprelude.PRE_PRELUDE_CLASS));
  }

  public static ModuleResolver rootModuleResolver = new ModuleResolver() {
    @Override
    public NamespaceMember locateModule(ModulePath modulePath) {
      ModuleID moduleID = myResolveAux.get(modulePath);
      return moduleID == null ? null : myModules.get(moduleID);
    }
  };

  public static NamespaceMember addModule(ModuleID moduleID, NamespaceMember member) {
    ModuleID oldID = myResolveAux.get(moduleID.getModulePath());
    if (oldID != null) {
      return myModules.get(oldID);
    }
    myModules.put(moduleID, member);
    myResolveAux.put(moduleID.getModulePath(), moduleID);
    return null;
  }

  public static NamespaceMember removeModule(ModuleID moduleID) {
    NamespaceMember result = myModules.remove(moduleID);
    if (result == null) {
      return null;
    }
    myResolveAux.remove(moduleID.getModulePath());
    return null;
  }

  public static NamespaceMember getModule(ModuleID module) {
    return myModules.get(module);
  }
}
