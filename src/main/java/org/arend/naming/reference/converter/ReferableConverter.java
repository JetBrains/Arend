package org.arend.naming.reference.converter;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

public interface ReferableConverter {
  Referable toDataReferable(Referable referable);
  TCReferable toDataLocatedReferable(LocatedReferable referable);
}
