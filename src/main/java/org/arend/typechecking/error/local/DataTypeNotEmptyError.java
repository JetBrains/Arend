package org.arend.typechecking.error.local;

import org.arend.core.definition.Constructor;
import org.arend.core.expr.DataCallExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class DataTypeNotEmptyError extends TypecheckingError {
  public final DataCallExpression dataCall;
  public final Collection<? extends Constructor> constructors;

  public DataTypeNotEmptyError(DataCallExpression dataCall, Collection<? extends Constructor> constructors, Concrete.SourceNode cause) {
    super("", cause);
    this.dataCall = dataCall;
    this.constructors = constructors;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("Data type '"), refDoc(dataCall.getDefinition().getReferable()), text("' is not empty"));
  }

  @Override
  public LineDoc getBodyDoc(PrettyPrinterConfig src) {
    return hList(text("Available constructors: "), hSep(text(", "), constructors.stream().map(con -> refDoc(con.getReferable())).collect(Collectors.toList())));
  }
}
