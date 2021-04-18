package org.arend.typechecking.error.local;

import org.arend.core.definition.Definition;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.DataCallExpression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class DataTypeNotEmptyError extends TypecheckingError {
  public final DataCallExpression dataCall;
  public final Collection<? extends Definition> constructors;

  public DataTypeNotEmptyError(DataCallExpression dataCall, Collection<? extends Definition> constructors, Concrete.SourceNode cause) {
    super("", cause);
    this.dataCall = dataCall;
    this.constructors = constructors;
  }

  public static List<Definition> getConstructors(Collection<? extends ConCallExpression> conCalls) {
    List<Definition> result = new ArrayList<>();
    for (ConCallExpression conCall : conCalls) {
      result.add(conCall.getDefinition());
    }
    return result;
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
