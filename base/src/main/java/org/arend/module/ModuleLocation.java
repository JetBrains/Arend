package org.arend.module;

import org.arend.ext.module.ModulePath;

import java.util.Objects;

public class ModuleLocation {
  public enum LocationKind { SOURCE, TEST, GENERATED }

  private final String myLibraryName;
  private final LocationKind myLocationKind;
  private final ModulePath myModulePath;

  public ModuleLocation(String libraryName, LocationKind locationKind, ModulePath modulePath) {
    myLibraryName = libraryName;
    myLocationKind = locationKind;
    myModulePath = modulePath;
  }

  public String getLibraryName() {
    return myLibraryName;
  }

  public LocationKind getLocationKind() {
    return myLocationKind;
  }

  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleLocation that = (ModuleLocation) o;
    return myLibraryName.equals(that.myLibraryName) &&
      myLocationKind == that.myLocationKind &&
      myModulePath.equals(that.myModulePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myLibraryName, myLocationKind, myModulePath);
  }

  @Override
  public String toString() {
    return myModulePath.toString();
  }
}
