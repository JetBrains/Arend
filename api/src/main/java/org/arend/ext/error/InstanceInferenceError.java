package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class InstanceInferenceError extends ArgInferenceError {
  public final ArendRef classRef;
  public final CoreExpression classifyingExpression;

  protected InstanceInferenceError(ArendRef classRef, CoreExpression expected, CoreExpression actual, CoreExpression classifyingExpression, ConcreteSourceNode cause, CoreExpression[] candidates) {
    super("", expected, actual, cause, candidates);
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
  }

  public InstanceInferenceError(ArendRef classRef, ConcreteSourceNode cause, CoreExpression[] candidates) {
    this(classRef, null, null, null, cause, candidates);
  }

  public InstanceInferenceError(ArendRef classRef, CoreExpression classifyingExpression, ConcreteSourceNode cause) {
    this(classRef, null, null, classifyingExpression, cause, new CoreExpression[0]);
  }

  public InstanceInferenceError(ArendRef classRef, CoreExpression expected, CoreExpression actual, ConcreteSourceNode cause, CoreExpression candidate) {
    this(classRef, expected, actual, null, cause, new CoreExpression[1]);
    candidates[0] = candidate;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Cannot infer an instance of class '"), refDoc(classRef), text("'"));
  }

  protected void addAdditionalDocs(List<Doc> docs, PrettyPrinterConfig ppConfig) {}

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    List<Doc> list = new ArrayList<>();
    if (classifyingExpression != null) {
      list.add(hang(text("with classifying expression:"), termDoc(classifyingExpression, ppConfig)));
    }
    addAdditionalDocs(list, ppConfig);
    list.add(super.getBodyDoc(ppConfig));
    return vList(list);
  }

  @Override
  public boolean hasExpressions() {
    return super.hasExpressions() || classifyingExpression != null;
  }
}
