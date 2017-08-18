package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class FieldsImplementationError<T> extends LocalTypeCheckingError<T> {
  public boolean alreadyImplemented;
  public Collection<? extends Abstract.ClassField> fields;

  public FieldsImplementationError(boolean alreadyImplemented, Collection<? extends Abstract.ClassField> fields, Concrete.SourceNode<T> cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.fields = fields;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), hSep(text(", "), fields.stream().map(f -> f == null ? text("parent") : refDoc(f) /* TODO[classes] */).collect(Collectors.toList())));
  }
}
