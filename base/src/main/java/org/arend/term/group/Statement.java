package org.arend.term.group;

import org.arend.term.NamespaceCommand;

public interface Statement {
  Group getGroup();
  NamespaceCommand getNamespaceCommand();
}
