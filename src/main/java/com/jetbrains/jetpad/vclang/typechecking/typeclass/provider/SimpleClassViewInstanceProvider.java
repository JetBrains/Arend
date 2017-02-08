package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleClassViewInstanceProvider implements ClassViewInstanceProvider {
  private final Map<Pair<Abstract.DefCallExpression, Integer>, Collection<? extends Abstract.ClassViewInstance>> myInstances = new HashMap<>();

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.DefCallExpression defCall, int paramIndex) {
    Collection<? extends Abstract.ClassViewInstance> instances = myInstances.get(new Pair<>(defCall, paramIndex));
    return instances == null ? Collections.<Abstract.ClassViewInstance>emptyList() : instances;
  }

  public void addInstances(Abstract.DefCallExpression defCall, int paramIndex, Collection<? extends Abstract.ClassViewInstance> instances) {
    myInstances.put(new Pair<>(defCall, paramIndex), instances);
  }
}
