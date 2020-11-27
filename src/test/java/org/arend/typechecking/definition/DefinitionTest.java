package org.arend.typechecking.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
    assertEquals(Nat(), typedDef.getTypeWithParams(new ArrayList<>(), Sort.SET0));
  }

  @Test
  public void functionWithArgs() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\func f (x : Nat) (y : Nat -> Nat) => y");
    assertNotNull(typedDef);
    assertSame(typedDef.status(), Definition.TypeCheckingStatus.NO_ERRORS);
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, Sort.SET0);
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
}
