package org.arend.typechecking.implicitargs.equations;

import java.util.Objects;

public class LevelEquation<Var> {
  private final Var myVar1;
  private final Var myVar2;
  private final Integer myConstant;
  private final Integer myMaxConstant;

  public LevelEquation(Var var1, Var var2, int constant, Integer maxConstant) {
    myVar1 = var1;
    myVar2 = var2;
    myConstant = constant;
    myMaxConstant = maxConstant;
  }

  public LevelEquation(Var var1, Var var2, int constant) {
    myVar1 = var1;
    myVar2 = var2;
    myConstant = constant;
    myMaxConstant = null;
  }

  public LevelEquation(Var var) {
    myVar1 = null;
    myVar2 = var;
    myConstant = null;
    myMaxConstant = null;
  }

  public LevelEquation(LevelEquation<? extends Var> equation) {
    myVar1 = equation.myVar1;
    myVar2 = equation.myVar2;
    myConstant = equation.myConstant;
    myMaxConstant = equation.myMaxConstant;
  }

  public boolean isInfinity() {
    return myConstant == null;
  }

  public Var getVariable1() {
    if (myConstant == null) {
      throw new IllegalStateException();
    }
    return myVar1;
  }

  public Var getVariable2() {
    if (myConstant == null) {
      throw new IllegalStateException();
    }
    return myVar2;
  }

  public int getConstant() {
    if (myConstant == null) {
      throw new IllegalStateException();
    }
    return myConstant;
  }

  public Integer getMaxConstant() {
    if (myConstant == null) {
      throw new IllegalStateException();
    }
    return myMaxConstant;
  }

  public Var getVariable() {
    if (myConstant != null) {
      throw new IllegalStateException();
    }
    return myVar2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LevelEquation<?> that = (LevelEquation<?>) o;

    if (!Objects.equals(myVar1, that.myVar1)) return false;
    if (!Objects.equals(myVar2, that.myVar2)) return false;
    if (!Objects.equals(myConstant, that.myConstant)) return false;
    return Objects.equals(myMaxConstant, that.myMaxConstant);
  }

  @Override
  public int hashCode() {
    int result = myVar1 != null ? myVar1.hashCode() : 0;
    result = 31 * result + (myVar2 != null ? myVar2.hashCode() : 0);
    result = 31 * result + (myConstant != null ? myConstant.hashCode() : 0);
    result = 31 * result + (myMaxConstant != null ? myMaxConstant.hashCode() : 0);
    return result;
  }
}
