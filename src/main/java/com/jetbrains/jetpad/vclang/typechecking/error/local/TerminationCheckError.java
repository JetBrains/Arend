package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.termination.CompositeCallMatrix;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;

import java.util.Set;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class TerminationCheckError<T> extends GeneralError<T> {
  public final Abstract.Definition definition;
  public final Set<RecursiveBehavior<Definition>> behaviors;

  // TODO[abstract]: override getCause and getCausePP
  public TerminationCheckError(Definition def, Set<RecursiveBehavior<Definition>> behaviors) {
    super(Level.ERROR, "");
    definition = def.getAbstractDefinition();
    this.behaviors = behaviors;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text("Termination check failed for function '"), refDoc(definition), text("'"));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return vList(behaviors.stream().map(TerminationCheckError::printBehavior).collect(Collectors.toList()));
  }

  private static Doc printBehavior(RecursiveBehavior rb) {
    return hang(text(rb.initialCallMatrix instanceof CompositeCallMatrix ? "Problematic sequence of recursive calls:" : "Problematic recursive call:"), rb.initialCallMatrix.getMatrixLabel());
  }
}
