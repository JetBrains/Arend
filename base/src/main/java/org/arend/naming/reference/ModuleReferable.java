package org.arend.naming.reference;

import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ModuleReferable implements GlobalReferable {
  public final ModulePath path;

  public ModuleReferable(ModulePath path) {
    this.path = path;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return path.toString();
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleReferable that = (ModuleReferable) o;
    return Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    return path.toString();
  }
}
