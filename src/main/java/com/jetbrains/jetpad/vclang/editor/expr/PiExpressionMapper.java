package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forArgument;
import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.PiExpression;
import static jetbrains.jetpad.cell.util.CellFactory.*;

public class PiExpressionMapper extends ExpressionMapper<PiExpression, PiExpressionMapper.Cell> {
  public PiExpressionMapper(PiExpression source) {
    super(source, new Cell(source.position().prec() > Abstract.PiExpression.PREC));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forArgument(this, getSource().domain(), getTarget().domain, "<dom>"));
    conf.add(forExpression(this, getSource().codomain(), getTarget().codomain, "<cod>"));
  }

  public static class Cell extends IndentCell {
    public final jetbrains.jetpad.cell.Cell domain = noDelete(indent());
    public final jetbrains.jetpad.cell.Cell codomain = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(label("("));
      CellFactory.to(this,
          domain,
          space(),
          text("->"),
          space(),
          codomain);
      if (parens) children().add(label(")"));

      focusable().set(true);
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toFirstFocusable(domain));
    }
  }
}
