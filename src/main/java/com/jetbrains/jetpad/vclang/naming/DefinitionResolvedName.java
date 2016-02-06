package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.term.definition.Name;

public class DefinitionResolvedName extends ResolvedName {
  private final String myName;
  private final Namespace myParent;


  public DefinitionResolvedName(Namespace parent, String name) {
    this.myName = name;
    this.myParent = parent;
  }


  @Override
  public NamespaceMember toNamespaceMember() {
    return myParent.getMember(myName);
  }

  @Override
  public ModuleID getModuleID() {
    return myParent.getResolvedName().getModuleID();
  }

  @Override
  public String getFullName() {
    return myParent.getResolvedName().getFullName() + "." + myName;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public ResolvedName getParent() {
    return myParent.getResolvedName();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof DefinitionResolvedName
        && myName.equals(((ResolvedName) other).getName())
        && myParent == ((DefinitionResolvedName) other).getParentNamespace();
  }

  public Namespace getParentNamespace() {
    return myParent;
  }
}
