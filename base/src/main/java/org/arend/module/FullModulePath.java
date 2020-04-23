package org.arend.module;

import org.arend.ext.module.ModulePath;

import java.util.List;
import java.util.Objects;

public class FullModulePath extends ModulePath {
  public enum LocationKind { SOURCE, TEST, GENERATED }

  private final String myLibraryName;
  private final LocationKind myLocationKind;

  public FullModulePath(String myLibraryName, LocationKind myLocationKind, List<String> path) {
    super(path);
    this.myLibraryName = myLibraryName;
    this.myLocationKind = myLocationKind;
  }

  public String getLibraryName() {
    return myLibraryName;
  }

  public LocationKind getLocationKind() {
    return myLocationKind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    FullModulePath that = (FullModulePath) o;
    return Objects.equals(myLibraryName, that.myLibraryName) &&
      myLocationKind == that.myLocationKind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myLibraryName, myLocationKind);
  }
}
