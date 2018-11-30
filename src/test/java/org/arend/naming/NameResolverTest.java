package org.arend.naming;

import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.module.ModulePath;
import org.arend.naming.reference.LocatedReferableImpl;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.EmptyScope;
import org.arend.naming.scope.ListScope;
import org.arend.naming.scope.SingletonScope;
import org.arend.prelude.Prelude;
import org.arend.term.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.arend.frontend.ConcreteExpressionFactory.*;
import static org.arend.typechecking.Matchers.*;
import static org.junit.Assert.*;

public class NameResolverTest extends NameResolverTestCase {
  @Test
  public void notInScopeError() {
    resolveNamesExpr("x", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void notInScopeLongNameError() {
    resolveNamesExpr("x.y", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void notInScopeLongNameError2() {
    resolveNamesExpr("x.y.z", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void notInScopeLongNameError3() {
    resolveNamesModule(
      "\\class X\n" +
      "\\func f => X.y", 1);
    assertThatErrorsAre(notInScope("y"));
  }

  @Test
  public void notInScopeLongNameError4() {
    resolveNamesModule(
      "\\class X\n" +
      "\\func f => X.y.z", 1);
    assertThatErrorsAre(notInScope("y"));
  }

  @Test
  public void notInScopeLongNameError5() {
    resolveNamesModule(
      "\\class X \\where { \\func y }\n" +
      "\\func f => X.y.z", 1);
    assertThatErrorsAre(notInScope("z"));
  }

  @Test
  public void notInScopeLongNameError6() {
    resolveNamesModule(
      "\\class X \\where { \\func y }\n" +
      "\\func f => X.y.z.w", 1);
    assertThatErrorsAre(notInScope("z"));
  }

  @Test
  public void parserInfix() {
    ConcreteLocatedReferable plusRef = new ConcreteLocatedReferable(null, "+", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 6, true), MODULE_PATH);
    Concrete.Definition plus = new Concrete.FunctionDefinition(Concrete.FunctionDefinition.Kind.FUNC, plusRef, Collections.emptyList(), null, null);
    plusRef.setDefinition(plus);
    ConcreteLocatedReferable mulRef = new ConcreteLocatedReferable(null, "*", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 7, true), MODULE_PATH);
    Concrete.Definition mul = new Concrete.FunctionDefinition(Concrete.FunctionDefinition.Kind.FUNC, mulRef, Collections.emptyList(), null, null);
    mulRef.setDefinition(mul);

    Concrete.Expression result = resolveNamesExpr(new ListScope(plusRef, mulRef), "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)");
    assertNotNull(result);
    assertTrue(compareAbstract(result, cApps(cVar(plusRef), cApps(cVar(plusRef), cNum(0), cApps(cVar(mulRef), cNum(1), cNum(2))), cApps(cVar(mulRef), cApps(cVar(mulRef), cNum(3), cApps(cVar(mulRef), cNum(4), cNum(5))), cApps(cVar(plusRef), cNum(6), cNum(7))))));
  }

  @Test
  public void parserInfixError() {
    ConcreteLocatedReferable plusRef = new ConcreteLocatedReferable(null, "+", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 6, true), MODULE_PATH);
    Concrete.Definition plus = new Concrete.FunctionDefinition(Concrete.FunctionDefinition.Kind.FUNC, plusRef, Collections.emptyList(), null, null);
    plusRef.setDefinition(plus);
    ConcreteLocatedReferable mulRef = new ConcreteLocatedReferable(null, "*", new Precedence(Precedence.Associativity.RIGHT_ASSOC, (byte) 6, true), MODULE_PATH);
    Concrete.Definition mul = new Concrete.FunctionDefinition(Concrete.FunctionDefinition.Kind.FUNC, mulRef, Collections.emptyList(), null, null);
    mulRef.setDefinition(mul);
    resolveNamesExpr(new ListScope(plusRef, mulRef), "11 + 2 * 3", 1);
  }

  @Test
  public void whereTest() {
    resolveNamesModule(
        "\\func f (x : \\Type0) => B.b (a x) \\where {\n" +
        "  \\func a (x : \\Type0) => x\n" +
        "  \\data D | D1 | D2\n" +
        "  \\class B \\where {\n" +
        "    \\data C | cr\n" +
        "    \\func b (x : \\Type0) => D1\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void whereTestDefCmd() {
    resolveNamesModule(
        "\\func f (x : \\Type0) => a \\where {\n" +
        "  \\class A \\where { \\func a => 0 }\n" +
        "  \\open A\n" +
        "}");
  }

  @Test
  public void whereOpenFunction() {
    resolveNamesModule(
        "\\func f => x \\where {\n" +
        "  \\func b => 0 \\where\n" +
        "    \\func x => 0\n" +
        "  \\open b(x)\n" +
        "}");
  }

  @Test
  public void whereNested() {
    resolveNamesModule(
        "\\func f => x \\where {\n" +
        "  \\data B | b\n" +
        "  \\func x => a \\where\n" +
        "    \\func a => b\n" +
        "}");
  }

  @Test
  public void whereOuterScope() {
    resolveNamesModule(
        "\\func f => 0 \\where {\n" +
        "  \\func g => 0\n" +
        "  \\func h => g\n" +
        "}");
  }

  @Test
  public void whereInSignature() {
    resolveNamesModule(
        "\\func f : D => d \\where\n" +
        "  \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    resolveNamesModule(
        "\\func f => 0 \\where\n" +
        "  \\func x => 0\n" +
        "\\func g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    resolveNamesModule(
        "\\func f => 0 \\where {\n" +
        "  \\func x => 0\n" +
        "  \\func y => x\n" +
        "}\n" +
        "\\func g => 0 \\where\n" +
        "  \\open f(y)");
  }

  @Test
  public void openTest() {
    resolveNamesModule("\\class A \\where { \\func x => 0 } \\open A \\func y => x");
  }

  @Test
  public void openTest2() {
    resolveNamesModule("\\class A \\where { \\func x => 0 } \\func y => x \\open A");
  }

  @Ignore
  @Test
  public void exportTest() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\export B } \\func y => A.x");
  }

  @Ignore("dynamic call")
  @Test
  public void staticFieldAccCallTest() {
    resolveNamesModule("\\class A { | x : \\Type0 \\class B \\where { \\func y => x } } \\func f (a : A) => a.B.y");
  }

  @Ignore("export")
  @Test
  public void exportPublicFieldsTest() {
    /*
    resolveNamesClass("test", "\\class A \\where { \\func x => 0 \\class B \\where { \\func y => x } \\export B } \\func f => A.y");
    assertNotNull(Root.getModule(new NameModuleSourceId("test")));
    Root.getModule(new NameModuleSourceId("test")).namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(staticNamespace.getMember("A"));
    assertEquals(3, staticNamespace.getMember("A").namespace.getMembers().size());
    assertEquals(3, ((Concrete.ClassDefinition) staticNamespace.getMember("A").abstractDefinition).getStatements().size());
    */
    assertTrue(false);
  }

  @Ignore
  @Test
  public void exportTest2() {
    /*
    resolveNamesDef(
        "\\class Test {\n" +
        "  | (+) (x y : \\Type0) : \\Type0\n" +
        "  \\class A {\n" +
        "    | x : \\Type0\n" +
        "    \\class B {\n" +
        "      | y : \\Type0\n" +
        "      \\class C \\where {\n" +
        "        \\func z => x + y\n" +
        "        \\func w => x\n" +
        "      }\n" +
        "      \\export C\n" +
        "    }\n" +
        "    \\class D { \\export B }\n" +
        "    \\func f (b : B) : b.C.z = b.z => path (\\lam _ => b.w + b.y)\n" +
        "  }\n" +
        "}");
    assertNotNull(Root.getModule(new NameModuleSourceId("test")));
    Namespace namespace = Root.getModule(new NameModuleSourceId("test")).namespace;

    assertEquals(namespace.getMembers().toString(), 2, namespace.getMembers().size());
    assertNotNull(namespace.getMember("A"));
    Concrete.ClassDefinition classA = (Concrete.ClassDefinition) namespace.getMember("A").abstractDefinition;
    assertEquals(classA.getStatements().toString(), 4, classA.getStatements().size());
    assertEquals(namespace.getMember("A").namespace.getMembers().toString(), 4, namespace.getMember("A").namespace.getMembers().size());
    Concrete.ClassDefinition classB = (Concrete.ClassDefinition) getField(classA, "B");
    assertNotNull(classB);
    assertEquals(classB.getStatements().toString(), 3, classB.getStatements().size());
    Concrete.ClassDefinition classC = (Concrete.ClassDefinition) getField(classB, "C");
    assertNotNull(classC);
    assertEquals(classC.getStatements().toString(), 2, classC.getStatements().size());
    Concrete.ClassDefinition classD = (Concrete.ClassDefinition) getField(classA, "D");
    assertNotNull(classD);
    assertEquals(classD.getStatements().toString(), 1, classD.getStatements().size());
    */
    assertTrue(false);
  }

  private Concrete.Definition getField(Concrete.ClassDefinition classDefinition, String name) {
    /*
    for (Concrete.Statement statement : classDefinition.getStatements()) {
      if (statement instanceof Concrete.DefineStatement && ((Concrete.DefineStatement) statement).getDefinition().textRepresentation().equals(name)) {
        return ((Concrete.DefineStatement) statement).getDefinition();
      }
    }
    */
    return null;
  }

  @Test
  public void staticDynamicDuplicateError() {
    resolveNamesDef(
      "\\class Test {\n" +
      "  \\func A => 0\n" +
      "} \\where {\n" +
      "  \\class A { }\n" +
      "}", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void defineExistingStaticTestError() {
    resolveNamesModule("\\class A { } \\func A => 0", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void defineExistingDynamicTestError() {
    resolveNamesDef("\\class Test { \\class A \\func A => 0 }", 1);
    assertThatErrorsAre(error());
  }

  @Ignore
  @Test
  public void exportExistingTestError() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } } \\export A \\class B \\where { \\func y => 0 }", 2);
  }

  @Ignore
  @Test
  public void exportExistingTestError2() {
    resolveNamesModule("\\class B \\where { \\func y => 0 } \\class A \\where { \\class B \\where { \\func x => 0 } } \\export A", 2);
  }

  @Test
  public void openInsideTest() {
    resolveNamesModule(
      "\\class A \\where {\n" +
      "  \\class B \\where\n" +
      "    \\func x => 0\n" +
      "  \\open B\n" +
      "}\n" +
      "\\func y => A.x", 1);
    assertThatErrorsAre(error());
  }

  @Ignore
  @Test
  public void exportInsideTest() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\export B } \\func y => A.x");
  }

  @Test
  public void classExtensionWhereTestError() {
    resolveNamesModule(
        "\\func f => 0 \\where {\n" +
        "  \\class A {}\n" +
        "  \\class A { \\func x => 0 }\n" +
        "}", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void multipleDefsWhere() {
    resolveNamesModule(
        "\\func f => 0 \\where {\n" +
        "  \\func d => 0\n" +
        "  \\func d => 1\n" +
        "}", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void dataConstructor() {
    resolveNamesModule("\\data D | d \\func f => D.d");
  }

  @Test
  public void testPreludeSuc() {
    resolveNamesDef("\\func test' => suc");
  }

  @Test
  public void testPreludeNonExistentMember() {
    resolveNamesDef("\\func test' => foo", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void openDefined() {
    resolveNamesModule(
        "\\func f => \\Type0\n" +
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\open X\n" +
        "\\func g => f");
  }

  @Test
  public void openUsingDefined() {
    resolveNamesModule(
        "\\func f => \\Type0\n" +
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\open X(f)\n" +
        "\\func g => f", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void openRenaming() {
    resolveNamesModule(
        "\\func f => \\Type0\n" +
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\open X(f \\as f')\n" +
        "\\func g => f\n" +
        "\\func g' => f'");
  }

  @Test
  public void openRenamingDefined() {
    resolveNamesModule(
        "\\func f' => \\Type0\n" +
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\open X(f \\as f')", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void openDuplicateName() {
    resolveNamesModule(
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\class Y \\where { \\func f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y\n" +
        "\\func g => f", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void openDuplicateModuleHiding() {
    resolveNamesModule(
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\class Y \\where { \\func f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y \\hiding (f)\n" +
        "\\func g => f");
  }

  @Test
  public void openUsingDuplicate() {
    resolveNamesModule(
        "\\class X \\where { \\func f => \\Type0 }\n" +
        "\\class Y \\where { \\func f => \\Type0 }\n" +
        "\\open X(f)\n" +
        "\\open Y(f)\n" +
        "\\func g => f", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void conditionsTest() {
    resolveNamesModule(
      "\\data I | left | right\n" +
      "\\data D (A : \\Type)\n" +
      "  | con1 A\n" +
      "  | con2 (D A) I {\n" +
      "    | left => d\n" +
      "    | right => d\n" +
      "  }", 2);
    assertThatErrorsAre(error(), error());
  }

  @Test
  public void patternsTest() {
    resolveNamesModule(
      "\\data K\n" +
      "  | k1 \\Prop\n" +
      "  | k2 \\Prop\n" +
      "\\func crash (k : K) : \\Prop\n" +
      "  | k1 a => a\n" +
      "  | k2 b => a", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void nameResolverLamOpenError() {
    resolveNamesExpr("\\lam (x : Nat) => (\\lam (y : Nat) => \\Pi (z : Nat -> \\Type0) (y : Nat) -> z ((\\lam (y : Nat) => y) y)) y", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void openExportTestError() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\open B } \\func y => A.x", 1);
    assertThatErrorsAre(error());
  }

  @Ignore
  @Test
  public void staticClassExportTest() {
    resolveNamesModule("\\class A \\where { \\func x => 0 } \\class B \\where { \\export A } \\func y => B.x");
  }

  @Ignore
  @Test
  public void nonStaticClassExportTestError() {
    resolveNamesModule("\\class Test { \\class A \\where { \\func x => 0 } } \\where { \\class B \\where { \\export A } \\func y => B.x }", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void nameResolverPiOpenError() {
    resolveNamesExpr("\\Pi (A : Nat -> \\Type0) (a b : A a) -> A 0", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void fieldsAreOpen() {
    resolveNamesModule("\\class Test { \\func y => x } \\where { \\class A { | x : Nat } }");
  }

  @Test
  public void dynamicsAreNotOpen() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat \\func y => x } }", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void staticsAreNotOpen() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat } \\where { \\func y => 0 } }", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void openStatics() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat } \\where { \\func y => 0 } \\open A }");
  }

  @Test
  public void openDynamics() {
    resolveNamesModule("\\class Test { \\func z => y } \\where { \\class A { | x : Nat \\func y => x } \\open A }");
  }

  @Test
  public void whereError() {
    resolveNamesModule(
      "\\func f (x : Nat) => x \\where\n" +
      "  \\func b => x", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void whereNoOpenFunctionError() {
    resolveNamesModule(
      "\\func f => x \\where\n" +
      "  \\func b => 0 \\where\n" +
      "    \\func x => 0", 1);
    assertThatErrorsAre(error());
  }

  @Ignore
  @Test
  public void export2TestError() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\func x => 0 } \\export B } \\func y => x", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void dynamicFunctionTest() {
    resolveNamesModule("\\class A { \\func x => 0 } \\func y : Nat => x", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void dynamicFunctionOpenTest() {
    resolveNamesModule("\\class A { \\func x => 0 } \\func y : Nat => x \\open A");
  }

  @Test
  public void duplicateInternalName() {
    resolveNamesModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "}\n" +
      "\\data D | x Nat", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void duplicateInternalExternalName() {
    resolveNamesModule(
      "\\data D | x Nat\n" +
      "\\func x => 0");
  }

  @Test
  public void classExtensionDuplicateFieldName() {
    resolveNamesModule(
      "\\class A {\n" +
      "  | f : Nat\n" +
      "}\n" +
      "\\class C {\n" +
      "  \\class B \\extends A {\n" +
      "    | f : Nat\n" +
      "  }\n" +
      "}", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void openHideTest() {
    resolveNamesModule(
      "\\class X \\where { \\func f => 0 }\n" +
      "\\open X(f) \\hiding(f)\n" +
      "\\func g => f", 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void openRenameHideOldTest() {
    resolveNamesModule(
      "\\class X \\where { \\func f => 0 }\n" +
      "\\open X(f \\as f') \\hiding(f)\n" +
      "\\func g => f'", 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void openRenameHideNewTest() {
    resolveNamesModule(
      "\\class X \\where { \\func f => 0 }\n" +
      "\\open X(f \\as f') \\hiding(f')\n" +
      "\\func g => f\n" +
      "\\func g' => f'", 2);
    assertThatErrorsAre(notInScope("f"), notInScope("f'"));
  }

  @Test
  public void openElementsTest() {
    ChildGroup group = resolveNamesModule(
      "\\import Prelude()\n" +
      "\\module X \\where { \\func f => 0 }\n" +
      "\\open X\n" +
      "\\func g => f");
    List<String> names = new ArrayList<>();
    for (Referable element : group.getGroupScope().getElements()) {
      names.add(element.textRepresentation());
    }
    assertEquals(Arrays.asList("X", "g", "f"), names);
  }

  @Test
  public void importTest() {
    resolveNamesModule("\\import Foo", 1);
  }

  @Test
  public void importPrelude() {
    resolveNamesModule(
      "\\import Prelude\n" +
      "\\func f : Nat => 0");
  }

  @Test
  public void hidePrelude() {
    resolveNamesModule(
      "\\import Prelude()\n" +
      "\\func f : Nat => 0", 1);
  }

  @Test
  public void importHidingName() {
    libraryManager.setModuleScopeProvider(module -> module == Prelude.MODULE_PATH ? preludeLibrary.getModuleScopeProvider().forModule(module) : EmptyScope.INSTANCE);
    resolveNamesModule(
      "\\import Mod\n" +
      "\\import Mod.Path\n" +
      "\\func foo => Path");
  }

  @Test
  public void importHidingNamespace() {
    libraryManager.setModuleScopeProvider(module -> module == Prelude.MODULE_PATH ? preludeLibrary.getModuleScopeProvider().forModule(module) : EmptyScope.INSTANCE);
    resolveNamesModule(
      "\\import Mod\n" +
      "\\import Mod.Path\n" +
      "\\func foo => Path.path");
  }

  @Test
  public void importOrder() {
    libraryManager.setModuleScopeProvider(module -> module == Prelude.MODULE_PATH
      ? preludeLibrary.getModuleScopeProvider().forModule(module)
      : module.equals(new ModulePath("Mod"))
        ? new SingletonScope(new LocatedReferableImpl(Precedence.DEFAULT, "foo", module))
        : EmptyScope.INSTANCE);
    /*
    resolveNamesModule(
      "\\import Mod\n" +
      "\\import Mod.Path\n" +
      "\\func bar => foo");
    */
    resolveNamesModule(
      "\\import Mod.Path\n" +
      "\\import Mod\n" +
      "\\func bar => foo");
  }
}
