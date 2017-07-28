package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class GoalError extends LocalTypeCheckingError {
  public final Map<Abstract.ReferableSourceNode, Binding> context;
  public final ExpectedType type;

  public GoalError(Map<Abstract.ReferableSourceNode, Binding> context, ExpectedType type, Abstract.Expression expression) {
    super(Level.GOAL, "", expression);
    this.context = new HashMap<>(context);
    this.type = type;
  }

  @Override
  public Doc getBodyDoc() {
    Doc doc = type == null ? nullDoc() : hang(text("Expected type:"), typeDoc(type));
    if (context.isEmpty()) {
      return doc;
    }

    List<Doc> contextDocs = new ArrayList<>(context.size());
    for (Map.Entry<Abstract.ReferableSourceNode, Binding> entry : context.entrySet()) {
      Expression type = entry.getValue().getTypeExpr();
      contextDocs.add(hang(hList(refDoc(entry.getKey()), text(" :")), type == null ? text("{?}") : termDoc(type)));
    }
    return vList(doc, hang(text("Context:"), vList(contextDocs)));
  }
}
