package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.instance.pool.RecursiveInstanceData;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class InstanceInferenceError extends ArgInferenceError {
  public final TCDefReferable classRef;
  public final Expression classifyingExpression;
  public final RecursiveInstanceHoleExpression holeExpression;

  private InstanceInferenceError(TCDefReferable classRef, Expression expected, Expression actual, Expression classifyingExpression, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr, Expression[] candidates) {
    super("", expected, actual, holeExpr != null && holeExpr.getData() instanceof Concrete.SourceNode ? (Concrete.SourceNode) holeExpr.getData() : cause, candidates);
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
    this.holeExpression = holeExpr;
  }

  public InstanceInferenceError(TCDefReferable classRef, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr, Expression[] candidates) {
    this(classRef, null, null, null, cause, holeExpr, candidates);
  }

  public InstanceInferenceError(TCDefReferable classRef, Expression classifyingExpression, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, null, null, classifyingExpression, cause, holeExpr, new Expression[0]);
  }

  public InstanceInferenceError(TCDefReferable classRef, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, expected, actual, null, cause, holeExpr, new Expression[1]);
    candidates[0] = candidate;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Cannot infer an instance of class '"), refDoc(classRef), text("'"));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    List<Doc> list = new ArrayList<>();
    if (classifyingExpression != null) {
      list.add(hang(text("with classifying expression:"), termDoc(classifyingExpression, ppConfig)));
    }
    if (holeExpression != null) {
      for (int i = holeExpression.recursiveData.size() - 1; i >= 0; i--) {
        RecursiveInstanceData recursiveData = holeExpression.recursiveData.get(i);
        boolean withExpr = recursiveData.classifyingExpression != null;
        LineDoc line = hList(text("for instance '"), refDoc(recursiveData.instanceRef), text("' of class '"), refDoc(recursiveData.classRef), text(withExpr ? "' with classifying expression:" : "'"));
        list.add(withExpr ? hang(line, termDoc(recursiveData.classifyingExpression, ppConfig)) : line);
      }
    }
    list.add(super.getBodyDoc(ppConfig));
    return vList(list);
  }

  @Override
  public boolean hasExpressions() {
    return super.hasExpressions() || classifyingExpression != null || holeExpression != null;
  }
}
