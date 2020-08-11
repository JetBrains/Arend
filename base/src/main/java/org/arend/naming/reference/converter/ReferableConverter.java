package org.arend.naming.reference.converter;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

public interface ReferableConverter {
  TCReferable toDataLocatedReferable(LocatedReferable referable);

  default Referable convert(Referable referable) {
    if (referable instanceof LocatedReferable) {
      var ref = toDataLocatedReferable((LocatedReferable) referable);
      if (ref != null) return ref;
    }
    return referable;
  }
}
