package com.jetbrains.jetpad.vclang.module;

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
import static org.junit.Assert.assertTrue;

public class ModuleLoaderTest {
  @Test
  public void recursiveTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void recursiveTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void recursiveTestError3() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f => B.g \\function h => 0");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertTrue(moduleLoader.getErrors().size() > 0);
  }

  @Test
  public void nonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\function g => A.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void moduleTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class C { \\function g : Nat \\function h => g }");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());

    assertEquals(2, moduleLoader.rootModule().findChild("A").getChildren().size());
    assertEquals(2, moduleLoader.rootModule().findChild("A").findChild("C").getChildren().size());
  }

  @Test
  public void nonStaticTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }

  @Test
  public void nonStaticTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void abstractNonStaticTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    MemorySourceSupplier sourceSupplier = new MemorySourceSupplier(moduleLoader);
    Module moduleA = new Module(moduleLoader.rootModule(), "A");
    Module moduleB = new Module(moduleLoader.rootModule(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    moduleLoader.reorderTypeCheckingUnits();
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }

  @Test
  public void numberOfFieldsTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 }");

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
    assertEquals(2, moduleLoader.rootModule().getChildren().size());
    assertEquals(2, moduleLoader.rootModule().getFields().size());
  }

  @Test
  public void openAbstractTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "\\class A { \\function x : Nat } \\open A \\function y => x";
    new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void openAbstractTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text = "\\class A { \\function x : Nat \\function y => x } \\open A \\function z => y";
    new BuildVisitor(new Module(moduleLoader.rootModule(), "test"), moduleLoader.rootModule(), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(1, moduleLoader.getErrors().size());
  }
}
