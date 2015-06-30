package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.assertEquals;

public class RecordsTest {
  @Test
  public void unknownExtTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }");
    assertEquals(0, moduleLoader.getErrors().size());

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
    assertEquals(0, moduleLoader.getErrors().size());

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
    assertEquals(0, moduleLoader.getErrors().size());

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
    assertEquals(0, moduleLoader.getErrors().size());

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
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }
}
