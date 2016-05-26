package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.*;

public class DefinitionTest {
  ListErrorReporter errorReporter;

  @Before
  public void initialize() {
    Root.initialize();
    errorReporter = new ListErrorReporter();
  }

  @Test
  public void function() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f : Nat => 0");
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
  }

  @Test
  public void functionUntyped() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f => 0");
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Nat(), typedDef.getType());
  }

  @Test
  public void functionWithArgs() {
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDef("\\function f (x : Nat) (y : Nat -> Nat) => y");
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), typedDef.getType());
  }

  @Test
  public void dataType() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D {A B : \\Type0} (I : A -> B -> \\Type0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)");

    LinkList parameters = new LinkList();
    parameters.append(param(false, vars("A", "B"), Universe(0)));
    DependentLink A = parameters.getFirst();
    DependentLink B = A.getNext();
    parameters.append(param("I", Pi(Reference(A), Pi(Reference(B), Universe(0)))));
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
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters.getFirst(), Universe(0)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    Substitution substitution = new Substitution();
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
    assertEquals(Pi(parameters.getFirst(), Pi(parameters1.getFirst(), DataCall(typedDef)
        .addArgument(Reference(A), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Reference(B), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Reference(I), AppExpression.DEFAULT)
        .addArgument(Reference(a), AppExpression.DEFAULT)
        .addArgument(Reference(b), AppExpression.DEFAULT))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(parameters.getFirst(), Pi(parameters2.getFirst(), DataCall(typedDef)
        .addArgument(Reference(A), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Reference(B), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Reference(I), AppExpression.DEFAULT)
        .addArgument(Reference(a), AppExpression.DEFAULT)
        .addArgument(Reference(b), AppExpression.DEFAULT))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void dataType2() {
    DataDefinition typedDef = (DataDefinition) typeCheckDef("\\data D (A : \\7-Type2) | con1 (X : \\1-Type5) X | con2 (Y : \\2-Type3) A Y");
    DependentLink A = typedDef.getParameters();

    LinkList parameters1 = new LinkList();
    parameters1.append(param("X", Universe(5, 1)));
    parameters1.append(param(Reference(parameters1.getFirst())));

    LinkList parameters2 = new LinkList();
    parameters2.append(param("Y", Universe(3, 2)));
    parameters2.append(param(Reference(A)));
    parameters2.append(param(Reference(parameters2.getFirst())));

    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(A, Universe(6, 7)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(A, Pi(parameters1.getFirst(), Apps(DataCall(typedDef), Reference(A)))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(A, Pi(parameters2.getFirst(), Apps(DataCall(typedDef), Reference(A)))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    DependentLink A = param("A", Universe(0));
    DependentLink B = param("B", Universe(1));

    ModuleID moduleID = new NameModuleID("test");
    Namespace namespace = new Namespace(moduleID);
    DataDefinition def = new DataDefinition(namespace.getChild("D").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(1, TypeUniverse.NOT_TRUNCATED), A);
    namespace.addDefinition(def);
    Constructor con = new Constructor(namespace.getChild("D").getChild("con").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(1, TypeUniverse.NOT_TRUNCATED), params(B, param(Reference(A)), param(Reference(B))), def);
    def.addConstructor(con);

    Concrete.Expression expr = cApps(cDefCall(null, con), cNat(), cZero(), cZero());
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, null);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Apps(DataCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    DependentLink A = param("A", Universe(0));
    DependentLink B = param("B", Universe(1));

    ModuleID moduleID = new NameModuleID("test");
    Namespace namespace = new Namespace(moduleID);
    DataDefinition def = new DataDefinition(namespace.getChild("D").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(1, TypeUniverse.NOT_TRUNCATED), A);
    namespace.addDefinition(def);
    Constructor con = new Constructor(namespace.getChild("D").getChild("con").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(1, TypeUniverse.NOT_TRUNCATED), params(B, param(Reference(A)), param(Reference(B))), def);
    def.addConstructor(con);

    Concrete.Expression expr = cApps(cVar("f"), cApps(cDefCall(null, con), cNat(), cLam("x", cVar("x")), cZero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DataCall(def), Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(localContext, errorReporter).build(), null);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    DependentLink A = param("A", Universe(0));
    ModuleID moduleID = new NameModuleID("test");
    Namespace namespace = new Namespace(moduleID);
    DataDefinition def = new DataDefinition(namespace.getChild("D").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, TypeUniverse.SetOfLevel(0), A);
    namespace.addDefinition(def);
    Constructor con = new Constructor(namespace.getChild("D").getChild("con").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, TypeUniverse.SetOfLevel(0), param(Reference(A)), def);
    def.addConstructor(con);

    Concrete.Expression expr = cApps(cVar("f"), cDefCall(null, con));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DataCall(def), Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(localContext, errorReporter).build(), null);
    assertEquals(errorReporter.getErrorList().toString(), 0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void patternVector() {
    typeCheckDef("\\data Vec \\Type0 Nat | Vec _ zero => Nil | Vec A (suc m) => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    typeCheckClass(
        "\\static \\data D (n : Nat) (n = n) | D zero _ => d\n" +
        "\\static \\data C {n : Nat} {p : n = n} (D n p) | C {zero} {p} d => c (p = p)");
  }

  @Test
  public void patternDepParamsError() {
    typeCheckClass(
        "\\static \\data D (n : Nat) (n = n) | D zero _ => d\n" +
        "\\static \\data C {n : Nat} {p : n = n} (D n p) | C {_} {p} d => c (p = p)", 1);
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
        "\\static \\data D {n : Nat} | D {zero} => d\n" +
        "\\static \\function test => d");
  }

  @Test
  public void patternAbstract() {
    typeCheckClass(
        "\\static \\data Wheel | wheel\n" +
        "\\static \\data VehicleType | bikeType | carType\n" +
        "\\static \\data Vehicle (t : VehicleType)\n" +
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
        "\\static \\data D (n : Nat) | D (zero) => d\n" +
        "\\static \\data C (m : Nat) (n : Nat) (D m) | C (zero) (zero) (d) => c");
  }

  @Test
  public void patternLiftError() {
    typeCheckClass(
        "\\static \\data D (n : Nat) | D (zero) => d\n" +
        "\\static \\data C (m : Nat) (n : Nat) (D m) | C _ (zero) (d) => c", 1);
  }

  @Test
  public void patternMultipleSubst() {
    typeCheckClass(
        "\\static \\data D (n : Nat) (m : Nat) | d (n = n) (m = m)\n" +
        "\\static \\data C | c (n m : Nat) (D n m)\n" +
        "\\static \\data E C | E (c (zero) (suc (zero)) (d _ _)) => e\n" +
        "\\static \\function test => (E (c 0 1 (d (path (\\lam _ => 0)) (path (\\lam _ => 1))))).e");
  }

  @Test
  public void patternConstructorDefCall() {
    typeCheckClass(
        "\\static \\data D (n : Nat) (m : Nat) | D (suc n) (suc m) => d (n = n) (m = m)\n" +
        "\\static \\function test => d (path (\\lam _ => 1)) (path (\\lam _ => 0))");
  }

  @Test
  public void patternConstructorDefCallError() {
    typeCheckClass(
        "\\static \\data D (n : Nat) | D (zero) => d\n" +
        "\\static \\function test (n : Nat) : D n => d", 1);
  }

  @Test
  public void patternSubstTest() {
    typeCheckClass(
        "\\static \\data E (n : Nat) | E (zero) => e\n" +
        "\\static \\data D (n : Nat) (E n) | D (zero) (e) => d\n" +
        "\\static \\function test => d");
  }

  @Test
  public void patternExpandArgsTest() {
    typeCheckClass(
        "\\static \\data D (n : Nat) | d (n = n)\n" +
        "\\static \\data C (D 1) | C (d p) => c\n" +
        "\\static \\function test : C (d (path (\\lam _ => 1))) => c");
  }

  @Test
  public void patternNormalizeTest() {
    typeCheckClass(
        "\\static \\data E (x : 0 = 0) | e\n" +
        "\\static \\data C (n : Nat) | C (suc n) => c (n = n)\n" +
        "\\static \\data D ((\\lam (x : \\Type0) => x) (C 1)) | D (c p) => x (E p)\n" +
        "\\static \\function test => x (E (path (\\lam _ => 0))).e");
  }

  @Test
  public void patternTypeCheck() {
    typeCheckClass(
        "\\static \\function f (x : Nat -> Nat) => x 0\n" +
        "\\static \\data Test (A : \\Set0)\n" +
        "  | Test (suc n) => foo (f n)", 1);
  }


  @Test
  public void constructorTest() {
    typeCheckClass(
        "\\static \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f 0 = n)\n" +
        "\\static \\function f (x : Nat) : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
        "\\static \\function g : D 0 (\\lam y => y) => con2 (path (\\lam _ => 0))");
  }

  @Test
  public void indexedWithConditionsError() {
    typeCheckClass(
        "\\static \\data S | base | loop I \\with | loop right => base | loop left => base\n" +
        "\\static \\data Q S | Q (base) => cq", 1);
  }
}
