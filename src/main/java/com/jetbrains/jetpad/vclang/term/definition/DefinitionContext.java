package com.jetbrains.jetpad.vclang.term.definition;

import java.util.HashMap;
import java.util.Map;

public class DefinitionContext {
  final private Map<String, Definition> myLocalMembers = new HashMap<>();
  final private Namespace myNamespace;

  public DefinitionContext(Namespace namespace) {
    myNamespace = namespace;
  }

  public Namespace getNamespace() {
    return myNamespace;
  }

  /*
  public Definition getLocalMember(String name) {
    return myLocalMembers.get(name);
  }

  public Collection<Definition> getLocalMembers() {
    return myLocalMembers.values();
  }

  public void addLocalMember(Definition definition) {
    myLocalMembers.put(definition.getName().name, definition);
  }

  public void removeLocalMember(Definition definition) {
    myLocalMembers.values().remove(definition);
  }
  */
}
