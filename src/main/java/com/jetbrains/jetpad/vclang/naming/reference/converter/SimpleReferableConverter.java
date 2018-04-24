package com.jetbrains.jetpad.vclang.naming.reference.converter;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public class SimpleReferableConverter implements ReferableConverter {
  private final Map<LocatedReferable, TCReferable> myMap = new WeakHashMap<>();

  @Override
  public Referable toDataReferable(Referable referable) {
    return referable;
  }

  @Override
  public TCReferable toDataLocatedReferable(LocatedReferable referable) {
    return referable == null ? null : myMap.get(referable);
  }

  public TCReferable computeIfAbsent(LocatedReferable referable, Function<? super LocatedReferable, ? extends TCReferable> tcReferable) {
    return myMap.computeIfAbsent(referable, tcReferable);
  }

  public TCReferable remove(LocatedReferable referable) {
    return myMap.remove(referable);
  }

  public void clear() {
    myMap.clear();
  }
}
