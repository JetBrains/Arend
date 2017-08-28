package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

public class NamespaceError<T> extends GeneralError<T> {
  public final NamespaceCommand command;

  public NamespaceError(String message, NamespaceCommand command) {
    super(Level.ERROR, message);
    this.command = command;
  }

  public NamespaceError(Level level, String message, NamespaceCommand command) {
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
