package com.jetbrains.jetpad.vclang.editor;

import com.google.common.base.Supplier;
import com.jetbrains.jetpad.vclang.editor.definition.DefinitionMapperFactory;
import com.jetbrains.jetpad.vclang.editor.expr.ExpressionCompletion;
import com.jetbrains.jetpad.vclang.editor.expr.ExpressionMapperFactory;
import com.jetbrains.jetpad.vclang.editor.expr.SideTransformMapperProcessor;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.EmptyDefinition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.util.CellLists;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.Synchronizer;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;
import jetbrains.jetpad.projectional.generic.Role;
import jetbrains.jetpad.projectional.generic.RoleCompletion;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public class Synchronizers {
  public static Synchronizer forDefinitions(Mapper<? extends Node, ? extends Cell> mapper, ObservableList<Definition> definitions, Cell target) {
    ProjectionalRoleSynchronizer<Node, Definition> synchronizer = ProjectionalSynchronizers.forRole(mapper, definitions, target, CellLists.newLineSeparated(target.children()), DefinitionMapperFactory.getInstance());
    synchronizer.setCompletion(new RoleCompletion<Node, Definition>() {
      @Override
      public List<CompletionItem> createRoleCompletion(CompletionParameters ctx, Mapper<?, ?> mapper, Node contextNode, final Role<Definition> target) {
        List<CompletionItem> result = new ArrayList<>();
        result.add(new SimpleCompletionItem("fun ", "function") {
          @Override
          public Runnable complete(String text) {
            return target.set(new FunctionDefinition());
          }
        });
        return result;
      }
    });
    synchronizer.setItemFactory(new Supplier<Definition>() {
      @Override
      public Definition get() {
        return new EmptyDefinition();
      }
    });
    return synchronizer;
  }

  public static Synchronizer forExpression(Mapper<? extends Node, ? extends Cell> mapper, Property<Expression> expression, Cell target, String placeholderText) {
    ProjectionalRoleSynchronizer<Node, Expression> synchronizer = ProjectionalSynchronizers.forSingleRole(mapper, expression, target, ExpressionMapperFactory.getInstance());
    synchronizer.setPlaceholderText(placeholderText);
    synchronizer.setCompletion(ExpressionCompletion.getInstance());
    synchronizer.addMapperProcessor(SideTransformMapperProcessor.getInstance());
    return synchronizer;
  }

  public static Synchronizer forArgument(Mapper<? extends Node, ? extends Cell> mapper, Property<Expression> argument, Cell target, String placeholderText) {
    ProjectionalRoleSynchronizer<Node, Expression> synchronizer = ProjectionalSynchronizers.forSingleRole(mapper, argument, target, ExpressionMapperFactory.getInstance());
    synchronizer.setPlaceholderText(placeholderText);
    synchronizer.setCompletion(new RoleCompletion<Node, Expression>() {
      @Override
      public List<CompletionItem> createRoleCompletion(CompletionParameters ctx, Mapper<?, ?> mapper, Node contextNode, final Role<Expression> target) {
        List<CompletionItem> result = ExpressionCompletion.getInstance().createRoleCompletion(ctx, mapper, contextNode, target);
        result.add(new SimpleCompletionItem("tele ", "telescope") {
          @Override
          public Runnable complete(String text) {
            return target.set(new Model.TelescopeArgument());
          }
        });
        return result;
      }
    });
    return synchronizer;
  }
}
