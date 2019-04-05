package org.arend.naming.reference.converter;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.util.FullName;
import org.arend.util.LongName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SimpleReferableConverter implements ReferableConverter {
  private final Map<FullName, TCReferable> myMap = new HashMap<>();
  private final Map<FullName, List<FullName>> myInternalReferables = new HashMap<>();

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

  private static FullName getTypecheckableFullName(LocatedReferable referable, FullName fullName) {
    GlobalReferable.Kind kind = referable.getKind();
    return (kind == GlobalReferable.Kind.CONSTRUCTOR || kind == GlobalReferable.Kind.FIELD) && fullName.longName.size() > 1
      ? new FullName(fullName.modulePath, new LongName(fullName.longName.toList().subList(0, fullName.longName.size() - 1)))
      : fullName;
  }

  private void addInternalReferable(LocatedReferable referable, FullName fullName) {
    FullName tcFullName = getTypecheckableFullName(referable, fullName);
    if (tcFullName != fullName) {
      myInternalReferables.computeIfAbsent(tcFullName, k -> new ArrayList<>()).add(fullName);
    }
  }

  public TCReferable putIfAbsent(LocatedReferable referable, TCReferable tcReferable) {
    FullName fullName = new FullName(referable);
    TCReferable prev = myMap.putIfAbsent(fullName, tcReferable);
    if (prev == null) {
      addInternalReferable(referable, fullName);
    }
    return prev;
  }

  public TCReferable computeIfAbsent(LocatedReferable referable, Function<? super FullName, ? extends TCReferable> tcReferable) {
    FullName fullName = new FullName(referable);
    return myMap.computeIfAbsent(fullName, fn -> {
      addInternalReferable(referable, fullName);
      return tcReferable.apply(fn);
    });
  }

  public TCReferable remove(LocatedReferable referable) {
    return remove(referable, new FullName(referable));
  }

  public TCReferable remove(LocatedReferable referable, FullName fullName) {
    TCReferable result = myMap.remove(fullName);
    if (result == null) {
      return null;
    }

    // Remove constructors and fields
    List<FullName> internalRefs = myInternalReferables.remove(getTypecheckableFullName(referable, fullName));
    if (internalRefs != null) {
      for (FullName name : internalRefs) {
        myMap.remove(name);
      }
    }

    return result;
  }

  public void clear() {
    myMap.clear();
    myInternalReferables.clear();
  }
}
