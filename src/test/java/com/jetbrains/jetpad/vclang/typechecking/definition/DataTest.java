package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DataTest extends TypeCheckingTestCase {
  @Test
  public void dataType() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D {A B : \\Set0} (I : A -> B -> \\Set0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)");
    List<DependentLink> params = new ArrayList<>();
    TypeMax type = typedDef.getTypeWithParams(params, LevelArguments.ZERO);

    LinkList parameters = new LinkList();
    parameters.append(param(false, vars("A", "B"), Universe(0, 0)));
    DependentLink A = parameters.getFirst();
    DependentLink B = A.getNext();
    parameters.append(param("I", Pi(Reference(A), Pi(Reference(B), Universe(0, 0)))));
    DependentLink I = B.getNext();
    parameters.append(param("a", Reference(A)));
    parameters.append(param("b", Reference(B)));
    DependentLink a = I.getNext();
    DependentLink b = a.getNext();

    LinkList parameters1 = new LinkList();
    parameters1.append(param("x", Reference(A)));
    parameters1.append(param(Apps(Reference(I), Reference(parameters1.getFirst()), Reference(b))));

    LinkList parameters2 = new LinkList();
    parameters2.append(param(false, "y", Reference(B)));
    parameters2.append(param(Apps(Reference(I), Reference(a), Reference(parameters2.getFirst()))));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(parameters.getFirst(), Universe(0, 0)), type.fromPiParameters(params));
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
    Type con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, LevelArguments.ZERO);
    assertEquals(Pi(parameters.getFirst(), Pi(parameters1.getFirst(), DataCall(typedDef, LevelArguments.ZERO,
      Reference(A),
      Reference(B),
      Reference(I),
      Reference(a),
      Reference(b)))), con1Type.fromPiParameters(con1Params));
    List<DependentLink> con2Params = new ArrayList<>();
    Type con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, LevelArguments.ZERO);
    assertEquals(Pi(parameters.getFirst(), Pi(parameters2.getFirst(), DataCall(typedDef, LevelArguments.ZERO,
      Reference(A),
      Reference(B),
      Reference(I),
      Reference(a),
      Reference(b)))), con2Type.fromPiParameters(con2Params));
  }

  @Test
  public void dataType2() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D (A : \\7-Type2) | con1 (X : \\1-Type5) X | con2 (Y : \\2-Type3) A Y");
    DependentLink A = typedDef.getParameters();
    List<DependentLink> params = new ArrayList<>();
    TypeMax type = typedDef.getTypeWithParams(params, LevelArguments.ZERO);
    List<DependentLink> con1Params = new ArrayList<>();
    Type con1Type = typedDef.getConstructors().get(0).getTypeWithParams(con1Params, LevelArguments.ZERO);
    List<DependentLink> con2Params = new ArrayList<>();
    Type con2Type = typedDef.getConstructors().get(1).getTypeWithParams(con2Params, LevelArguments.ZERO);

    LinkList parameters1 = new LinkList();
    parameters1.append(param("X", Universe(5, 1)));
    parameters1.append(param(Reference(parameters1.getFirst())));

    LinkList parameters2 = new LinkList();
    parameters2.append(param("Y", Universe(3, 2)));
    parameters2.append(param(Reference(A)));
    parameters2.append(param(Reference(parameters2.getFirst())));

    assertNotNull(typedDef);
    assertEquals(Definition.TypeCheckingStatus.NO_ERRORS, typedDef.status());
    assertEquals(Pi(A, Universe(6, 7)), type.fromPiParameters(params));
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(A, Pi(parameters1.getFirst(), DataCall(typedDef, LevelArguments.ZERO, Reference(A)))), con1Type.fromPiParameters(con1Params));
    assertEquals(Pi(A, Pi(parameters2.getFirst(), DataCall(typedDef, LevelArguments.ZERO, Reference(A)))), con2Type.fromPiParameters(con2Params));
  }

  @Test
  public void constructor() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cDefCall(null, con.getAbstractDefinition()), cNat(), cZero(), cZero());

    CheckTypeVisitor.Result result = typeCheckExpr(expr, null);
    assertThat(result.type, is((TypeMax) DataCall(def, LevelArguments.ZERO, Nat())));
  }

  @Test
  public void constructorInfer() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con (B : \\1-Type1) A B");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cVar("f"), cApps(cDefCall(null, con.getAbstractDefinition()), cNat(), cLam("x", cVar("x")), cZero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(DataCall(def, LevelArguments.ZERO, Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.Result result = typeCheckExpr(localContext, expr, null);
    assertThat(result.type, is((TypeMax) Nat()));
  }

  @Test
  public void constructorConst() {
    DataDefinition def = (DataDefinition) typeCheckDef("\\data D (A : \\1-Type0) | con A");

    Constructor con = def.getConstructor("con");
    Concrete.Expression expr = cApps(cVar("f"), cDefCall(null, con.getAbstractDefinition()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), DataCall(def, LevelArguments.ZERO, Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.Result result = typeCheckExpr(localContext, expr, null);
    assertThat(result.type, is((TypeMax) Pi(Nat(), Nat())));
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
