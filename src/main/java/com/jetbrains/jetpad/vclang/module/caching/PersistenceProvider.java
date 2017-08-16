package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.net.URI;

public interface PersistenceProvider<SourceIdT extends SourceId> {
  URI getUri(SourceIdT sourceId);
  SourceIdT getModuleId(URI sourceUrl);
  String getIdFor(Abstract.GlobalReferableSourceNode definition);
  Abstract.Definition getFromId(SourceIdT sourceId, String id);
}
