package org.arend.typechecking.definition;

import org.arend.Matchers;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.LevelMismatchError;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AxiomTest extends TypeCheckingTestCase {
  @Test
  public void axiomTest() {
    Definition def = typeCheckDef("\\axiom test (x : Nat) : x = x");
    assertTrue(((FunctionDefinition) def).isAxiom());
    assertEquals(CoreFunctionDefinition.Kind.LEMMA, ((FunctionDefinition) def).getKind());
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, def.status());
  }

  @Test
  public void notPropTest() {
    typeCheckDef("\\axiom test (x : Nat) : Nat", 1);
    assertThatErrorsAre(Matchers.typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void axiomBodyTest() {
    typeCheckDef("\\axiom test (x : Nat) : x = x => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(CertainTypecheckingError.Kind.AXIOM_WITH_BODY));
  }

  @Test
  public void axiomBodyTest2() {
    typeCheckDef("\\axiom test (x : Nat) : x = x \\elim x", 1);
    assertThatErrorsAre(Matchers.typecheckingError(CertainTypecheckingError.Kind.AXIOM_WITH_BODY));
  }

  @Test
  public void dependenciesTest() {
    typeCheckModule(
      "\\axiom axiom1 (x : Nat) : x = x\n" +
      "\\axiom axiom2 (x : Nat) : x = x\n" +
      "\\lemma foo => axiom1 3\n" +
      "\\lemma bar => axiom2 3\n" +
      "\\lemma baz => (foo,bar)");
    assertEquals(Collections.singleton(getDefinition("axiom1")), getDefinition("foo").getAxioms());
    assertEquals(Collections.singleton(getDefinition("axiom2")), getDefinition("bar").getAxioms());
    assertEquals(new HashSet<>(Arrays.asList(getDefinition("axiom1"), getDefinition("axiom2"))), getDefinition("baz").getAxioms());
  }

  @Test
  public void axiomDependencyTest() {
    typeCheckModule(
      "\\axiom axiom1 (x : Nat) : x = x\n" +
      "\\axiom axiom2 (x : Nat) : axiom1 x = axiom1 x");
    assertEquals(new HashSet<>(Arrays.asList(getDefinition("axiom1"), getDefinition("axiom2"))), getDefinition("axiom2").getAxioms());
  }

  @Test
  public void mutuallyRecursiveTest() {
    typeCheckModule(
      "\\axiom axiom1 (x : Nat) : x = x\n" +
      "\\axiom axiom2 (x : Nat) : x = x\n" +
      "\\lemma foo (n : Nat) : 0 = 0\n" +
      "  | 0 => axiom1 0\n" +
      "  | suc n => bar n\n" +
      "\\lemma bar (n : Nat) : 0 = 0\n" +
      "  | 0 => axiom2 0\n" +
      "  | suc n => foo n");
    Set<Definition> axioms = new HashSet<>(Arrays.asList(getDefinition("axiom1"), getDefinition("axiom2")));
    assertEquals(axioms, getDefinition("foo").getAxioms());
    assertEquals(axioms, getDefinition("bar").getAxioms());
  }

  @Test
  public void mutuallyRecursiveTest2() {
    typeCheckModule(
      "\\axiom axiom (x : Nat) (d : D) : x = x\n" +
      "\\data D : \\Set0\n" +
      "  | con1\n" +
      "  | con2 (axiom 0 con1 = idp) (foo 0 con1 = idp)\n" +
      "\\lemma foo (n : Nat) (d : D) : 0 = 0 \\elim n\n" +
      "  | 0 => idp\n" +
      "  | suc n => idp");
    Set<Definition> axioms = Collections.singleton(getDefinition("axiom"));
    assertEquals(axioms, getDefinition("axiom").getAxioms());
    assertEquals(axioms, getDefinition("D").getAxioms());
    assertEquals(axioms, getDefinition("foo").getAxioms());
  }
}
