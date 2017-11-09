package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

public interface FullNameProvider {
  FullName fullNameFor(GlobalReferable definition);
}
