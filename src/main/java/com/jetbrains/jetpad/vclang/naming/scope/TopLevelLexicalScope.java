package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Group;

public class TopLevelLexicalScope extends LexicalScope {
  private final ImportedScope myImportedScope;

  public TopLevelLexicalScope(ImportedScope importedScope, Scope parent, Group group) {
    super(parent, group, false);
    myImportedScope = importedScope;
  }

  @Override
  ImportedScope getImportedScope() {
    return myImportedScope;
  }
}
