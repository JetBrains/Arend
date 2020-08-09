package org.arend.typechecking.definition;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefinableMetaTest extends TypeCheckingTestCase {
  @Test
  public void noArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red \\where \\meta red => 114514");
    var body = (Expression) def.getBody();
    assertEquals("114514", body.normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void uniArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 \\where \\meta red x => x Nat.+ 514");
    var body = (Expression) def.getBody();
    assertEquals(String.valueOf(114 + 514), body.normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void tooManyArgSubst() {
    typeCheckDef("\\func thaut => warm 114 514 \\where \\meta warm x => x", 1);
  }

  @Test
  public void tooLittleArgSubst() {
    typeCheckDef("\\func thaut => warm \\where \\meta warm x => x", 1);
  }

  @Test
  public void biArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 514 \\where \\meta red x y => x Nat.+ y");
    var body = (Expression) def.getBody();
    assertEquals(String.valueOf(114 + 514), body.normalize(NormalizationMode.WHNF).toString());
  }
}
