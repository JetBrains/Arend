package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class LibraryReferable implements GlobalReferable {
  public final String libraryName;

  public LibraryReferable(String libraryName) {
    this.libraryName = libraryName;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Override
  public boolean isModule() {
    return true;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return libraryName;
  }
}
