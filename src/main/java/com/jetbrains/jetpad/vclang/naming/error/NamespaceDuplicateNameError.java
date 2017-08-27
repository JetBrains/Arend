package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

public class NamespaceDuplicateNameError<T> extends NamespaceError<T> {
  public final Referable referable;
  public final Referable previous;

  public NamespaceDuplicateNameError(Level level, Referable referable, Referable previous, Group.NamespaceCommand cmd) {
    super(level, "Duplicate name: " + referable.textRepresentation(), cmd);
    this.referable = referable;
    this.previous = previous;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return ReferableDuplicateNameError.getBodyDoc(referable, previous);
  }
}
