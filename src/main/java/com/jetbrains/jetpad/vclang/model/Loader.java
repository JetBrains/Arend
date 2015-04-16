package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import jetbrains.jetpad.json.JsonArray;
import jetbrains.jetpad.json.JsonParser;
import jetbrains.jetpad.otmodel.json.Node2JsonConverter;
import jetbrains.jetpad.otmodel.node.ot.persistence.NodeIdPersistenceContext;
import jetbrains.jetpad.otmodel.ot.command.EntityContextCommandProcessor;
import jetbrains.jetpad.otmodel.ot.persistence.id.BaseIdCompressor;
import jetbrains.jetpad.otmodel.ot.persistence.id.SelfUpdatingIdCompressor;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public class Loader {
  public static ModuleMapper create(String data, Hook hook) {
    final WrapperContext ctx = Setup.context();
    final Module module = create(data, ctx);

    final Root root = new Root(ctx);
    root.input.set(module);

    EntityContextCommandProcessor processor = ctx.get(EntityContextCommandProcessor.KEY);

    processor.execute(new Runnable() {
      @Override
      public void run() {
        ctx.getRoots().add(root);
      }
    });
    hook.run(root);

    ModuleMapper mapper = new ModuleMapper(module);
    mapper.attachRoot();
    return mapper;
  }

  private static Module create(String data, WrapperContext ctx) {
    if (data == null || data.isEmpty() /* temporary */) {
      return Example.create(ctx);
    }

    JsonArray jsonArray = (JsonArray) JsonParser.parse(data);
    BaseIdCompressor idCompressor = new SelfUpdatingIdCompressor();
    Node2JsonConverter converter = new Node2JsonConverter(new NodeIdPersistenceContext(idCompressor));
    jetbrains.jetpad.otmodel.node.Node node = converter.fromJson(jsonArray);
    return (Module) ctx.getWrapperFor(node);
  }

  public static final Hook MOCK = new Hook() {
    @Override
    public void run(Root root) {}
  };

  public static interface Hook {
    void run(Root root);
  }
}
