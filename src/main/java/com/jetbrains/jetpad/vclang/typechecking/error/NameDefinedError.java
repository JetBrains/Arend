package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class NameDefinedError extends TypeCheckingError {
  private final String myName;
  private final Namespace myNamespace;

  public NameDefinedError(boolean alreadyDefined, Namespace namespace, Abstract.SourceNode expression, String name, Namespace where) {
    super(namespace, alreadyDefined ? "is already defined" : "is not defined", expression, null);
    myName = name;
    myNamespace = where;
  }

  public NameDefinedError(boolean alreadyDefined, Abstract.SourceNode expression, String name, Namespace where) {
    super(alreadyDefined ? "is already defined" : "is not defined", expression, null);
    myName = name;
    myNamespace = where;
  }

  public NameDefinedError(boolean alreadyDefined, Namespace namespace, Abstract.SourceNode expression, Utils.Name name, Namespace where) {
    this(alreadyDefined, namespace, expression, name.getPrefixName(), where);
  }

  public NameDefinedError(boolean alreadyDefined, Abstract.SourceNode expression, Utils.Name name, Namespace where) {
    this(alreadyDefined, expression, name.getPrefixName(), where);
  }

  @Override
  public String toString() {
    String msg = printHeader() + "Name '" + myName + "' " + getMessage();
    if (myNamespace != null) {
      msg += " in " + myNamespace;
    }
    return msg;
  }
}
