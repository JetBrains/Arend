package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class TypeCheckingError extends GeneralError {
  private Abstract.Definition myDefinition;

  public TypeCheckingError(Level level, Abstract.Definition definition, String message, Abstract.SourceNode cause) {
    super(level, message, cause);
    myDefinition = definition;
  }

  public TypeCheckingError(Abstract.Definition definition, String message, Abstract.SourceNode cause) {
    super(message, cause);
    myDefinition = definition;
  }

  @Deprecated
  public TypeCheckingError(String message, Abstract.SourceNode cause) {
    super(message, cause);
    myDefinition = null;
  }

  @Deprecated
  public TypeCheckingError(String message) {
    this(null, message, null);
  }

  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  @Deprecated
  public void setDefinition(Abstract.Definition definition) {
    myDefinition = definition;
  }
}
