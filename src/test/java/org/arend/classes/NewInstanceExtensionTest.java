package org.arend.classes;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.NewExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelPair;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class NewInstanceExtensionTest extends TypeCheckingTestCase {
  @Test
  public void extTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : \\new r { | y => 2 } = \\new R r.x 2 => idp");
  }

  @Test
  public void withoutNew() {
    // This test produces an error on resolving since the resolver preprocess bodies and types of functions and R is not resolved at this stage.
    resolveNamesModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => r { | x => 2 }", 1);
  }

  @Test
  public void withoutNew2() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => (1, r { | x => 2 })", 1);
  }

  @Test
  public void extDepTest() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R) (q : r.x = r.x) : \\new r { | p => q } = \\new R r.x q => idp");
  }

  @Test
  public void extDepTest2() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R) => \\new r { | x => 2 }", 1);
  }

  @Test
  public void extDepTest3() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R 2) => \\new r { | x => 2 }", 1);
  }

  @Test
  public void extDepTest4() {
    typeCheckModule(
      """
        \\record R (x : Nat) (p : x = x)
        \\record S \\extends R | x => 2
        \\lemma f (s : S) => \\new s
        """);
  }

  @Test
  public void newInstTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => \\let t : R r.x r.y => \\new r \\in t");
  }

  @Test
  public void levelTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (a : A)\n" +
      "\\func f : \\Sigma C Nat => (\\new C \\levels 1 1 Nat 0, 0)");
    assertEquals(new LevelPair(new Level(1), new Level(1)), ((Expression) Objects.requireNonNull(((FunctionDefinition) getDefinition("f")).getBody())).cast(TupleExpression.class).getFields().get(0).cast(NewExpression.class).getClassCall().getLevels());
  }

  @Test
  public void replacementTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : R r.x => r");
  }
}
