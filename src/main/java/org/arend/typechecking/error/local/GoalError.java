package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class GoalError extends TypecheckingError {
  public final String name;
  public final Map<Referable, Binding> context;
  public final ExpectedType expectedType;
  public final Expression actualType;
  public final List<GeneralError> errors;

  public GoalError(String name, Map<Referable, Binding> context, ExpectedType expectedType, Expression actualType, List<GeneralError> errors, Concrete.Expression expression) {
    super(Level.GOAL, "Goal" + (name == null ? "" : " " + name), expression);
    this.name = name;
    this.context = new LinkedHashMap<>(context);
    this.expectedType = expectedType;
    this.actualType = actualType;
    this.errors = errors;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    Doc expectedDoc = expectedType == null ? nullDoc() : hang(text("Expected type:"), expectedType.prettyPrint(ppConfig));
    Doc actualDoc = actualType == null ? nullDoc() : hang(text(expectedType != null ? "  Actual type:" : "Type:"), termDoc(actualType, ppConfig));

    Doc contextDoc;
    if (!context.isEmpty()) {
      List<Doc> contextDocs = new ArrayList<>(context.size());
      for (Map.Entry<Referable, Binding> entry : context.entrySet()) {
        if (!entry.getValue().isHidden()) {
          Expression type = entry.getValue().getTypeExpr();
          contextDocs.add(hang(hList(entry.getKey() == null ? text("_") : refDoc(entry.getKey()), text(" :")), type == null ? text("{?}") : termDoc(type, ppConfig)));
        }
      }
      contextDoc = contextDocs.isEmpty() ? nullDoc() : hang(text("Context:"), vList(contextDocs));
    } else {
      contextDoc = nullDoc();
    }

    Doc errorsDoc;
    if (!errors.isEmpty()) {
      List<Doc> errorsDocs = new ArrayList<>(errors.size());
      for (GeneralError error : errors) {
        errorsDocs.add(hang(error.getHeaderDoc(ppConfig), error.getBodyDoc(ppConfig)));
      }
      errorsDoc = hang(text("Errors:"), vList(errorsDocs));
    } else {
      errorsDoc = nullDoc();
    }

    return vList(expectedDoc, actualDoc, contextDoc, errorsDoc);
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
