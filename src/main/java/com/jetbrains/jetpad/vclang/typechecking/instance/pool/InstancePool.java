package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;

public interface InstancePool {
  Expression getInstance(Expression classifyingExpression, TCClassReferable classRef);
}
