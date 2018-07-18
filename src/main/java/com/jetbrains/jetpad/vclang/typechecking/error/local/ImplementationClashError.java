package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ImplementationClashError extends TypecheckingError {
  public final Expression oldImplementation;
  public final Expression newImplementation;

  public ImplementationClashError(Expression oldImplementation, ClassField classField, Expression newImplementation, Concrete.SourceNode cause) {
    super("New implementation of field '" + classField.getName() + "' differs from the previous one", cause);
    this.oldImplementation = oldImplementation;
    this.newImplementation = newImplementation;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Old:"), oldImplementation.prettyPrint(ppConfig)),
      hang(text("New:"), newImplementation.prettyPrint(ppConfig)));
  }
}
