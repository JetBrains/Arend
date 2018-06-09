package com.jetbrains.jetpad.vclang.naming.reference.converter;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.util.FullName;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SimpleReferableConverter implements ReferableConverter {
  private final Map<FullName, TCReferable> myMap = new HashMap<>();

  @Override
  public Referable toDataReferable(Referable referable) {
    return referable;
  }

  @Override
  public TCReferable toDataLocatedReferable(LocatedReferable referable) {
    return referable == null ? null : myMap.get(new FullName(referable));
  }

  public TCReferable get(LocatedReferable referable) {
    return myMap.get(new FullName(referable));
  }

  public TCReferable computeIfAbsent(LocatedReferable referable, Function<? super FullName, ? extends TCReferable> tcReferable) {
    return myMap.computeIfAbsent(new FullName(referable), tcReferable);
  }

  public TCReferable remove(LocatedReferable referable) {
    return myMap.remove(new FullName(referable));
  }

  public void clear() {
    myMap.clear();
  }
}
