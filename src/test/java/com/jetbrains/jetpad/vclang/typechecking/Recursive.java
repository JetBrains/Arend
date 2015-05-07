package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Recursive {
  @Test
  public void list() {
    Concrete.Definition def = parseDef("\\data List (A : \\Type0) | nil | cons A (List A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    Definition newDef = def.accept(new DefinitionCheckTypeVisitor(Prelude.DEFINITIONS, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(newDef);
  }

  @Test
  public void dataLeftError() {
    Concrete.Definition def = parseDef("\\data List (A : \\Type0) | nil | cons (List A -> A)");
    List<TypeCheckingError> errors = new ArrayList<>();
    def.accept(new DefinitionCheckTypeVisitor(Prelude.DEFINITIONS, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }

  @Test
  public void dataRightError() {
    Concrete.Definition def = parseDef("\\data List (B : \\Type0 -> \\Type0) (A : \\Type0) | nil | cons (B (List B A))");
    List<TypeCheckingError> errors = new ArrayList<>();
    def.accept(new DefinitionCheckTypeVisitor(Prelude.DEFINITIONS, errors), new ArrayList<Binding>());
    assertEquals(1, errors.size());
  }
}
