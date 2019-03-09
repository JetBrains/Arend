package org.arend.naming.reference;

public interface TCReferable extends LocatedReferable, DataContainer {
  TCReferable getTypecheckable();
}
