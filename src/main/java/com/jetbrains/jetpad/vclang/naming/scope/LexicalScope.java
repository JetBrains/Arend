package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import java.util.function.Predicate;

public class LexicalScope extends PartialLexicalScope {
  public LexicalScope(Scope parent, Group group, boolean ignoreOpen) {
    super(parent, group, ignoreOpen);
  }

  public LexicalScope(Scope parent, Group group, NamespaceCommand cmd) {
    super(parent, group, cmd);
  }

  @Override
  public PartialLexicalScope restrict(NamespaceCommand cmd) {
    return new LexicalScope(parent, group, cmd);
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    Referable referable = super.find(pred);
    if (referable != null) {
      return referable;
    }
    return parent.find(pred);
  }
}
