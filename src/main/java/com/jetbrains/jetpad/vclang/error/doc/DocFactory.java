package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.*;

public class DocFactory {
  public static ReferenceDoc refDoc(Abstract.ReferableSourceNode sourceNode) {
    return new ReferenceDoc(sourceNode);
  }

  public static LineDoc empty() {
    return new TextDoc("");
  }

  public static Doc nullDoc() {
    return new VListDoc(Collections.emptyList());
  }

  public static Doc hang(Doc top, Doc bottom) {
    return bottom.isNull() ? top : new HangDoc(top, bottom);
  }

  public static Doc vHang(Doc top, Doc... bottom) {
    return vList(top, indent(vList(bottom)));
  }

  public static Doc indent(Doc doc) {
    return new HangDoc(nullDoc(), doc);
  }

  public static LineDoc text(String string) {
    return string == null ? empty() : new TextDoc(string);
  }

  public static Doc multiline(String string) {
    if (string == null) {
      return nullDoc();
    }

    String[] lines = string.split("\\n");
    if (lines.length == 0) {
      return nullDoc();
    }
    if (lines.length == 1) {
      return new TextDoc(lines[0]);
    }

    List<Doc> docs = new ArrayList<>(lines.length);
    for (String line : lines) {
      docs.add(new TextDoc(line));
    }
    return new VListDoc(docs);
  }

  public static TermDoc termDoc(Expression expression) {
    return new TermDoc(expression);
  }

  public static TermLineDoc termLine(Expression expression) {
    return new TermLineDoc(expression);
  }

  public static Doc typeDoc(ExpectedType type) {
    if (type instanceof Expression) {
      return new TermDoc((Expression) type);
    }
    if (type == ExpectedType.OMEGA) {
      return new TextDoc("a universe");
    }
    throw new IllegalStateException();
  }

  public static SourceNodeDoc sourceNodeDoc(Abstract.SourceNode sourceNode, PrettyPrinterInfoProvider infoProvider) {
    return new SourceNodeDoc(sourceNode, infoProvider);
  }

  public static Doc vList(Collection<? extends Doc> docs) {
    return docs.size() == 1 ? docs.iterator().next() : new VListDoc(docs);
  }

  public static Doc vList(Doc... docs) {
    if (docs.length == 1) {
      return docs[0];
    }

    List<Doc> list = new ArrayList<>(docs.length);
    for (Doc doc : docs) {
      if (!doc.isNull()) {
        list.add(doc);
      }
    }
    return new VListDoc(list);
  }

  public static LineDoc hList(Collection<? extends LineDoc> docs) {
    return docs.size() == 1 ? docs.iterator().next() : new HListDoc(docs);
  }

  public static LineDoc hList(LineDoc... docs) {
    return docs.length == 1 ? docs[0] : new HListDoc(Arrays.asList(docs));
  }

  public static LineDoc hSep(LineDoc sep, Collection<? extends LineDoc> docs) {
    if (docs.isEmpty() || sep.isEmpty()) {
      return new HListDoc(docs);
    } else {
      List<LineDoc> list = new ArrayList<>(docs.size() * 2 - 1);
      boolean first = true;
      for (LineDoc doc : docs) {
        if (!doc.isEmpty()) {
          if (first) {
            first = false;
          } else {
            list.add(sep);
          }
          list.add(doc);
        }
      }
      return hList(list);
    }
  }

  public static LineDoc hSep(LineDoc sep, LineDoc... docs) {
    return hSep(sep, Arrays.asList(docs));
  }

  public static HListDoc hEnd(LineDoc sep, Collection<? extends LineDoc> docs) {
    List<LineDoc> list = new ArrayList<>(docs.size() * 2);
    for (LineDoc doc : docs) {
      if (!doc.isEmpty()) {
        list.add(doc);
        list.add(sep);
      }
    }
    return new HListDoc(list);
  }

  public static HListDoc hEnd(LineDoc sep, LineDoc... docs) {
    return hEnd(sep, Arrays.asList(docs));
  }
}
