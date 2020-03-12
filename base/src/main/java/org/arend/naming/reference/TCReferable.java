package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;

public interface TCReferable extends LocatedReferable, DataContainer {
  TCReferable getTypecheckable();
}
