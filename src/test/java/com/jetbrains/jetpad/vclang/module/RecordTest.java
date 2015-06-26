package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.assertEquals;

public class RecordTest {
  @Test
  public void recordTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader, "\\class B { \\function f : Nat -> \\Type0 \\function g : f 0 } \\function f (p : B) : p.f 0 => p.g ");
    assertEquals(0, moduleLoader.getErrors().size());

    List<TypeCheckingError> errors = new ArrayList<>();
    for (ModuleLoader.TypeCheckingUnit unit : moduleLoader.getTypeCheckingUnits()) {
      unit.rawDefinition.accept(new DefinitionCheckTypeVisitor(unit.typedDefinition, errors), new ArrayList<Binding>());
    }
    assertEquals(0, errors.size());
  }
}
