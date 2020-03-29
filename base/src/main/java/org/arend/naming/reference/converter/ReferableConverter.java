package org.arend.naming.reference.converter;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

public interface ReferableConverter {
  Referable toDataReferable(Referable referable);
  TCReferable toDataLocatedReferable(LocatedReferable referable);

  default Referable convert(Referable referable) {
    return referable instanceof MetaReferable ? referable
      : referable instanceof LocatedReferable
        ? toDataLocatedReferable((LocatedReferable) referable)
        : toDataReferable(referable);
  }
}
