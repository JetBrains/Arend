package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.net.URL;

public interface PersistenceProvider<SourceIdT extends SourceId> {
  URL getUrl(SourceIdT sourceId);
  SourceIdT getModuleId(URL sourceUrl);
  String getIdFor(Abstract.Definition definition);
  Abstract.Definition getFromId(SourceIdT sourceId, String id);
}
