package org.arend.typechecking.definition;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.*;

public class DataTest extends TypeCheckingTestCase {
  @Test
  public void dataType() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D {A B : \\Set0} (I : A -> B -> \\Set0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)");
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, Sort.SET0);

    SingleDependentLink A = singleParams(false, vars("A", "B"), Universe(0, 0));
    SingleDependentLink B = A.getNext();
    SingleDependentLink I = singleParam("I", Pi(Ref(A), Pi(Ref(B), Universe(0, 0))));
    SingleDependentLink a = singleParam("a", Ref(A));
    SingleDependentLink b = singleParam("b", Ref(B));
    SingleDependentLink x = singleParam("x", Ref(A));
    SingleDependentLink y = singleParam(false, "y", Ref(B));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(A, Pi(I, Pi(a, Pi(b, Universe(0, 0))))), fromPiParameters(type, params));
    assertEquals(2, typedDef.getConstructors().size());

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink link = typedDef.getParameters();
    substitution.add(link, Ref(A));
    link = link.getNext();
    substitution.add(link, Ref(B));
    link = link.getNext();
    substitution.add(link, Ref(I));
    link = link.getNext();
    substitution.add(link, Ref(a));
    link = link.getNext();
    substitution.add(link, Ref(b));
    List<DependentLink> con1Params = new ArrayList<>();
    Expression con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, Sort.SET0);
    assertEquals(Pi(A, Pi(I, Pi(a, Pi(b, Pi(x, Pi(Apps(Ref(I), Ref(x), Ref(b)), DataCall(typedDef, Sort.SET0,
      Ref(A),
      Ref(B),
      Ref(I),
      Ref(a),
      Ref(b)))))))), fromPiParameters(con1Type, con1Params));
    List<DependentLink> con2Params = new ArrayList<>();
    Expression con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, Sort.SET0);
    assertEquals(Pi(A, Pi(I, Pi(a, Pi(b, Pi(y, Pi(Apps(Ref(I), Ref(a), Ref(y)), DataCall(typedDef, Sort.SET0,
      Ref(A),
      Ref(B),
      Ref(I),
      Ref(a),
      Ref(b)))))))), fromPiParameters(con2Type, con2Params));
  }

  @Test
  public void dataType2() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D (A : \\7-Type2) | con1 (X : \\1-Type5) X | con2 (Y : \\2-Type3) A Y");
    SingleDependentLink A = singleParam("A", Universe(2, 7));
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, Sort.SET0);
    List<DependentLink> con1Params = new ArrayList<>();
    Expression con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, Sort.SET0);
    List<DependentLink> con2Params = new ArrayList<>();
    Expression con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, Sort.SET0);

    SingleDependentLink X = singleParam("X", Universe(5, 1));
    SingleDependentLink Y = singleParam("Y", Universe(3, 2));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(A, Universe(6, 7)), fromPiParameters(type, params));
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(A, Pi(X, Pi(Ref(X), DataCall(typedDef, Sort.SET0, Ref(A))))), fromPiParameters(con1Type, con1Params));
    assertEquals(Pi(A, Pi(Y, Pi(Ref(A), Pi(Ref(Y), DataCall(typedDef, Sort.SET0, Ref(A)))))), fromPiParameters(con2Type, con2Params));
  }

  @Test
  public void constructor() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cVar(con.getReferable()), cNat(), cZero(), cZero());

    TypecheckingResult result = typeCheckExpr(expr, null);
    assertEquals(result.type, DataCall(def, Sort.STD, Nat()));
  }

  @Test
  public void constructorInfer() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    LocalReferable f = ref("f");
    LocalReferable x = ref("x");
    Concrete.Expression expr = cApps(cVar(f), cApps(cVar(con.getReferable()), cNat(), cLam(cName(x), cVar(x)), cZero()));
    Map<Referable, Binding> localContext = new HashMap<>();
    localContext.put(f, new TypedBinding(f.textRepresentation(), Pi(DataCall(def, Sort.SET0, Pi(Nat(), Nat())), Nat())));

    TypecheckingResult result = typeCheckExpr(localContext, expr, null);
    assertEquals(result.type, Nat());
  }

  @Test
  public void constructorConst() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con A");

    Constructor con = def.getConstructor("con");
    LocalReferable f = ref("f");
    Concrete.Expression expr = cApps(cVar(f), cVar(con.getReferable()));
    Map<Referable, Binding> localContext = new HashMap<>();
    localContext.put(f, new TypedBinding(f.textRepresentation(), Pi(Pi(Nat(), DataCall(def, Sort.SET0, Nat())), Pi(Nat(), Nat()))));

    TypecheckingResult result = typeCheckExpr(localContext, expr, null);
    assertEquals(result.type, Pi(Nat(), Nat()));
  }

  @Test
  public void constructorTest() {
    typeCheckModule(
      "\\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f 0 = n)\n" +
      "\\func f (x : Nat) : D x (\\lam y => y) => con1 idp\n" +
      "\\func g : D 0 (\\lam y => y) => con2 idp");
  }

  @Test
  public void truncatedDataElimOk() {
    typeCheckModule(
      "\\truncated \\data S : \\Set | base | loop I { | left => base | right => base }\n"+
      "\\func f (x : S) : Nat | base => 0 | loop _ => 0");
  }

  @Test
  public void truncatedDataElimError() {
    typeCheckModule(
      "\\truncated \\data S : \\Prop | base | loop I { | left => base | right => base }\n"+
      "\\sfunc f (x : S) : Nat | base => 0 | loop _ => 0", 1);
  }

  @Test
  public void constructorsOrderTest() {
    typeCheckModule(
      "\\data D\n" +
      "  | con1 I \\with {\n" +
      "    | left => con2" +
      "  }\n" +
      "  | con2", 1);
  }

  @Test
  public void inductionRecursion() {
    typeCheckModule(
      "\\func f (d : D) : \\Set | d1 => Nat | d2 x y => \\Pi (a : f x) -> f (y a)\n" +
      "\\data D : \\Set | d1 | d2 (x : D) (f x -> D)");
  }

  @Test
  public void propWithConstructors() {
    typeCheckModule(
      "\\data D (n : Nat) \\with\n" +
      "  | 0 => con1\n" +
      "  | suc _ => con2\n" +
      "\\func f (n : Nat) : \\Prop => D n");
  }

  @Test
  public void propWithConstructorsError() {
    typeCheckModule(
      "\\data D (n : Nat) \\with\n" +
      "  | 0 => con1\n" +
      "  | _ => con2\n" +
      "\\func f (n : Nat) : \\Prop => D n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propWithConstructorsError2() {
    typeCheckModule(
      "\\data D (n : Nat) \\with\n" +
      "  | 0 => con1\n" +
      "  | 0 => con2\n" +
      "\\func f (n : Nat) : \\Prop => D n", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propTruncatedData() {
    typeCheckModule(
      "\\truncated \\data D (A B : \\Prop) : \\Prop\n" +
      "  | inl A\n" +
      "  | inr B");
  }

  @Test
  public void propSum() {
    typeCheckModule(
      "\\data D (A B : \\Prop) : \\Prop\n" +
      "  | inl A\n" +
      "  | inr B", 1);
  }

  @Test
  public void covariantTest() {
    typeCheckModule(
      "\\data D (A B : \\Type)\n" +
      "  | con1 A\n" +
      "  | con2 B");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertTrue(d.isCovariant(0));
    assertTrue(d.isCovariant(1));
  }

  @Test
  public void covariantSigmaTest() {
    typeCheckModule(
      "\\data D (A B : \\Type)\n" +
        "  | con1 (\\Sigma A B)\n" +
        "  | con2 B");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertTrue(d.isCovariant(0));
    assertTrue(d.isCovariant(1));
  }

  @Test
  public void recursiveCovariantTest() {
    typeCheckModule(
      "\\data D (A : \\Type)\n" +
      "  | con1\n" +
      "  | con2 A (D A)");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertTrue(d.isCovariant(0));
  }

  @Ignore
  @Test
  public void recursiveCovariantTest2() {
    typeCheckModule(
      "\\data D (A B : \\Type)\n" +
      "  | con1\n" +
      "  | con2 A (D B A)");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertTrue(d.isCovariant(0));
    assertTrue(d.isCovariant(1));
  }

  @Test
  public void nonCovariantTest() {
    typeCheckModule(
      "\\data D (A B : \\Type)\n" +
      "  | con1 (A -> B)\n" +
      "  | con2 B");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertFalse(d.isCovariant(0));
    assertTrue(d.isCovariant(1));
  }

  @Test
  public void recursiveNonCovariantTest() {
    typeCheckModule(
      "\\data D (A : \\Type)\n" +
      "  | con1\n" +
      "  | con2 (D (A -> Nat))");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertFalse(d.isCovariant(0));
  }

  @Test
  public void recursiveNonCovariantTest2() {
    typeCheckModule(
      "\\data D (A B : \\Type)\n" +
      "  | con1\n" +
      "  | con2 A (D B (A -> Nat))");
    DataDefinition d = (DataDefinition) getDefinition("D");
    assertFalse(d.isCovariant(0));
    assertFalse(d.isCovariant(1));
  }

  @Test
  public void constructorAndTypeClass() {
    typeCheckModule(
      "\\class C | x : Nat\n" +
      "\\data D (c : C) | con Nat\n" +
      "\\func f (c : C) => con 0");
  }

  @Test
  public void constructorWithPatternsAndTypeClass() {
    typeCheckModule(
      "\\class C | x : Nat\n" +
      "\\data D (c : C) (n : Nat) \\elim n | zero => con Nat\n" +
      "\\func f (c : C) => con 0");
  }

  @Test
  public void constructorWithPatternsAndTypeClass2() {
    typeCheckModule(
      "\\class C | x : Nat\n" +
      "\\data D (n : Nat) (c : C) \\elim n | zero => con Nat\n" +
      "\\func f (c : C) => con 0");
  }
}
