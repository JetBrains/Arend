package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.instance.pool.RecursiveInstanceData;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;

import java.util.ArrayList;
import java.util.List;

import static org.arend.error.doc.DocFactory.*;

public class InstanceInferenceError extends ArgInferenceError {
  public final TCClassReferable classRef;
  public final Expression classifyingExpression;
  public final RecursiveInstanceHoleExpression holeExpression;

  private InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Expression classifyingExpression, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr, Expression[] candidates) {
    super("Cannot infer an instance of class '" + classRef.textRepresentation() + "'", expected, actual, holeExpr != null && holeExpr.getData() instanceof Concrete.SourceNode ? (Concrete.SourceNode) holeExpr.getData() : cause, candidates);
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
    this.holeExpression = holeExpr;
  }

  public InstanceInferenceError(TCClassReferable classRef, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr, Expression[] candidates) {
    this(classRef, null, null, null, cause, holeExpr, candidates);
  }

  public InstanceInferenceError(TCClassReferable classRef, Expression classifyingExpression, Concrete.SourceNode cause, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, null, null, classifyingExpression, cause, holeExpr, new Expression[0]);
  }

  public InstanceInferenceError(TCClassReferable classRef, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate, RecursiveInstanceHoleExpression holeExpr) {
    this(classRef, expected, actual, null, cause, holeExpr, new Expression[1]);
    candidates[0] = candidate;
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
}
