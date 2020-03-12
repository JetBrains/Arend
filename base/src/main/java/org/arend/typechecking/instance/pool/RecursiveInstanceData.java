package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;

public class RecursiveInstanceData {
  public final TCReferable instanceRef;
  public final TCClassReferable classRef;
  public final Expression classifyingExpression;

  public RecursiveInstanceData(TCReferable instanceRef, TCClassReferable classRef, Expression classifyingExpression) {
    this.instanceRef = instanceRef;
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
  }
}
