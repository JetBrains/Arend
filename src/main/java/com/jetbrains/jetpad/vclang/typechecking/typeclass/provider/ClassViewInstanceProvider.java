package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;

public interface ClassViewInstanceProvider {
  Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.ReferenceExpression defCall, int paramIndex);
}
