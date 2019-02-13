package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.error.Error;
import org.arend.error.doc.Doc;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.*;

import static org.arend.core.expr.visitor.ToAbstractVisitor.Flag.*;
import static org.arend.error.doc.DocFactory.*;

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
    EnumSet<ToAbstractVisitor.Flag> flags = ppConfig.getExpressionFlags().clone();
    flags.remove(SHOW_CON_PARAMS);
    flags.remove(SHOW_IMPLICIT_ARGS);
    flags.remove(SHOW_TYPES_IN_LAM);
    flags.remove(SHOW_INFERENCE_LEVEL_VARS);
    ppConfig = new PrettyPrinterConfig() {
      @Override
      public EnumSet<ToAbstractVisitor.Flag> getExpressionFlags() {
        return flags;
      }
    };

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
