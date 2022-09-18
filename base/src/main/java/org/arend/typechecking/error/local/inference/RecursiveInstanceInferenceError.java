package org.arend.typechecking.error.local.inference;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.error.InstanceInferenceError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;
import org.arend.typechecking.instance.pool.RecursiveInstanceData;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;

import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class RecursiveInstanceInferenceError extends InstanceInferenceError {
  public final RecursiveInstanceHoleExpression holeExpression;

  private RecursiveInstanceInferenceError(ArendRef classRef, CoreExpression expected, CoreExpression actual, CoreExpression classifyingExpression, ConcreteSourceNode cause, RecursiveInstanceHoleExpression holeExpr, CoreExpression[] candidates) {
    super(classRef, expected, actual, classifyingExpression, holeExpr != null && holeExpr.getData() instanceof ConcreteSourceNode ? (ConcreteSourceNode) holeExpr.getData() : cause, candidates);
    this.holeExpression = holeExpr;
  }

  public RecursiveInstanceInferenceError(ArendRef classRef, ConcreteSourceNode cause, RecursiveInstanceHoleExpression holeExpr, CoreExpression[] candidates) {
    this(classRef, null, null, null, cause, holeExpr, candidates);
  }

  public RecursiveInstanceInferenceError(ArendRef classRef, CoreExpression classifyingExpression, ConcreteSourceNode cause, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, null, null, classifyingExpression, cause, holeExpr, new CoreExpression[0]);
  }

  public RecursiveInstanceInferenceError(ArendRef classRef, CoreExpression expected, CoreExpression actual, ConcreteSourceNode cause, CoreExpression candidate, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, expected, actual, null, cause, holeExpr, new CoreExpression[1]);
    candidates[0] = candidate;
  }

  @Override
  public void addAdditionalDocs(List<Doc> docs, PrettyPrinterConfig ppConfig) {
    if (holeExpression != null) {
      for (int i = holeExpression.recursiveData.size() - 1; i >= 0; i--) {
        RecursiveInstanceData recursiveData = holeExpression.recursiveData.get(i);
        boolean withExpr = recursiveData.classifyingExpression != null;
        LineDoc line = hList(text("for instance '"), refDoc(recursiveData.instanceRef), text("' of class '"), refDoc(recursiveData.classRef), text(withExpr ? "' with classifying expression:" : "'"));
        docs.add(withExpr ? hang(line, termDoc(recursiveData.classifyingExpression, ppConfig)) : line);
      }
    }
  }

  @Override
  public boolean hasExpressions() {
    return super.hasExpressions() || holeExpression != null;
  }
}
