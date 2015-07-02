package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parse;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.assertEquals;

public class RecordsTest {
  @Test
  public void recordTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class B { \\function f : Nat -> \\Type0 \\function g : f 0 } \\function f (p : B) : p.f 0 => p.g ");

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }

  @Test
  public void unknownExtTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void typeMismatchMoreTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x (a : Nat) => a }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void typeMismatchLessTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class C { \\function f (x y z : Nat) : Nat } \\function D => C { \\override f a => \\lam z w => z }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }

  @Test
  public void argTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class C { \\function f (a : Nat) : Nat } \\function D => C { \\override f (a : Nat -> Nat) => 0 }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void resultTypeMismatchTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => \\lam (t : Nat) => t }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void parentCallTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader,
        "\\class A {\n" +
            "\\function c : Nat -> Nat -> Nat\n" +
            "\\function f : Nat -> Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
            "\\override f n <= c n n\n" +
        "}");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }

  @Test
  public void recursiveTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class A { \\function f : Nat -> Nat } \\function B => A { \\override f n <= \\elim n | zero => zero | suc n' => f (suc n') }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void duplicateNameTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A {\n" +
            "\\function f : Nat -> Nat\n" +
        "}\n" +
        "\\function B => A {\n" +
            "\\function f (n : Nat) <= n\n" +
        "}";
    new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void overriddenFieldAccTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class Point {\n" +
          "\\function x : Nat\n" +
          "\\function y : Nat\n" +
        "}\n" +
        "\\function diagonal => \\lam (d : Nat) => Point {\n" +
          "\\override x => d\n" +
          "\\override y => d\n" +
        "}\n" +
        "\\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)";
    parseDefs(moduleLoader, text);

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }
}
