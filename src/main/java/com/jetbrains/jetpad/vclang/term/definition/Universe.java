package com.jetbrains.jetpad.vclang.term.definition;

public abstract class Universe {
  private final int myLevel;
  public static final int NO_LEVEL = -10;
  public enum Cmp { EQUALS, NOT_COMPARABLE, LESS, GREATER }

  public Universe(int level) {
    myLevel = level;
  }

  public Universe() {
    myLevel = NO_LEVEL;
  }

  public int getLevel() {
    return myLevel;
  }

  public abstract Cmp compare(Universe other);
  public abstract Universe succ();

  public boolean lessOrEquals(Universe other) {
    Cmp result = compare(other);
    return result == Cmp.LESS || result == Cmp.EQUALS;
  }

  public Universe max(Universe other) {
    Cmp result = compare(other);
    if (result == Cmp.GREATER || result == Cmp.EQUALS) return this;
    if (result == Cmp.LESS) return other;
    return null;
  }

  protected Cmp compareLevels(int other) {
    if (myLevel == other) return Cmp.EQUALS;
    if (myLevel == NO_LEVEL) return Cmp.GREATER;
    if (other == NO_LEVEL || myLevel < other) return Cmp.LESS;
    return Cmp.GREATER;
  }

  public static class Type extends Universe {
    private final int myTruncated;
    public static final int NOT_TRUNCATED = -20;
    public static final int PROP = -1;
    public static final int SET = 0;

    public Type(int level, int truncated) {
      super(level);
      myTruncated = truncated;
    }

    public Type(int level) {
      super(level);
      myTruncated = NOT_TRUNCATED;
    }

    public Type() {
      myTruncated = NOT_TRUNCATED;
    }

    int getTruncated() {
      return myTruncated;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof Type)) return false;
      if (myTruncated == PROP && ((Type) other).myTruncated == PROP) return true;
      return myTruncated == ((Type) other).myTruncated && getLevel() == ((Type) other).getLevel();
    }

    @Override
    public String toString() {
      if (myTruncated == PROP) return "\\Prop";
      String level = getLevel() == NO_LEVEL ? "" : Integer.toString(getLevel());
      if (myTruncated == SET) return "\\Set" + level;
      return "\\" + (myTruncated == NOT_TRUNCATED ? "" : myTruncated + "-") + "Type" + level;
    }

    private Cmp compareTruncated(int other) {
      if (myTruncated == other) return Cmp.EQUALS;
      if (myTruncated == NOT_TRUNCATED) return Cmp.GREATER;
      if (other == NOT_TRUNCATED || myTruncated < other) return Cmp.LESS;
      return Cmp.GREATER;
    }

    @Override
    public Cmp compare(Universe other) {
      if (this == other) return Cmp.EQUALS;
      if (!(other instanceof Type)) return Cmp.NOT_COMPARABLE;
      if (myTruncated == PROP) {
        return ((Type) other).myTruncated == PROP ? Cmp.EQUALS : Cmp.LESS;
      }
      if (((Type) other).getTruncated() == PROP) {
        return Cmp.GREATER;
      }

      Cmp r1 = compareLevels(other.getLevel());
      Cmp r2 = compareTruncated(((Type) other).getTruncated());
      if (r1 == Cmp.NOT_COMPARABLE) return Cmp.NOT_COMPARABLE;
      if (r1 == Cmp.LESS) {
        return r2 == Cmp.LESS || r2 == Cmp.EQUALS ? Cmp.LESS : Cmp.NOT_COMPARABLE;
      }
      if (r1 == Cmp.GREATER) {
        return r2 == Cmp.GREATER || r2 == Cmp.EQUALS ? Cmp.GREATER : Cmp.NOT_COMPARABLE;
      }
      return r2;
    }

    @Override
    public Type succ() {
      return new Type(getLevel() == NO_LEVEL ? NO_LEVEL : getLevel() + 1, myTruncated);
    }
  }
}
