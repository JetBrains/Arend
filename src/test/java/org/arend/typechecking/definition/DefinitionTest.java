package org.arend.typechecking.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.subst.LevelPair;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.arend.ExpressionFactory.Pi;
import static org.arend.ExpressionFactory.fromPiParameters;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class DefinitionTest extends TypeCheckingTestCase {
  @Test
  public void function() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\func f : Nat => 0");
    assertNotNull(typedDef);
    assertSame(typedDef.status(), Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void functionUntyped() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\func f => 0");
    assertNotNull(typedDef);
    assertSame(typedDef.status(), Definition.TypeCheckingStatus.NO_ERRORS);
    assertEquals(Nat(), typedDef.getTypeWithParams(new ArrayList<>(), LevelPair.SET0));
  }

  @Test
  public void functionWithArgs() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\func f (x : Nat) (y : Nat -> Nat) => y");
    assertNotNull(typedDef);
    assertSame(typedDef.status(), Definition.TypeCheckingStatus.NO_ERRORS);
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, LevelPair.SET0);
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), fromPiParameters(type, params));
  }

  @Test
  public void errorInParameters() {
    typeCheckModule(
        "\\data E (n : Nat) | e\n" +
        "\\data D (n : Nat -> Nat) (E n) | d\n" +
        "\\func test => D", 1);
  }

  @Test
  public void errorInParametersCon() {
    typeCheckModule(
        "\\data E (n : Nat) | e\n" +
        "\\data D (n : Nat -> Nat) (E n) | d\n" +
        "\\func test => d", 1);
  }

  @Test
  public void patternVector() {
    typeCheckDef("\\data Vec (A : \\Type0) (n : Nat) \\elim n | zero => Nil | suc m => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    typeCheckModule(
        "\\data D (n : Nat) (n = n) \\elim n | zero => d\n" +
        "\\data C {n : Nat} {p : n = n} (D n p) \\elim n | zero => c (p = p)");
  }

  @Test
  public void patternDepParams2() {
    typeCheckModule(
        "\\data D (n : Nat) (n = n) \\elim n | zero => d\n" +
        "\\data C {n : Nat} {p : n = n} (D n p) | c (p = p)");
  }

  @Test
  public void patternNested() {
    typeCheckDef("\\data C Nat \\with | suc (suc n) => c2 (n = n)");
  }

  @Test
  public void patternDataLE() {
    typeCheckDef("\\data LE Nat Nat \\with | zero, m => LE-zero | suc n, suc m => LE-suc (LE n m)");
  }

  @Test
  public void patternImplicitError() {
    typeCheckDef("\\data D Nat \\with | {A} => d", 1);
  }

  @Test
  public void patternConstructorCall() {
    typeCheckModule(
        "\\data D {Nat} \\with | {zero} => d\n" +
        "\\func test => d");
  }

  @Test
  public void patternAbstract() {
    typeCheckModule(
        "\\data Wheel | wheel\n" +
        "\\data VehicleType | bikeType | carType\n" +
        "\\data Vehicle VehicleType \\with\n" +
        "  | carType => car Wheel Wheel Wheel Wheel" +
        "  | bikeType => bike Wheel Wheel");
  }

  @Test
  public void patternLift() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d\n" +
        "\\data C (m n : Nat) (d : D m) \\elim m, n | zero, zero => c");
  }

  @Test
  public void patternLift2() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d\n" +
        "\\data C (m n : Nat) (D m) \\elim n | zero => c");
  }

  @Test
  public void patternMultipleSubst() {
    typeCheckModule(
        "\\data D (n m : Nat) | d (n = n) (m = m)\n" +
        "\\data C | c (n m : Nat) (D n m)\n" +
        "\\data E C \\with | c zero (suc zero) (d _ _) => e\n" +
        "\\func test => e {path (\\lam _ => 0)} {path (\\lam _ => 1)}");
  }

  @Test
  public void patternConstructorDefCall() {
    typeCheckModule(
        "\\data D (n m : Nat) \\elim n, m | suc n, suc m => d (n = n) (m = m)\n" +
        "\\func test => d (path (\\lam _ => 1)) (path (\\lam _ => 0))");
  }

  @Test
  public void patternConstructorDefCallError() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d\n" +
        "\\func test (n : Nat) : D n => d", 1);
  }

  @Test
  public void patternSubstTest() {
    typeCheckModule(
        "\\data E Nat \\with | zero => e\n" +
        "\\data D (n : Nat) (E n) \\elim n | zero => d\n" +
        "\\func test => d");
  }

  @Test
  public void patternExpandArgsTest() {
    typeCheckModule(
        "\\data D (n : Nat) | d (n = n)\n" +
        "\\data C (D 1) \\with | d p => c\n" +
        "\\func test : C (d (path (\\lam _ => 1))) => c");
  }

  @Test
  public void patternNormalizeTest() {
    typeCheckModule(
        "\\data E (x : 0 = 0) | e\n" +
        "\\data C (n m : Nat) \\with | suc n', suc (suc n) => c (n = n)\n" +
        "\\data D ((\\lam (x : \\Type0) => x) (C 1 2)) \\with | c p => x (E p)\n" +
        "\\func test => x (e {path (\\lam _ => 0)})");
  }

  @Test
  public void patternNormalizeTest1() {
    typeCheckModule(
        "\\data E (x : 0 = 0) | e\n" +
        "\\data C Nat Nat \\with | suc n', suc (suc n) => c (n = n)\n" +
        "\\data D ((\\lam (x : \\Type0) => x) (C 1 1)) \\with | c p => x (E p)", 1);
  }

  @Test
  public void patternTypeCheck() {
    typeCheckModule(
        "\\func f (x : Nat -> Nat) => x 0\n" +
        "\\data Test (A : \\Set0) \\with\n" +
        "  | suc n => foo (f n)", 1);
  }

  @Test
  public void fieldsEvaluation() {
    typeCheckModule(
      "\\class C (X : \\Type) | x0 : X | x1 : \\Type -> X | x2 (A : \\Type) : x1 A = x0 -> Nat\n" +
      "\\instance NatC : C Nat 0 (\\lam _ => 0) (\\lam _ _ => 0)\n" +
      "\\func test : Nat => x0");
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("test").getUniverseKind());
  }

  @Test
  public void covariantField() {
    typeCheckModule(
      "\\record R (A : \\Type) (a : A)\n" +
      "\\record S \\extends R\n" +
      "  | f : A -> A");
    ClassField field = (ClassField) getDefinition("R.A");
    assertTrue(((ClassDefinition) getDefinition("R")).isCovariantField(field));
    assertFalse(((ClassDefinition) getDefinition("S")).isCovariantField(field));
  }

  @Test
  public void universeKindTest() {
    typeCheckModule(
      "\\data D (A : Nat -> \\Type) | con (A 0)\n" +
      "\\func f (A : Nat -> \\Type) => 0\n" +
      "\\record C (A : Nat -> \\Type) (a : A 0)\n" +
      "\\func g (A : Nat -> \\Type) => C A\n" +
      "\\record R \\extends C | A _ => \\Sigma");
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("f").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, ((ClassCallExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("g")).getBody())).getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, ((ClassDefinition) getDefinition("R")).getBaseUniverseKind());
    assertEquals(UniverseKind.ONLY_COVARIANT, getDefinition("R").getUniverseKind());
  }

  @Test
  public void universeKindTest2() {
    typeCheckModule(
      "\\data D (A : \\Type -> \\Type) | con (A (\\Sigma))\n" +
      "\\func f (A : \\Type -> \\Type) => 0\n" +
      "\\record C (A : \\Type -> \\Type) (a : A (\\Sigma))\n" +
      "\\func g (A : \\Type -> \\Type) => C A\n" +
      "\\record R \\extends C | A _ => \\Sigma");
    assertEquals(UniverseKind.WITH_UNIVERSES, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.WITH_UNIVERSES, getDefinition("f").getUniverseKind());
    assertEquals(UniverseKind.WITH_UNIVERSES, ((ClassCallExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("g")).getBody())).getUniverseKind());
    assertEquals(UniverseKind.WITH_UNIVERSES, getDefinition("R").getUniverseKind());
  }

  @Test
  public void universeKindTest3() {
    typeCheckModule(
      "\\data D (A : Nat -> \\Set) | con (A 0)\n" +
      "\\func f (A : Nat -> \\Set) => 0\n" +
      "\\record C (A : Nat -> \\Set) (a : A 0)\n" +
      "\\func g (A : Nat -> \\Set) => C A\n" +
      "\\record R \\extends C | A _ => \\Sigma");
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("f").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, ((ClassCallExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("g")).getBody())).getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, ((ClassDefinition) getDefinition("R")).getBaseUniverseKind());
    assertEquals(UniverseKind.ONLY_COVARIANT, getDefinition("R").getUniverseKind());
  }

  @Test
  public void universeKindTest4() {
    typeCheckModule(
      "\\data D (A : Nat -> \\Type (\\suc \\lp)) | con (A 0)\n" +
      "\\func f (A : Nat -> \\Type (\\suc \\lp)) => 0\n" +
      "\\record C (A : Nat -> \\Type (\\suc \\lp)) (a : A 0)" +
      "\\func g (A : Nat -> \\Type (\\suc \\lp)) => C A\n" +
      "\\record R \\extends C | A => \\lam _ => \\Sigma");
    assertEquals(UniverseKind.ONLY_COVARIANT, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.ONLY_COVARIANT, getDefinition("f").getUniverseKind());
    assertEquals(UniverseKind.ONLY_COVARIANT, ((ClassCallExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("g")).getBody())).getUniverseKind());
    assertEquals(UniverseKind.ONLY_COVARIANT, getDefinition("R").getUniverseKind());
  }

  @Test
  public void universeKindTest6() {
    typeCheckModule(
      "\\data D (A : Nat -> \\Type) : \\Type (\\max \\lp 1) | con (A 0)\n" +
      "\\func f (A : Nat -> \\Type) : \\Type (\\max \\lp 1) => Nat");
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("f").getUniverseKind());
  }

  @Test
  public void universeKindTest7() {
    typeCheckModule(
      "\\data D (A : Nat -> \\Type) : \\Type (\\suc \\lp) | con (A 0)\n" +
      "\\func f (A : Nat -> \\Type) : \\Type (\\suc \\lp) => Nat");
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("D").getUniverseKind());
    assertEquals(UniverseKind.NO_UNIVERSES, getDefinition("f").getUniverseKind());
  }

  @Test
  public void extensionLevelsTest() {
    typeCheckModule(
      "\\record A \\plevels p1 <= p2 <= p3\n" +
      "  | f : \\Sigma (\\hType p1) (\\hType p2)\n" +
      "\\record B \\plevels q1 <= q2 \\extends A (0, q1, \\suc q2)\n" +
      "\\record C \\plevels q1 <= q2 \\extends A (q1, q2, \\suc q2)\n" +
      "\\record D \\plevels q1 <= q2 \\extends A (1, 2, \\suc (\\suc q2))\n" +
      "\\record E \\plevels q1 <= q2 \\extends A (q1, \\suc q2, \\suc q2)");

    ClassField field = (ClassField) getDefinition("A.f");
    ClassDefinition bClass = (ClassDefinition) getDefinition("B");
    ClassDefinition cClass = (ClassDefinition) getDefinition("C");
    ClassDefinition dClass = (ClassDefinition) getDefinition("D");
    ClassDefinition eClass = (ClassDefinition) getDefinition("E");

    assertEquals(UniverseKind.NO_UNIVERSES, bClass.getBaseUniverseKind());
    assertTrue(bClass.isOmegaField(field));
    assertEquals(UniverseKind.NO_UNIVERSES, cClass.getBaseUniverseKind());
    assertTrue(cClass.isOmegaField(field));
    assertEquals(UniverseKind.NO_UNIVERSES, dClass.getBaseUniverseKind());
    assertFalse(dClass.isOmegaField(field));
    assertEquals(UniverseKind.ONLY_COVARIANT, eClass.getBaseUniverseKind());
  }

  @Test
  public void cycleTest() {
    typeCheckModule(
      "\\record R\n" +
      "  | X : \\Set\n" +
      "  | A : X -> D\n" +
      "\\record D (Y : R)\n" +
      "\\func f (e : \\Set) : R \\cowith\n" +
      "  | X => e\n" +
      "  | A x => \\new D {\n" +
      "    | Y => (\\this : R)\n" +
      "  }", -1);
  }
}
