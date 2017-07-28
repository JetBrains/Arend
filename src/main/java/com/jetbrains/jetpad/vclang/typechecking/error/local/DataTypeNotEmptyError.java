package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class DataTypeNotEmptyError extends LocalTypeCheckingError {
  public final DataCallExpression dataCall;
  public final Collection<? extends Constructor> constructors;

  public DataTypeNotEmptyError(DataCallExpression dataCall, Collection<? extends Constructor> constructors, Abstract.SourceNode cause) {
    super("", cause);
    this.dataCall = dataCall;
    this.constructors = constructors;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" Data type '"), refDoc(dataCall.getDefinition().getAbstractDefinition()), text("' is not empty"));
  }

  @Override
  public LineDoc getBodyDoc() {
    return hList(text("Available constructors: "), hSep(text(", "), constructors.stream().map(con -> refDoc(con.getAbstractDefinition())).collect(Collectors.toList())));
  }
}
