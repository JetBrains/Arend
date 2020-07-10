package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.naming.reference.TCReferable;

public class RecursiveInstanceData {
  public final TCReferable instanceRef;
  public final TCReferable classRef;
  public final Expression classifyingExpression;

  public RecursiveInstanceData(TCReferable instanceRef, TCReferable classRef, Expression classifyingExpression) {
    this.instanceRef = instanceRef;
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
  }
}
