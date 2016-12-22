package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Pi;
import static org.junit.Assert.*;

public class DefinitionTest extends TypeCheckingTestCase {
  @Test
  public void function() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f : Nat => 0");
    assertNotNull(typedDef);
    assertTrue(typedDef.hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
  }

  @Test
  public void functionUntyped() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f => 0");
    assertNotNull(typedDef);
    assertTrue(typedDef.hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
    assertEquals(Nat(), typedDef.getTypeWithParams(new ArrayList<DependentLink>(), new LevelArguments()));
  }

  @Test
  public void functionWithArgs() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f (x : Nat) (y : Nat -> Nat) => y");
    assertNotNull(typedDef);
    assertTrue(typedDef.hasErrors() == Definition.TypeCheckingStatus.NO_ERRORS);
    List<DependentLink> params = new ArrayList<>();
    TypeMax type = typedDef.getTypeWithParams(params, new LevelArguments());
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), type.fromPiParameters(params));
  }

  @Test
  public void errorInParameters() {
    typeCheckClass(
        "\\data E (n : Nat) | e\n" +
        "\\data D (n : Nat -> Nat) (E n) | d\n" +
        "\\function test => D", 2);
  }

  @Test
  public void errorInParametersCon() {
    typeCheckClass(
        "\\data E (n : Nat) | e\n" +
        "\\data D (n : Nat -> Nat) (E n) | d\n" +
        "\\function test => d", 2);
  }

  @Test
  public void patternVector() {
    typeCheckDef("\\data Vec \\Type0 Nat | Vec _ zero => Nil | Vec A (suc m) => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    typeCheckClass(
        "\\data D (n : Nat) (n = n) | D zero _ => d\n" +
        "\\data C {n : Nat} {p : n = n} (D n p) | C {zero} {p} d => c (p = p)");
  }

  @Test
  public void patternDepParamsError() {
    typeCheckClass(
        "\\data D (n : Nat) (n = n) | D zero _ => d\n" +
        "\\data C {n : Nat} {p : n = n} (D n p) | C {_} {p} d => c (p = p)", 1);
  }

  @Test
  public void patternNested() {
    typeCheckDef("\\data C (n : Nat) | C (suc (suc n)) => c2 (n = n)");
  }

  @Test
  public void patternDataLE() {
    typeCheckDef("\\data LE (n m : Nat) | LE zero m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
  }

  @Test
  public void patternImplicitError() {
    typeCheckDef("\\data D (A : Nat) | D {A} => d", 1);
  }

  @Test
  public void patternConstructorCall() {
    typeCheckClass(
        "\\data D {n : Nat} | D {zero} => d\n" +
        "\\function test => d");
  }

  @Test
  public void patternAbstract() {
    typeCheckClass(
        "\\data Wheel | wheel\n" +
        "\\data VehicleType | bikeType | carType\n" +
        "\\data Vehicle (t : VehicleType)\n" +
        "  | Vehicle (carType) => car Wheel Wheel Wheel Wheel" +
        "  | Vehicle (bikeType) => bike Wheel Wheel");
  }

  @Test
  public void patternUnknownConstructorError() {
    typeCheckDef("\\data D (n : Nat) | D (suc (luc m)) => d", 1);
  }

  @Test
  public void patternLift() {
    typeCheckClass(
        "\\data D (n : Nat) | D (zero) => d\n" +
        "\\data C (m : Nat) (n : Nat) (D m) | C (zero) (zero) (d) => c");
  }

  @Test
  public void patternLiftError() {
    typeCheckClass(
        "\\data D (n : Nat) | D (zero) => d\n" +
        "\\data C (m : Nat) (n : Nat) (D m) | C _ (zero) (d) => c", 1);
  }

  @Test
  public void patternMultipleSubst() {
    typeCheckClass(
        "\\data D (n : Nat) (m : Nat) | d (n = n) (m = m)\n" +
        "\\data C | c (n m : Nat) (D n m)\n" +
        "\\data E C | E (c (zero) (suc (zero)) (d _ _)) => e\n" +
        "\\function test => (E (c 0 1 (d (path (\\lam _ => 0)) (path (\\lam _ => 1))))).e");
  }

  @Test
  public void patternConstructorDefCall() {
    typeCheckClass(
        "\\data D (n : Nat) (m : Nat) | D (suc n) (suc m) => d (n = n) (m = m)\n" +
        "\\function test => d (path (\\lam _ => 1)) (path (\\lam _ => 0))");
  }

  @Test
  public void patternConstructorDefCallError() {
    typeCheckClass(
        "\\data D (n : Nat) | D (zero) => d\n" +
        "\\function test (n : Nat) : D n => d", 1);
  }

  @Test
  public void patternSubstTest() {
    typeCheckClass(
        "\\data E (n : Nat) | E (zero) => e\n" +
        "\\data D (n : Nat) (E n) | D (zero) (e) => d\n" +
        "\\function test => d");
  }

  @Test
  public void patternExpandArgsTest() {
    typeCheckClass(
        "\\data D (n : Nat) | d (n = n)\n" +
        "\\data C (D 1) | C (d p) => c\n" +
        "\\function test : C (d (path (\\lam _ => 1))) => c");
  }

  @Test
  public void patternNormalizeTest() {
    typeCheckClass(
        "\\data E (x : 0 = 0) | e\n" +
        "\\data C (n m : Nat) | C (suc n) (suc (suc n)) => c (n = n)\n" +
        "\\data D ((\\lam (x : \\Type0) => x) (C 1 2)) | D (c p) => x (E p)\n" +
        "\\function test => x (E (path (\\lam _ => 0))).e");
  }

  @Test
  public void patternNormalizeTest1() {
    typeCheckClass(
        "\\data E (x : 0 = 0) | e\n" +
        "\\data C (n m : Nat) | C (suc n) (suc (suc n)) => c (n = n)\n" +
        "\\data D ((\\lam (x : \\Type0) => x) (C 1 1)) | D (c p) => x (E p)", 1);
  }

  @Test
  public void patternTypeCheck() {
    typeCheckClass(
        "\\function f (x : Nat -> Nat) => x 0\n" +
        "\\data Test (A : \\Set0)\n" +
        "  | Test (suc n) => foo (f n)", 1);
  }

  @Test
  public void indexedWithConditionsError() {
    typeCheckClass(
        "\\data S | base | loop I \\with | loop right => base | loop left => base\n" +
        "\\data Q S | Q (base) => cq", 1);
  }
}
