package org.arend.typechecking.typeclass;

import org.arend.Matchers;
import org.arend.core.definition.Constructor;
import org.arend.core.subst.LevelPair;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.ExpressionFactory.ConCall;

public class TypeClassesClassifiers extends TypeCheckingTestCase {
  @Test
  public void conTest() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\class C (d : D) | p : d = d\n" +
      "\\instance f : C con1 | p => idp\n" +
      "\\lemma g : con1 = con1 => p");
  }

  @Test
  public void conTestError() {
    typeCheckModule(
      "\\data D | con1 | con2\n" +
      "\\class C (d : D) | p : d = d\n" +
      "\\instance f : C con1 | p => idp\n" +
      "\\lemma g : con2 = con2 => p", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("C"), ConCall((Constructor) getDefinition("D.con2"), LevelPair.STD, Collections.emptyList())));
  }

  @Test
  public void partialConTest() {
    typeCheckModule(
      "\\data D | con1 (x y : Nat) | con2\n" +
      "\\class C (d : Nat -> D) | p : d = d\n" +
      "\\instance f : C (con1 0) | p => idp\n" +
      "\\lemma g : con1 0 = con1 0 => p");
  }

  @Test
  public void partialConTest2() {
    typeCheckModule(
      "\\data D | con1 (x y : Nat) | con2\n" +
      "\\class C (d : Nat -> D) | p (n : Nat) : d n = d n\n" +
      "\\instance f : C (con1 0) | p n => idp\n" +
      "\\func g : con1 0 2 = con1 0 2 => p 2");
  }

  @Test
  public void sigmaTest() {
    typeCheckModule(
      "\\class C (A : \\Type) | a : A\n" +
      "\\instance s : C (\\Sigma Nat Nat) | a => (1,2)\n" +
      "\\func f : \\Sigma Nat Nat => a");
  }

  @Test
  public void unitTest() {
    typeCheckModule(
      "\\class C (A : \\Type) | a : A\n" +
      "\\instance s : C (\\Sigma) | a => ()\n" +
      "\\func f : \\Sigma => a");
  }
}
