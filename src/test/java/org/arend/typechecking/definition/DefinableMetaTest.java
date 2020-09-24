package org.arend.typechecking.definition;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class DefinableMetaTest extends TypeCheckingTestCase {
  @Test
  public void noArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red \\where \\meta red => 114514");
    assertEquals("114514", ((Expression) def.getBody()).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void uniArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 \\where \\meta red x => x Nat.+ 514");
    assertEquals(String.valueOf(114 + 514), ((Expression) def.getBody()).normalize(NormalizationMode.WHNF).toString());
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
  public void appBody() {
    var def = (FunctionDefinition) typeCheckDef("\\func alendia => matchy suc zero \\where \\meta matchy f x => f x");
    assertEquals("1", ((Expression) def.getBody()).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void invokeTwice() {
    var def = (FunctionDefinition) typeCheckDef("\\func him => self 65 Nat.+ self 65 \\where \\meta self x => x");
    assertEquals(String.valueOf(65 + 65), ((Expression) def.getBody()).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void invokeManyTimes() {
    var def = (FunctionDefinition) typeCheckDef("\\func him => self 65 Nat.+ self 65 Nat.+ self 65 Nat.+ self 65 \\where \\meta self x => x");
    assertEquals(String.valueOf(65 + 65 + 65 + 65), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void hierarchy() {
    var def = (FunctionDefinition) typeCheckDef("\\func zhang => ice \\where {\n" +
      "  \\meta ice => alendia.tesla\n" +
      "  \\func alendia => 1 \\where \\meta tesla => 1\n" +
      "}");
    assertEquals(String.valueOf(1), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void biArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 514 \\where \\meta red x y => x Nat.+ y");
    assertEquals(String.valueOf(114 + 514), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }
}
