package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class FieldsImplementationError extends LocalTypeCheckingError {
  public boolean alreadyImplemented;
  public Collection<? extends Abstract.ClassField> fields;

  public FieldsImplementationError(boolean alreadyImplemented, Collection<? extends Abstract.ClassField> fields, Abstract.SourceNode cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.fields = fields;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), hSep(text(", "), fields.stream().map(DocFactory::refDoc).collect(Collectors.toList())));
  }
}
