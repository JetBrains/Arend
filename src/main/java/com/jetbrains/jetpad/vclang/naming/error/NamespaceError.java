package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

public class NamespaceError<T> extends GeneralError<T> {
  public final Group.NamespaceCommand command;

  public NamespaceError(String message, Group.NamespaceCommand command) {
    super(Level.ERROR, message);
    this.command = command;
  }

  public NamespaceError(Level level, String message, Group.NamespaceCommand command) {
    super(level, message);
    this.command = command;
  }

  @Override
  public T getCause() {
    return (T) command;
  }

  @Override
  public PrettyPrintable getCausePP() {
    return command;
  }
}
