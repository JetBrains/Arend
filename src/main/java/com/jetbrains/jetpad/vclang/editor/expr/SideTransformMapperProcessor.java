package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.completion.Completion;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.CompletionSupplier;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperProcessor;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.model.property.Property;

import java.util.ArrayList;
import java.util.List;

public class SideTransformMapperProcessor implements MapperProcessor<Expression, Cell> {
  private static final SideTransformMapperProcessor INSTANCE = new SideTransformMapperProcessor();

  private SideTransformMapperProcessor() {}

  @Override
  public void process(final Mapper<? extends Expression, ? extends Cell> mapper) {
    final Cell cell = mapper.getTarget();
    final Expression expr = mapper.getSource();

    Cell firstFocusable = Composites.firstFocusable(cell);
    Cell lastFocusable = Composites.lastFocusable(cell);

    if (lastFocusable != null) {
      lastFocusable.addTrait(new CellTrait() {
        @Override
        public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
          if (spec == Completion.RIGHT_TRANSFORM) {
            return new CompletionSupplier() {
              @Override
              public List<CompletionItem> get(CompletionParameters cp) {
                List<CompletionItem> result = new ArrayList<>();
                result.add(new SimpleCompletionItem(" ") {
                  @Override
                  public Runnable complete(String text) {
                    AppExpression appExpr = new AppExpression();
                    Mapper<?, ?> parent = mapper.getParent();
                    ((Property<Expression>) expr.getPosition().getRole()).set(appExpr);
                    AppExpressionMapper appExprMapper = (AppExpressionMapper) parent.getDescendantMapper(appExpr);
                    appExpr.setFunction(expr);
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

    if (firstFocusable != null) {
      firstFocusable.addTrait(new CellTrait() {
        @Override
        public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
          if (spec == Completion.LEFT_TRANSFORM) {
            return new CompletionSupplier() {
              @Override
              public List<CompletionItem> get(CompletionParameters cp) {
                List<CompletionItem> result = new ArrayList<>();
                result.add(new SimpleCompletionItem(" ") {
                  @Override
                  public Runnable complete(String text) {
                    AppExpression appExpr = new AppExpression();
                    Mapper<?, ?> parent = mapper.getParent();
                    ((Property<Expression>) expr.getPosition().getRole()).set(appExpr);
                    AppExpressionMapper appExprMapper = (AppExpressionMapper) parent.getDescendantMapper(appExpr);
                    appExpr.setArgument(expr);
                    return CellActions.toFirstFocusable(appExprMapper.getTarget().function);
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
  }

  public static SideTransformMapperProcessor getInstance() {
    return INSTANCE;
  }
}
