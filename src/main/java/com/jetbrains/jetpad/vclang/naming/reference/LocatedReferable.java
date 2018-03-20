package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nullable;
import java.util.List;

public interface LocatedReferable extends GlobalReferable {
  @Nullable
  ModulePath getLocation(List<? super String> fullName);
}
