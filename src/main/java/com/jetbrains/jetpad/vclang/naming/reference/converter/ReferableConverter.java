package com.jetbrains.jetpad.vclang.naming.reference.converter;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

public interface ReferableConverter {
  Referable toDataReferable(Referable referable);
  TCReferable toDataLocatedReferable(LocatedReferable referable);
}
