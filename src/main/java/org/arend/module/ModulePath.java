package org.arend.module;

import org.arend.util.LongName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModulePath extends LongName {
  public ModulePath(List<String> path) {
    super(path);
  }

  public ModulePath(String name) {
    super(Collections.singletonList(name));
  }

  public static ModulePath moduleName(String... module) {
    return new ModulePath(Arrays.asList(module));
  }

  public static ModulePath fromString(String path) {
    return moduleName(path.split("\\."));
  }
}
