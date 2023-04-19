package org.arend.ext;

import org.arend.ext.core.definition.*;
import org.arend.ext.reference.ArendRef;

/**
 * Provides access to the definitions in the prelude.
 */
public interface ArendPrelude {
  CoreDataDefinition getInterval();
  CoreConstructor getLeft();
  CoreConstructor getRight();
  CoreFunctionDefinition getSqueeze();
  CoreFunctionDefinition getSqueezeR();
  CoreDataDefinition getNat();
  CoreConstructor getZero();
  CoreConstructor getSuc();
  CoreFunctionDefinition getPlus();
  CoreFunctionDefinition getMul();
  CoreFunctionDefinition getMinus();
  CoreDataDefinition getFin();
  CoreFunctionDefinition getFinFromNat();
  CoreDataDefinition getInt();
  CoreConstructor getPos();
  CoreConstructor getNeg();
  CoreDataDefinition getString();
  CoreFunctionDefinition getCoerce();
  CoreFunctionDefinition getCoerce2();
  CoreDataDefinition getPath();
  CoreFunctionDefinition getEquality();
  ArendRef getPathConRef();
  CoreFunctionDefinition getInProp();
  CoreFunctionDefinition getIdp();
  ArendRef getAtRef();
  CoreFunctionDefinition getIso();
  CoreFunctionDefinition getDivMod();
  CoreFunctionDefinition getDiv();
  CoreFunctionDefinition getMod();
  CoreFunctionDefinition getDivModProp();
  CoreClassDefinition getDArray();
  CoreFunctionDefinition getArray();
  CoreClassField getArrayElementsType();
  CoreClassField getArrayLength();
  CoreClassField getArrayAt();
  CoreFunctionDefinition getEmptyArray();
  CoreFunctionDefinition getArrayCons();
  CoreFunctionDefinition getArrayIndex();
}
