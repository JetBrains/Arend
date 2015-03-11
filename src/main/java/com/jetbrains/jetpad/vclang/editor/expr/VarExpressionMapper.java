package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.completion.Completion;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.CompletionSupplier;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.property.Property;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.editor.util.Validators.identifier;
import static jetbrains.jetpad.cell.text.TextEditing.validTextEditing;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class VarExpressionMapper extends Mapper<VarExpression, TextCell> {
  public VarExpressionMapper(VarExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().addTrait(validTextEditing(identifier()));




    getTarget().addTrait(new CellTrait() {


      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {

        if (spec == Completion.RIGHT_TRANSFORM) {
          return new CompletionSupplier() {
            @Override
            public List<CompletionItem> get(CompletionParameters cp) {
              List<CompletionItem> result = new ArrayList<>();

              result.add(new SimpleCompletionItem("") {
                @Override
                public Runnable complete(String text) {
                  AppExpression appExpr = new AppExpression();
                  Mapper<?, ?> parent = getParent();
                  ((Property<Expression>) getSource().getPosition().getRole()).set(appExpr);
                  AppExpressionMapper appExprMapper = (AppExpressionMapper) parent.getDescendantMapper(appExpr);
                  appExpr.function().set(getSource());

                  return CellActions.toFirstFocusable(appExprMapper.getTarget().argument);

                }
              });

              return result;
            }
          };

        }

        return super.get(cell, spec);
      }
    });
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name(), getTarget().text()));
  }
}
