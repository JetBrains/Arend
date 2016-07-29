package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

public class LevelEquation<Var> {
  public Var var1;
  public Var var2;
  public int constant;

  public LevelEquation(Var var1, Var var2, int constant) {
    this.var1 = var1;
    this.var2 = var2;
    this.constant = constant;
  }
}
