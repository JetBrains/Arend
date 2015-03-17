package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.editor.definition.FunctionDefinitionMapper;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.model.property.Property;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class IdentifierTest extends EditingTestCase {
  private Property<Expression> myTerm;

  @Before
  public void init() {
    FunctionDefinition def = new FunctionDefinition();
    myModule.definitions.add(def);
    myTerm = def.term();

    Cell cell = ((FunctionDefinitionMapper.Cell) myRootMapper.getDescendantMapper(def).getTarget()).term;
    cell.focusable().set(true);
    Composites.firstFocusable(cell).focus();
  }

  @Test
  public void create1() {
    myViewContainer.keyTyped(new KeyEvent(null, 'x', Collections.<ModifierKey>emptySet()));
    assertTrue(myTerm.get() instanceof VarExpression);
    assertEquals("x", ((VarExpression) myTerm.get()).getName());
  }

  @Test
  public void create2() {
    myViewContainer.keyTyped(new KeyEvent(null, 'x', Collections.<ModifierKey>emptySet()));
    myViewContainer.keyTyped(new KeyEvent(null, 'y', Collections.<ModifierKey>emptySet()));
    assertTrue(myTerm.get() instanceof VarExpression);
    assertEquals("xy", ((VarExpression) myTerm.get()).getName());
  }

  @Test
  public void createFail() {
    myViewContainer.keyTyped(new KeyEvent(null, '1', Collections.<ModifierKey>emptySet()));
    assertFalse(myTerm.get() instanceof VarExpression);
  }

  @Test
  public void delete() {
    myViewContainer.keyTyped(new KeyEvent(null, 'x', Collections.<ModifierKey>emptySet()));
    myViewContainer.keyTyped(new KeyEvent(null, 'y', Collections.<ModifierKey>emptySet()));
    myViewContainer.keyPressed(new KeyEvent(Key.BACKSPACE));
    assertTrue(myTerm.get() instanceof VarExpression);
    myViewContainer.keyPressed(new KeyEvent(Key.BACKSPACE));
    assertEquals(null, myTerm.get());
  }
}
