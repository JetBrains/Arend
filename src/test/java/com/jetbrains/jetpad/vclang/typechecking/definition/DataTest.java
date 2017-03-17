package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class DataTest extends TypeCheckingTestCase {
  @Test
  public void dataType() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D {A B : \\Set0} (I : A -> B -> \\Set0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)");
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, LevelArguments.ZERO);

    SingleDependentLink A = singleParam(false, vars("A", "B"), Universe(0, 0));
    SingleDependentLink B = A.getNext();
    SingleDependentLink I = singleParam("I", Pi(Reference(A), Pi(Reference(B), Universe(0, 0))));
    SingleDependentLink a = singleParam("a", Reference(A));
    SingleDependentLink b = singleParam("b", Reference(B));
    SingleDependentLink x = singleParam("x", Reference(A));
    SingleDependentLink y = singleParam(false, vars("y"), Reference(B));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(A, Pi(B, Pi(I, Pi(a, Pi(b, Universe(0, 0)))))), type.fromPiParameters(params));
    assertEquals(2, typedDef.getConstructors().size());

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink link = typedDef.getParameters();
    substitution.add(link, Reference(A));
    link = link.getNext();
    substitution.add(link, Reference(B));
    link = link.getNext();
    substitution.add(link, Reference(I));
    link = link.getNext();
    substitution.add(link, Reference(a));
    link = link.getNext();
    substitution.add(link, Reference(b));
    List<DependentLink> con1Params = new ArrayList<>();
    Expression con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, LevelArguments.ZERO);
    assertEquals(Pi(A, Pi(B, Pi(I, Pi(a, Pi(b, Pi(x, Pi(Apps(Reference(I), Reference(x), Reference(b)), DataCall(typedDef, LevelArguments.ZERO,
      Reference(A),
      Reference(B),
      Reference(I),
      Reference(a),
      Reference(b))))))))), con1Type.fromPiParameters(con1Params));
    List<DependentLink> con2Params = new ArrayList<>();
    Expression con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, LevelArguments.ZERO);
    assertEquals(Pi(A, Pi(B, Pi(I, Pi(a, Pi(b, Pi(y, Pi(Apps(Reference(I), Reference(a), Reference(y)), DataCall(typedDef, LevelArguments.ZERO,
      Reference(A),
      Reference(B),
      Reference(I),
      Reference(a),
      Reference(b))))))))), con2Type.fromPiParameters(con2Params));
  }

  @Test
  public void dataType2() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D (A : \\7-Type2) | con1 (X : \\1-Type5) X | con2 (Y : \\2-Type3) A Y");
    SingleDependentLink A = singleParam("A", Universe(2, 7));
    List<DependentLink> params = new ArrayList<>();
    Expression type = typedDef.getTypeWithParams(params, LevelArguments.ZERO);
    List<DependentLink> con1Params = new ArrayList<>();
    Expression con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, LevelArguments.ZERO);
    List<DependentLink> con2Params = new ArrayList<>();
    Expression con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, LevelArguments.ZERO);

    SingleDependentLink X = singleParam("X", Universe(5, 1));
    SingleDependentLink Y = singleParam("Y", Universe(3, 2));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(A, Universe(6, 7)), type.fromPiParameters(params));
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(A, Pi(X, Pi(Reference(X), DataCall(typedDef, LevelArguments.ZERO, Reference(A))))), con1Type.fromPiParameters(con1Params));
    assertEquals(Pi(A, Pi(Y, Pi(Reference(A), Pi(Reference(Y), DataCall(typedDef, LevelArguments.ZERO, Reference(A)))))), con2Type.fromPiParameters(con2Params));
  }

  @Test
  public void constructor() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cDefCall(null, con.getAbstractDefinition()), cNat(), cZero(), cZero());

    CheckTypeVisitor.Result result = typeCheckExpr(expr, null);
    assertEquals(result.type, DataCall(def, LevelArguments.ZERO, Nat()));
  }

  @Test
  public void constructorInfer() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cVar("f"), cApps(cDefCall(null, con.getAbstractDefinition()), cNat(), cLam("x", cVar("x")), cZero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(DataCall(def, LevelArguments.ZERO, Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.Result result = typeCheckExpr(localContext, expr, null);
    assertEquals(result.type, Nat());
  }

  @Test
  public void constructorConst() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con A");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cVar("f"), cDefCall(null, con.getAbstractDefinition()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), DataCall(def, LevelArguments.ZERO, Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.Result result = typeCheckExpr(localContext, expr, null);
    assertEquals(result.type, Pi(Nat(), Nat()));
  }

  @Test
  public void constructorTest() {
    typeCheckClass(
      "\\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f 0 = n)\n" +
      "\\function f (x : Nat) : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
      "\\function g : D 0 (\\lam y => y) => con2 (path (\\lam _ => 0))");
  }

  @Test
  public void truncatedDataElimOk() {
    typeCheckClass(
      "\\truncated \\data S : \\Set | base | loop I \\with loop left => base | loop right => base\n"+
      "\\function f (x : S) : Nat <= \\elim x | base => 0 | loop _ => 0");
  }

  @Test
  public void truncatedDataElimError() {
    typeCheckClass(
      "\\truncated \\data S : \\Prop | base | loop I \\with loop left => base | loop right => base\n"+
      "\\function f (x : S) : Nat <= \\elim x | base => 0 | loop _ => 0", 1);
  }
}
