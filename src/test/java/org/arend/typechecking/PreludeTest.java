package org.arend.typechecking;

import org.arend.Matchers;
import org.arend.core.definition.Definition;
import org.arend.ext.ArendPrelude;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class PreludeTest extends TypeCheckingTestCase {
  @Test
  public void testForEach() {
    var obj = new Prelude();
    List<Definition> fields = new ArrayList<>();
    for (Method method : ArendPrelude.class.getDeclaredMethods()) {
      try {
        Object result = method.invoke(obj);
        Definition def = result instanceof Definition ? (Definition) result : result instanceof TCDefReferable ? ((TCDefReferable) result).getTypechecked() : null;
        if (def != null) fields.add(def);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    Prelude.forEach(it -> {
      if (!fields.remove(it)) {
        fail(it.getName() + " is traversed but is not a prelude definition!");
      }
    });
    if (!fields.isEmpty()) {
      fail(fields.stream()
        .map(Definition::getName)
        .collect(Collectors.joining(", ", "prelude definition(s) ", " not traversed!")));
    }
  }

  @Test
  public void testCoe2Sigma() {
    typeCheckModule("\\func foo (A : \\Type) (i j : I) (a : A) : coe2 (\\lam _ => A) i a j = a => idp");
  }

  @Test
  public void testCoe2Left() {
    typeCheckModule("\\func foo (A : I -> \\Type) (j : I) (a : A left) : coe2 A left a j = coe A a j => idp");
  }

  @Test
  public void testCoe2Right() {
    typeCheckModule("\\func foo (A : I -> \\Type) (i : I) (a : A i) : coe2 A i a right = coe2 A i a right => path (\\lam _ => coe (\\lam k => A (I.squeezeR i k)) a right)");
  }

  @Test
  public void testCoe2RightRight() {
    typeCheckModule("\\func foo (A : I -> \\Type) (a : A right) : coe2 A right a right = a => idp");
  }

  // This works only with a stricter version of iso, see CompareVisitor
  @Test
  public void testIsoComparison() {
    typeCheckModule(
      "\\func id (x : Nat) => x\n" +
      "\\func id' (x : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc n => suc n\n" +
      "\\func id'-id (x : Nat) : id' x = x\n" +
      "  | 0 => idp\n" +
      "  | suc n => idp\n" +
      "\\func test : path (iso id id (\\lam _ => idp) (\\lam _ => idp)) = path (iso id id' id'-id id'-id) => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void testIsoComparisonError() {
    typeCheckModule(
      "\\func id (x : Nat) => x\n" +
      "\\func id' (x : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc n => suc n\n" +
      "\\func id'-id (x : Nat) : id' x = x\n" +
      "  | 0 => idp\n" +
      "  | suc n => idp\n" +
      "\\func test : path (iso id id (\\lam _ => idp) (\\lam _ => idp)) = path (iso id' id id'-id id'-id) => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  // This works only with a stricter version of iso, see NormalizeVisitor
  @Test
  public void isoFreeVarEval() {
    typeCheckModule(
      "\\func id (x : Nat) => x\n" +
      "\\func id' (i : I) (x : Nat) : Nat \\elim x\n" +
      "  | 0 => 0\n" +
      "  | suc n => suc n\n" +
      "\\func id'-id (i : I) (x : Nat) : id' i x = x \\elim x\n" +
      "  | 0 => idp\n" +
      "  | suc n => idp\n" +
      "\\func test : coe (\\lam i => iso id (id' i) (id'-id i) (id'-id i) i) 0 right = 0 => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void isoFreeVarEvalError() {
    typeCheckModule(
      "\\func id (x : Nat) => x\n" +
      "\\func id' (i : I) (x : Nat) : Nat \\elim x\n" +
      "  | 0 => 0\n" +
      "  | suc n => suc n\n" +
      "\\func id'-id (i : I) (x : Nat) : id' i x = x \\elim x\n" +
      "  | 0 => idp\n" +
      "  | suc n => idp\n" +
      "\\func test : coe (\\lam i => iso (id' i) id (id'-id i) (id'-id i) i) 0 right = 0 => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }
}
