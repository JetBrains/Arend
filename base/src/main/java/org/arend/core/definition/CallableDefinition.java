package org.arend.core.definition;

import org.arend.core.expr.Expression;
import org.arend.core.subst.Levels;
import org.arend.naming.reference.TCDefReferable;

import java.util.List;

public abstract class CallableDefinition extends Definition {
  public CallableDefinition(TCDefReferable referable, TypeCheckingStatus status) {
    super(referable, status);
  }

  public abstract Expression getDefCall(Levels levels, List<Expression> args);
}
