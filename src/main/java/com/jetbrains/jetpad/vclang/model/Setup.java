package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.otmodel.entity.Entity;
import jetbrains.jetpad.otmodel.node.Node;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.ot.command.EntityContextCommandProcessor;
import jetbrains.jetpad.otmodel.wrapper.*;

public class Setup {
  public static EntityWrapperFactory factoryForEntities() {
    return new EntityWrapperFactory() {
      @Override
      public EntityWrapper<?> createWrapperFor(WrapperContext ctx, Entity e) {
        if (Root.CONCEPT_ID.equals(e.getConceptId())) {
          return new Root(ctx, e);
        }
        return null;
      }
    };
  }

  public static NodeWrapperFactory factoryForNodes() {
    return new NodeWrapperFactory() {
      @Override
      public NodeWrapper<?> createWrapperFor(WrapperContext ctx, Node node) {
        NodeConceptId conceptId = node.getConceptId();

        if (Module.CONCEPT_ID.equals(conceptId)) {
          return new Module(ctx, node);
        }

        throw new RuntimeException("Not implemented yet.");
      }
    };
  }

  public static WrapperContext context() {
    WrapperContext ctx = new WrapperContext();
    EntityContextCommandProcessor processor = new EntityContextCommandProcessor(ctx.getEntityContext());
    ctx.put(EntityContextCommandProcessor.KEY, processor);
    ctx.addEntityWrapperFactory(factoryForEntities());
    ctx.addNodeWrapperFactory(factoryForNodes());
    return ctx;
  }
}
