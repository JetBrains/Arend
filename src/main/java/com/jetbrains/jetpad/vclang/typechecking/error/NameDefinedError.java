package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;

public class NameDefinedError extends TypeCheckingError {
  private final String myName;
  private final ResolvedName myResolvedName;

  public NameDefinedError(boolean alreadyDefined, ResolvedName resolvedName, Abstract.SourceNode expression, String name, ResolvedName where) {
    super(resolvedName, alreadyDefined ? "is already defined" : "is not defined", expression);
    myName = name;
    myResolvedName = where;
  }

  public NameDefinedError(boolean alreadyDefined, Abstract.SourceNode expression, String name, ResolvedName where) {
    super(alreadyDefined ? "is already defined" : "is not defined", expression);
    myName = name;
    myResolvedName = where;
  }

  public NameDefinedError(boolean alreadyDefined, ResolvedName resolvedName, Abstract.SourceNode expression, Name name, ResolvedName where) {
    this(alreadyDefined, resolvedName, expression, name.getPrefixName(), where);
  }

  public NameDefinedError(boolean alreadyDefined, Abstract.SourceNode expression, Name name, ResolvedName where) {
    this(alreadyDefined, expression, name.getPrefixName(), where);
  }

  @Override
  public String toString() {
    String msg = printHeader() + "Name '" + myName + "' " + getMessage();
    if (myResolvedName != null) {
      msg += " in " + myResolvedName;
    }
    return msg;
  }
}
