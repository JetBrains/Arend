package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.net.URI;

public interface PersistenceProvider<SourceIdT extends SourceId> {
  URI getUri(SourceIdT sourceId);
  SourceIdT getModuleId(URI sourceUrl);
  String getIdFor(GlobalReferable definition);
  GlobalReferable getFromId(SourceIdT sourceId, String id);
}
