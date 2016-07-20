package com.jetbrains.jetpad.vclang.term.expr.subst;

public class Substitution {
  public ExprSubstitution exprSubst = new ExprSubstitution();
  public LevelSubstitution levelSubst = new LevelSubstitution();

  public Substitution() {
  }

  public Substitution(ExprSubstitution subst) {
    exprSubst = subst;
  }

  public Substitution(LevelSubstitution subst) {
    levelSubst = subst;
  }

  public boolean isEmpty() {
    return exprSubst.getDomain().isEmpty() && levelSubst.getDomain().isEmpty();
  }
}
