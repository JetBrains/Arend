package com.jetbrains.jetpad.vclang.naming;

public class DefinitionResolvedName extends ResolvedName {
  private final String myName;
  private final ResolvedName myParent;


  public DefinitionResolvedName(ResolvedName parent, String name) {
    this.myName = name;
    this.myParent = parent;
  }

  @Override
  public NamespaceMember toNamespaceMember() {
    // FIXME[serial]
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFullName() {
    return myParent.getFullName() + "." + myName;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public ResolvedName getParent() {
    return myParent;
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof DefinitionResolvedName
        && myName.equals(((ResolvedName) other).getName())
        && myParent == ((DefinitionResolvedName) other).getParent();
  }
}
