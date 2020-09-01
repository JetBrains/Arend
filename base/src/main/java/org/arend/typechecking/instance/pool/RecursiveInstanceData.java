package org.arend.typechecking.instance.pool;

import org.arend.core.expr.Expression;
import org.arend.naming.reference.TCDefReferable;

public class RecursiveInstanceData {
  public final TCDefReferable instanceRef;
  public final TCDefReferable classRef;
  public final Expression classifyingExpression;

  public RecursiveInstanceData(TCDefReferable instanceRef, TCDefReferable classRef, Expression classifyingExpression) {
    this.instanceRef = instanceRef;
    this.classRef = classRef;
    this.classifyingExpression = classifyingExpression;
  }
}
