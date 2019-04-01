package org.arend.naming.reference.converter;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.util.FullName;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
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

  public TCReferable putIfAbsent(LocatedReferable referable, TCReferable tcReferable) {
    return myMap.putIfAbsent(new FullName(referable), tcReferable);
  }

  public TCReferable computeIfAbsent(LocatedReferable referable, Function<? super FullName, ? extends TCReferable> tcReferable) {
    return myMap.computeIfAbsent(new FullName(referable), tcReferable);
  }

  public TCReferable compute(LocatedReferable referable, BiFunction<? super FullName, ? super TCReferable, ? extends TCReferable> tcReferable) {
    return myMap.compute(new FullName(referable), tcReferable);
  }

  public TCReferable remove(LocatedReferable referable) {
    return myMap.remove(new FullName(referable));
  }

  public TCReferable remove(FullName fullName) {
    return myMap.remove(fullName);
  }

  public void clear() {
    myMap.clear();
  }
}
