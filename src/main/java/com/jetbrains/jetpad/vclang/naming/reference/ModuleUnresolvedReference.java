package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ModuleUnresolvedReference implements UnresolvedReference {
  private final Object myData;
  private final ModulePath myModulePath;
  private final List<String> myPath;
  private Referable myResolved;

  public ModuleUnresolvedReference(Object data, ModulePath modulePath, List<String> path) {
    myData = data;
    myModulePath = modulePath;
    myPath = path;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    builder.append(myModulePath);
    for (String name : myPath) {
      builder.append('.').append(name);
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleUnresolvedReference that = (ModuleUnresolvedReference) o;

    return myModulePath.equals(that.myModulePath) && myPath.equals(that.myPath);
  }

  @Override
  public int hashCode() {
    int result = myModulePath.hashCode();
    result = 31 * result + myPath.hashCode();
    return result;
  }

  @Nullable
  @Override
  public Referable resolve(Scope scope, NameResolver nameResolver) {
    if (myResolved != null) {
      return myResolved;
    }
    if (nameResolver == null) {
      myResolved = this;
      return this;
    }

    ModuleNamespace moduleNamespace = nameResolver.resolveModuleNamespace(myModulePath);
    if (moduleNamespace == null) {
      myResolved = this;
      return null;
    }

    myResolved = moduleNamespace.getRegisteredClass();
    if (myResolved == null) {
      myResolved = this;
      return null;
    }
    for (String name : myPath) {
      myResolved = nameResolver.nsProviders.statics.forReferable((GlobalReferable) myResolved).resolveName(name);
      if (myResolved == null) {
        myResolved = this;
        return null;
      }
    }
    return myResolved;
  }

  @Nonnull
  @Override
  public Referable resolveDynamic(GlobalReferable enclosingClass, NameResolver nameResolver) {
    if (myResolved != null) {
      return myResolved;
    }
    myResolved = this;
    return this;
  }
}
