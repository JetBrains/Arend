package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

import java.util.HashMap;
import java.util.Map;

public class MultiNameResolver implements NameResolver {
  private final Map<String, NamespaceMember> myNames = new HashMap<>();

  public void add(NamespaceMember member) {
    myNames.put(member.namespace.getName().name, member);
  }

  public void remove(String name) {
    myNames.remove(name);
  }

  @Override
  public NamespaceMember locateName(String name) {
    return myNames.get(name);
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    return null;
  }
}
