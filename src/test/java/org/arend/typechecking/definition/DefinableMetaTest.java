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

  // @Test
  public void uniArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 \\where \\meta red x => x Nat.+ 514");
    var body = (Expression) def.getBody();
    assertEquals(String.valueOf(114 + 514), body.normalize(NormalizationMode.WHNF).toString());
  }
}
