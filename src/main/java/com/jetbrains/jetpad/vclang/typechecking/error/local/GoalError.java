package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class GoalError extends TypecheckingError {
  public final String name;
  public final Map<Referable, Binding> context;
  public final ExpectedType expectedType;
  public final Expression actualType;
  public final List<Error> errors;

  public GoalError(String name, Map<Referable, Binding> context, ExpectedType expectedType, Expression actualType, List<Error> errors, Concrete.Expression expression) {
    super(Level.GOAL, "", expression);
    this.name = name;
    this.context = new HashMap<>(context);
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
        Expression type = entry.getValue().getTypeExpr();
        contextDocs.add(hang(hList(entry.getKey() == null ? text("_") : refDoc(entry.getKey()), text(" :")), type == null ? text("{?}") : termDoc(type, ppConfig)));
      }
      contextDoc = hang(text("Context:"), vList(contextDocs));
    } else {
      contextDoc = nullDoc();
    }

    Doc errorsDoc;
    if (!errors.isEmpty()) {
      List<Doc> errorsDocs = new ArrayList<>(errors.size());
      for (Error error : errors) {
        errorsDocs.add(hang(error.getHeaderDoc(ppConfig), error.getBodyDoc(ppConfig)));
      }
      errorsDoc = hang(text("Errors:"), vList(errorsDocs));
    } else {
      errorsDoc = nullDoc();
    }

    return vList(expectedDoc, actualDoc, contextDoc, errorsDoc);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof GoalError && cause.equals(((GoalError) obj).cause);
  }

  @Override
  public int hashCode() {
    return cause.hashCode();
  }
}
