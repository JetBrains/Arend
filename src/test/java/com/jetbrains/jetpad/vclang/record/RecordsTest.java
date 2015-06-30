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
  public void unknownExtTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class Point { \\function x : Nat \\function y : Nat } \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }");
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>(1);
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(1, errors.size());
  }
}
