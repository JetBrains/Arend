package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.typeMismatchError;

public class LemmaTest extends TypeCheckingTestCase {
  @Test
  public void lemmaTyped() {
    typeCheckModule("\\lemma f : 0 = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void lemmaUntyped() {
    typeCheckModule("\\lemma f => path (\\lam _ => 0)");
  }

  @Test
  public void lemmaNotPropTyped() {
    typeCheckModule("\\lemma f : Nat => 0", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lemmaNotPropUntyped() {
    typeCheckModule("\\lemma f => 0", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lemmaRecursive() {
    typeCheckModule(
      "\\lemma f (n : Nat) : 0 Nat.+ n = n\n" +
      "  | 0 => path (\\lam _ => 0)\n" +
      "  | suc n => path (\\lam i => suc (f n @ i))");
  }

  @Test
  public void lemmaNotPropRecursive() {
    typeCheckModule(
      "\\lemma f (n : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc n => n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lemmaCowith() {
    typeCheckModule(
      "\\class C (n m : Nat)\n" +
      "\\lemma f : C \\cowith\n" +
      "  | n => 0\n" +
      "  | m => 1", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lemmaNew() {
    typeCheckModule(
      "\\class C (n m : Nat)\n" +
      "\\lemma f => \\new C {\n" +
      "  | n => 0\n" +
      "  | m => 1\n" +
      "}");
  }

  @Test
  public void lemmaCowithFieldProp() {
    typeCheckModule(
      "\\class C (n : Nat) { \\field x : 0 = 0 }\n" +
      "\\lemma f : C 0 \\cowith\n" +
      "  | x => path (\\lam _ => 0)\n" +
      "\\func g : f.x = path (\\lam _ => 0) => path (\\lam _ => path (\\lam _ => 0))", 1);
  }
}
