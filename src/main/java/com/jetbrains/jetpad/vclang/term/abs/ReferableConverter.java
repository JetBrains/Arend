package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

public interface ReferableConverter {
  Referable toDataReferable(Referable referable);
  LocatedReferable toDataLocatedReferable(LocatedReferable referable);
}
