package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;

public interface ClassViewInstanceProvider {
  Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.DefCallExpression defCall, int paramIndex);
}
