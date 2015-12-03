package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

public enum CompareResult {
  GREATER, LESS, EQUIV, NOT_EQUIV, MAYBE_GREATER, MAYBE_LESS, MAYBE_EQUIV;

  public boolean isEquiv() {
    return this == EQUIV || this == MAYBE_EQUIV;
  }

  public CompareResult mustBeEquiv() {
    return isEquiv() ? this : NOT_EQUIV;
  }

  public CompareResult and(CompareResult result) {
    switch (this) {
      case GREATER:
        switch (result) {
          case GREATER:
          case EQUIV:
            return CompareResult.GREATER;
          case MAYBE_GREATER:
          case MAYBE_EQUIV:
            return CompareResult.MAYBE_GREATER;
          default:
            return CompareResult.NOT_EQUIV;
        }
      case LESS:
        switch (result) {
          case LESS:
          case EQUIV:
            return CompareResult.LESS;
          case MAYBE_LESS:
          case MAYBE_EQUIV:
            return CompareResult.MAYBE_LESS;
          default:
            return CompareResult.NOT_EQUIV;
        }
      case EQUIV:
        return result;
      case NOT_EQUIV:
        return CompareResult.NOT_EQUIV;
      case MAYBE_GREATER:
        return result == CompareResult.NOT_EQUIV || result == CompareResult.LESS || result == CompareResult.MAYBE_LESS ? CompareResult.NOT_EQUIV : CompareResult.MAYBE_GREATER;
      case MAYBE_LESS:
        return result == CompareResult.NOT_EQUIV || result == CompareResult.GREATER || result == CompareResult.MAYBE_GREATER ? CompareResult.NOT_EQUIV : CompareResult.MAYBE_LESS;
      case MAYBE_EQUIV:
        switch (result) {
          case GREATER:
          case MAYBE_GREATER:
            return CompareResult.MAYBE_GREATER;
          case LESS:
          case MAYBE_LESS:
            return CompareResult.MAYBE_LESS;
          case EQUIV:
          case MAYBE_EQUIV:
            return CompareResult.MAYBE_EQUIV;
          case NOT_EQUIV:
            return CompareResult.NOT_EQUIV;
        }
    }
    throw new IllegalStateException();
  }
}
