package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.UniverseExpression;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.values.Color;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.text;
import static jetbrains.jetpad.cell.util.ValueEditors.intProperty;
import static jetbrains.jetpad.mapper.Synchronizers.*;

public class UniverseExpressionMapper extends ExpressionMapper<UniverseExpression, UniverseExpressionMapper.Cell> {
  public UniverseExpressionMapper(UniverseExpression source) {
    super(source, new Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsTwoWay(getSource().level(), intProperty(getTarget().level)));
    conf.add(forPropsOneWay(getTarget().level.textColor(), getTarget().textColor));
    conf.add(forProperty(getSource().level(), new Runnable() {
      @Override
      public void run() {
        getSource().wellTypedExpr().set(null);
      }
    }));
  }

  public static class Cell extends IndentCell {
    public final TextCell level = noDelete(new TextCell());
    public final Property<Color> textColor;

    public Cell() {
      TextCell type = text("Type");
      textColor = type.textColor();
      CellFactory.to(this,
          type,
          level);

      focusable().set(true);
      level.addTrait(TextEditing.validTextEditing(Validators.unsignedInteger()));
    }
  }
}
