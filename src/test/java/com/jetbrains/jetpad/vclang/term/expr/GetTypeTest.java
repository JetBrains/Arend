package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.SigmaExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class GetTypeTest extends TypeCheckingTestCase {
  private void testType(Expression expected, ChildGroup result) {
    assertEquals(expected, ((FunctionDefinition) getDefinition(result, "test")).getResultType());
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) getDefinition(result, "test")).getBody()).getExpression().getType());
  }

  @Test
  public void constructorTest() {
    ChildGroup result = typeCheckModule("\\data List (A : \\1-Type0) | nil | cons A (List A) \\func test => cons 0 nil");
    testType(DataCall((DataDefinition) getDefinition(result, "List"), Sort.SET0, Nat()), result);
  }

  @Test
  public void nilConstructorTest() {
    ChildGroup result = typeCheckModule("\\data List (A : \\1-Type0) | nil | cons A (List A) \\func test => List.nil {Nat}");
    testType(DataCall((DataDefinition) getDefinition(result, "List"), Sort.SET0, Nat()), result);
  }

  @Test
  public void classExtTest() {
    ChildGroup result = typeCheckModule("\\class Test { | A : \\Type0 | a : A } \\func test => Test { A => Nat }");
    assertEquals(Universe(new Level(1), new Level(LevelVariable.HVAR, 1)), getDefinition(result, "Test").getTypeWithParams(new ArrayList<>(), Sort.STD));
    assertEquals(Universe(Sort.SET0), getDefinition(result, "test").getTypeWithParams(new ArrayList<>(), Sort.SET0));
    testType(Universe(Sort.SET0), result);
  }

  @Test
  public void lambdaTest() {
    ChildGroup result = typeCheckModule("\\func test => \\lam (f : Nat -> Nat) => f 0");
    testType(Pi(Pi(Nat(), Nat()), Nat()), result);
  }

  @Test
  public void lambdaTest2() {
    ChildGroup result = typeCheckModule("\\func test => \\lam (A : \\Type0) (x : A) => x");
    SingleDependentLink A = singleParam("A", Universe(new Level(0), new Level(LevelVariable.HVAR)));
    Expression expectedType = Pi(A, Pi(singleParam("x", Ref(A)), Ref(A)));
    testType(expectedType, result);
  }

  @Test
  public void fieldAccTest() {
    ChildGroup result = typeCheckModule("\\class C { | x : Nat \\func f (p : 0 = x) => p } \\func test (p : Nat -> C) => C.f {p 0}");
    SingleDependentLink p = singleParam("p", Pi(Nat(), new ClassCallExpression((ClassDefinition) getDefinition(result, "C"), Sort.SET0)));
    Expression type = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Nat(),
        Zero(),
        FieldCall((ClassField) getDefinition(result, "C.x"), Sort.PROP, Apps(Ref(p), Zero())));
    List<DependentLink> testParams = new ArrayList<>();
    Expression testType = getDefinition(result, "test").getTypeWithParams(testParams, Sort.SET0);
    assertEquals(Pi(p, Pi(type, type)).normalize(NormalizeVisitor.Mode.NF), fromPiParameters(testType, testParams).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void tupleTest() {
    ChildGroup result = typeCheckModule("\\func test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    DependentLink xy = parameter(true, vars("x", "y"), Nat());
    testType(new SigmaExpression(Sort.PROP, params(xy, paramExpr(FunCall(Prelude.PATH_INFIX, Sort.SET0, Nat(), Ref(xy), Ref(xy.getNext()))))), result);
  }

  @Test
  public void letTest() {
    Definition def = typeCheckDef("\\func test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    SingleDependentLink F = singleParam("F", Pi(Nat(), Universe(new Level(0), new Level(LevelVariable.HVAR))));
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink f = singleParam("f", Pi(x, Apps(Ref(F), Ref(x))));
    assertEquals(Pi(F, Pi(f, Apps(Ref(F), Zero()))), ((LeafElimTree) ((FunctionDefinition) def).getBody()).getExpression().getType().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void patternConstructor1() {
    ChildGroup result = typeCheckModule(
        "\\data C Nat \\with | zero => c1 | suc n => c2 Nat");
    DataDefinition data = (DataDefinition) getDefinition(result, "C");
    List<DependentLink> c1Params = new ArrayList<>();
    Expression c1Type = data.getConstructor("c1").getTypeWithParams(c1Params, Sort.SET0);
    assertEquals(DataCall(data, Sort.SET0, Zero()), c1Type);
    List<DependentLink> c2Params = new ArrayList<>();
    Expression c2Type = data.getConstructor("c2").getTypeWithParams(c2Params, Sort.SET0);
    DependentLink params = data.getConstructor("c2").getDataTypeParameters();
    assertEquals(
        fromPiParameters(Pi(Nat(), DataCall(data, Sort.SET0, Suc(Ref(params)))), DependentLink.Helper.toList(params)),
        fromPiParameters(c2Type, c2Params)
    );
  }

  @Test
  public void patternConstructor2() {
    ChildGroup result = typeCheckModule(
        "\\data Vec (A : \\Set0) (n : Nat) \\elim n | zero => Nil | suc n => Cons A (Vec A n)" +
        "\\data D (n : Nat) (Vec Nat n) \\elim n | zero => dzero | suc n => done");
    DataDefinition vec = (DataDefinition) getDefinition(result, "Vec");
    DataDefinition d = (DataDefinition) getDefinition(result, "D");
    List<DependentLink> dzeroParams = new ArrayList<>();
    Expression dzeroType = d.getConstructor("dzero").getTypeWithParams(dzeroParams, Sort.SET0);
    assertEquals(
        fromPiParameters(DataCall(d, Sort.SET0, Zero(), Ref(d.getConstructor("dzero").getDataTypeParameters())), DependentLink.Helper.toList(d.getConstructor("dzero").getDataTypeParameters())),
        fromPiParameters(dzeroType, dzeroParams)
    );
    List<DependentLink> doneAllParams = new ArrayList<>();
    Expression doneType = d.getConstructor("done").getTypeWithParams(doneAllParams, Sort.SET0);
    DependentLink doneParams = d.getConstructor("done").getDataTypeParameters();
    assertEquals(
        fromPiParameters(DataCall(d, Sort.SET0, Suc(Ref(doneParams)), Ref(doneParams.getNext())), DependentLink.Helper.toList(d.getConstructor("done").getDataTypeParameters())),
        fromPiParameters(doneType, doneAllParams)
    );
    List<DependentLink> consAllParams = new ArrayList<>();
    Expression consType = vec.getConstructor("Cons").getTypeWithParams(consAllParams, Sort.SET0);
    DependentLink consParams = vec.getConstructor("Cons").getDataTypeParameters();
    assertEquals(
        fromPiParameters(Pi(Ref(consParams), Pi(DataCall(vec, Sort.SET0, Ref(consParams), Ref(consParams.getNext())), DataCall(vec, Sort.SET0, Ref(consParams), Suc(Ref(consParams.getNext()))))), DependentLink.Helper.toList(consParams)),
        fromPiParameters(consType, consAllParams)
    );
  }

  @Test
  public void patternConstructor3() {
    ChildGroup result = typeCheckModule(
        "\\data D | d \\Type0\n" +
        "\\data C D \\with | d A => c A");
    DataDefinition d = (DataDefinition) getDefinition(result, "D");
    DataDefinition c = (DataDefinition) getDefinition(result, "C");
    DependentLink A = c.getConstructor("c").getDataTypeParameters();
    List<DependentLink> cParams = new ArrayList<>();
    Expression cType = c.getConstructor("c").getTypeWithParams(cParams, Sort.SET0);
    assertEquals(
        fromPiParameters(Pi(Ref(A), DataCall(c, Sort.SET0, ConCall(d.getConstructor("d"), Sort.SET0, Collections.emptyList(), Ref(A)))), DependentLink.Helper.toList(c.getConstructor("c").getDataTypeParameters())),
        fromPiParameters(cType, cParams)
    );
  }

  @Test
  public void patternConstructorDep() {
    ChildGroup result = typeCheckModule(
        "\\data Box (n : Nat) | box\n" +
        "\\data D (n : Nat) (Box n) \\elim n | zero => d");
    DataDefinition d = (DataDefinition) getDefinition(result, "D");
    List<DependentLink> dParams = new ArrayList<>();
    Expression dType = d.getConstructor("d").getTypeWithParams(dParams, Sort.SET0);
    assertEquals(
        fromPiParameters(DataCall(d, Sort.SET0, Zero(), Ref(d.getConstructor("d").getDataTypeParameters())), DependentLink.Helper.toList(d.getConstructor("d").getDataTypeParameters())),
        fromPiParameters(dType, dParams)
    );
  }
}
