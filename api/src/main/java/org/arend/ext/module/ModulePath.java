package org.arend.ext.module;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ModulePath extends LongName {
  public ModulePath(@NotNull List<String> path) {
    super(path);
  }

  public ModulePath(String... name) {
    super(Arrays.asList(name));
  }

  public static ModulePath fromString(String path) {
    return new ModulePath(path.split("\\."));
  }
}
