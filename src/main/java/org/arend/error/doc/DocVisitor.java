package org.arend.error.doc;

public interface DocVisitor<P, R> {
  R visitVList(VListDoc doc, P params);
  R visitHList(HListDoc doc, P params);
  R visitText(TextDoc doc, P params);
  R visitHang(HangDoc doc, P params);
  R visitReference(ReferenceDoc doc, P params);
  R visitCaching(CachingDoc doc, P params);
  R visitTermLine(TermLineDoc doc, P params);
}
