package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.model.composite.Composites;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.model.expr.Model.AppExpression;
import static com.jetbrains.jetpad.vclang.model.expr.Model.VarExpression;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SideTransformTest extends EditingTestCase {
  private VarExpression myVarExpr1 = new VarExpression();
  private VarExpression myVarExpr2 = new VarExpression();
  private AppExpression myAppExpr = new AppExpression();
  private FunctionDefinition myDef = new FunctionDefinition();

  @Before
  public void init() {
    myModule.definitions.add(myDef);
    myDef.term().set(myAppExpr);
    myAppExpr.function().set(myVarExpr1);
    myVarExpr1.name().set("x");
    myAppExpr.argument().set(myVarExpr2);
    myVarExpr2.name().set("y");
  }

  @Test
  public void appLeftFun() {
    TextCell cell = (TextCell) myRootMapper.getDescendantMapper(myVarExpr1).getTarget();
    cell.caretPosition().set(0);
    Composites.<Cell>firstFocusable(cell).focus();
    myViewContainer.keyTyped(new KeyEvent(null, ' ', Collections.<ModifierKey>emptySet()));
    assertTrue(myDef.getTerm() instanceof AppExpression);
    AppExpression appExpr1 = (AppExpression) myDef.getTerm();
    assertTrue(appExpr1.getFunction() instanceof AppExpression);
    AppExpression appExpr2 = (AppExpression) appExpr1.getFunction();
    assertEquals(myVarExpr1, appExpr2.getArgument());
    assertEquals(myVarExpr2, appExpr1.getArgument());
    myViewContainer.keyTyped(new KeyEvent(null, 'z', Collections.<ModifierKey>emptySet()));
    assertTrue(appExpr2.getFunction() instanceof VarExpression);
    assertEquals("z", ((VarExpression) appExpr2.getFunction()).getName());
  }

  @Test
  public void appRightFun() {
    TextCell cell = (TextCell) myRootMapper.getDescendantMapper(myVarExpr1).getTarget();
    cell.caretPosition().set(cell.text().get().length());
    Composites.<Cell>firstFocusable(cell).focus();
    myViewContainer.keyTyped(new KeyEvent(null, ' ', Collections.<ModifierKey>emptySet()));
    assertTrue(myDef.getTerm() instanceof AppExpression);
    AppExpression appExpr1 = (AppExpression) myDef.getTerm();
    assertTrue(appExpr1.getFunction() instanceof AppExpression);
    AppExpression appExpr2 = (AppExpression) appExpr1.getFunction();
    assertEquals(myVarExpr1, appExpr2.getFunction());
    assertEquals(myVarExpr2, appExpr1.getArgument());
    myViewContainer.keyTyped(new KeyEvent(null, 'z', Collections.<ModifierKey>emptySet()));
    assertTrue(appExpr2.getArgument() instanceof VarExpression);
    assertEquals("z", ((VarExpression) appExpr2.getArgument()).getName());
  }

  @Test
  public void appLeftArg() {
    TextCell cell = (TextCell) myRootMapper.getDescendantMapper(myVarExpr2).getTarget();
    cell.caretPosition().set(0);
    Composites.<Cell>firstFocusable(cell).focus();
    myViewContainer.keyTyped(new KeyEvent(null, ' ', Collections.<ModifierKey>emptySet()));
    assertTrue(myDef.getTerm() instanceof AppExpression);
    AppExpression appExpr1 = (AppExpression) myDef.getTerm();
    assertTrue(appExpr1.getFunction() instanceof AppExpression);
    AppExpression appExpr2 = (AppExpression) appExpr1.getFunction();
    assertEquals(myVarExpr1, appExpr2.getFunction());
    assertEquals(myVarExpr2, appExpr1.getArgument());
    myViewContainer.keyTyped(new KeyEvent(null, 'z', Collections.<ModifierKey>emptySet()));
    assertTrue(appExpr2.getArgument() instanceof VarExpression);
    assertEquals("z", ((VarExpression) appExpr2.getArgument()).getName());
  }

  @Test
  public void appRightArg() {
    TextCell cell = (TextCell) myRootMapper.getDescendantMapper(myVarExpr2).getTarget();
    cell.caretPosition().set(cell.text().get().length());
    Composites.<Cell>firstFocusable(cell).focus();
    myViewContainer.keyTyped(new KeyEvent(null, ' ', Collections.<ModifierKey>emptySet()));
    assertTrue(myDef.getTerm() instanceof AppExpression);
    AppExpression appExpr1 = (AppExpression) myDef.getTerm();
    assertTrue(appExpr1.getFunction() instanceof AppExpression);
    AppExpression appExpr2 = (AppExpression) appExpr1.getFunction();
    assertEquals(myVarExpr1, appExpr2.getFunction());
    assertEquals(myVarExpr2, appExpr2.getArgument());
    myViewContainer.keyTyped(new KeyEvent(null, 'z', Collections.<ModifierKey>emptySet()));
    assertTrue(appExpr1.getArgument() instanceof VarExpression);
    assertEquals("z", ((VarExpression) appExpr1.getArgument()).getName());
  }
}
