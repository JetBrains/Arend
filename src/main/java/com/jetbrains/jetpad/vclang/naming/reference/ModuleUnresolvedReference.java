package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nonnull;
import java.util.List;

public class ModuleUnresolvedReference implements Referable {
  private final ModulePath myModulePath;
  private final List<String> myNames;

  public ModuleUnresolvedReference(ModulePath modulePath, List<String> names) {
    myModulePath = modulePath;
    myNames = names;
  }

  public ModulePath getModulePath() {
    return myModulePath;
  }

  public List<String> getNames() {
    return myNames;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    builder.append(myModulePath);
    for (String name : myNames) {
      builder.append('.').append(name);
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleUnresolvedReference that = (ModuleUnresolvedReference) o;

    return myModulePath.equals(that.myModulePath) && myNames.equals(that.myNames);
  }

  @Override
  public int hashCode() {
    int result = myModulePath.hashCode();
    result = 31 * result + myNames.hashCode();
    return result;
  }
}
