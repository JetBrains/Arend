package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class GoalError<T> extends LocalTypeCheckingError<T> {
  public final String name;
  public final Map<Abstract.ReferableSourceNode, Binding> context;
  public final ExpectedType expectedType;
  public final Expression actualType;
  public final List<Error> errors;

  public GoalError(String name, Map<Abstract.ReferableSourceNode, Binding> context, ExpectedType expectedType, Expression actualType, List<Error> errors, Concrete.Expression<T> expression) {
    super(Level.GOAL, "", expression);
    this.name = name;
    this.context = new HashMap<>(context);
    this.expectedType = expectedType;
    this.actualType = actualType;
    this.errors = errors;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    Doc expectedDoc = expectedType == null ? nullDoc() : hang(text("Expected type:"), typeDoc(expectedType));
    Doc actualDoc = actualType == null ? nullDoc() : hang(text(expectedType != null ? "  Actual type:" : "Type:"), termDoc(actualType));

    Doc contextDoc;
    if (!context.isEmpty()) {
      List<Doc> contextDocs = new ArrayList<>(context.size());
      for (Map.Entry<Abstract.ReferableSourceNode, Binding> entry : context.entrySet()) {
        Expression type = entry.getValue().getTypeExpr();
        contextDocs.add(hang(hList(refDoc(entry.getKey()), text(" :")), type == null ? text("{?}") : termDoc(type)));
      }
      contextDoc = hang(text("Context:"), vList(contextDocs));
    } else {
      contextDoc = nullDoc();
    }

    Doc errorsDoc;
    if (!errors.isEmpty()) {
      List<Doc> errorsDocs = new ArrayList<>(errors.size());
      for (Error error : errors) {
        errorsDocs.add(hang(error.getHeaderDoc(src), error.getBodyDoc(src)));
      }
      errorsDoc = hang(text("Errors:"), vList(errorsDocs));
    } else {
      errorsDoc = nullDoc();
    }

    return vList(expectedDoc, actualDoc, contextDoc, errorsDoc);
  }
}
