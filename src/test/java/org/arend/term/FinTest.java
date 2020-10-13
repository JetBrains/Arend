package org.arend.term;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class FinTest extends TypeCheckingTestCase {
  @Test
  public void emptySet() {
    typeCheckDef("\\func xy (_ : Fin 0) : Empty" +
      "\\where \\data Empty");
  }

  @Test
  public void literalFin() {
    typeCheckDef("\\func ren : Fin 1 => 0");
    typeCheckDef("\\func xyr : Fin 2 => 1");
    typeCheckDef("\\func xyren : Fin 101 => 100");
  }

  @Test
  public void unfiniteFin() {
    typeCheckDef("\\func dalao : 0 = {Nat} (\\let | x : Fin 1 => 0 \\in x) => idp");
    typeCheckDef("\\func julao : 0 = {Fin 1} (\\let | x : Fin 1 => 0 \\in x) => idp");
  }

  @Test
  public void weakenFin() {
    typeCheckDef("\\func julao : Fin 2 => 0");
    typeCheckDef("\\func juruo : Fin 514 => 114");
  }
}
