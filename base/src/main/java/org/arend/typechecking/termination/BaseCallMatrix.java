package org.arend.typechecking.termination;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.util.StringFormat;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseCallMatrix<T> {
  public enum R {
    Unknown(),
    Equal(),
    LessThan()
  }

  private static BaseCallMatrix.R rmul(BaseCallMatrix.R a, BaseCallMatrix.R b) {
    switch (a) {
      case Equal:
        switch (b) {
          case Equal:
            return BaseCallMatrix.R.Equal;
          case LessThan:
            return BaseCallMatrix.R.LessThan;
          default:
            return BaseCallMatrix.R.Unknown;
        }
      case LessThan:
        switch (b) {
          case Equal:
          case LessThan:
            return BaseCallMatrix.R.LessThan;
          default:
            return BaseCallMatrix.R.Unknown;
        }
      default:
        return BaseCallMatrix.R.Unknown;
    }
  }

  private static BaseCallMatrix.R radd(BaseCallMatrix.R a, BaseCallMatrix.R b) {
    if (a == R.LessThan || b == R.LessThan) return R.LessThan;
    if (a == R.Equal || b == R.Equal) return R.Equal;
    return R.Unknown;
  }

  static boolean rleq(BaseCallMatrix.R a, BaseCallMatrix.R b) {
    switch (a) {
      case LessThan:
        return (b == BaseCallMatrix.R.LessThan);
      case Equal:
        return (b == BaseCallMatrix.R.LessThan || b == BaseCallMatrix.R.Equal);
      default:
        return true;
    }
  }

  private final HashMap<Integer, HashMap<Integer, BaseCallMatrix.R>> matrixMap = new HashMap<>();

  private final int myWidth;
  private final int myHeight;

  BaseCallMatrix(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  BaseCallMatrix(BaseCallMatrix<T> m1, BaseCallMatrix<T> m2) {
    // multiplication constructor
    if (m1.myWidth != m2.myHeight) {
      throw new IllegalArgumentException();
    }
    myHeight = m1.myHeight;
    myWidth = m2.myWidth;

    for (Integer i : m1.matrixMap.keySet()) {
      HashMap<Integer, BaseCallMatrix.R> m1map = m1.matrixMap.get(i);
      for (Integer j : m1map.keySet()) {
        HashMap<Integer, BaseCallMatrix.R> m2map = m2.matrixMap.get(j);
        if (m2map != null) for (Map.Entry<Integer, R> e : m2map.entrySet()) {
          int k = e.getKey();
          BaseCallMatrix.R ik_value = getValue(i, k);
          if (ik_value != R.LessThan) {
            BaseCallMatrix.R ik_summand = rmul(m1map.get(j), e.getValue());
            BaseCallMatrix.R new_ik_value = radd(ik_value, ik_summand);
            if (new_ik_value != ik_value) set(i, k, new_ik_value);
          }
        }
      }
    }
  }

  int getHeight() {
    return myHeight;
  }

  int getWidth() {
    return myWidth;
  }

  public abstract T getCodomain();

  public abstract T getDomain();

  public abstract int getCompositeLength();

  public void set(int i, int j, BaseCallMatrix.R v) {
    if (v != R.Unknown) {
      HashMap<Integer, BaseCallMatrix.R> map = matrixMap.computeIfAbsent(i, k -> new HashMap<>());
      map.put(j, v);
    } else {
      HashMap<Integer, BaseCallMatrix.R> map = matrixMap.get(i);
      if (map != null) {
        map.remove(j);
      }
    }
  }

  public BaseCallMatrix.R getValue(int i, int j) {
    HashMap<Integer, BaseCallMatrix.R> result = matrixMap.get(i);
    if (result == null) return R.Unknown; else {
      BaseCallMatrix.R result2 = result.get(j);
      if (result2 == null) return R.Unknown; else return result2;
    }
  }

  @Override
  public final boolean equals(Object object) {
    if (object instanceof BaseCallMatrix) {
      BaseCallMatrix<?> cm = (BaseCallMatrix<?>) object;
      if (getCodomain() != cm.getCodomain() || getDomain() != cm.getDomain()) return false;
      return cm.matrixMap.equals(this.matrixMap);
    }
    return false;
  }

  public BaseCallMatrix.R compare(Object object) {
    if (object instanceof BaseCallMatrix) {
      BaseCallMatrix<?> cm = (BaseCallMatrix<?>) object;
      if (this.equals(cm)) return R.Equal;
      if (this.getDomain() != cm.getDomain() || this.getCodomain() != cm.getCodomain()) throw new IllegalArgumentException();
      for (Integer i : matrixMap.keySet()) {
        HashMap<Integer, R> map = matrixMap.get(i);
        for (Integer j : map.keySet()) {
          R my = map.get(j);
          R theirs = cm.getValue(i, j);
          if (!rleq(my, theirs)) return R.Unknown;
        }
      }
      return R.LessThan;
    }
    return R.Unknown;
  }

  @Override
  public final int hashCode() {
    int result = getCodomain().hashCode() * 31 + getDomain().hashCode();
    return result * 31 + matrixMap.hashCode();
  }

  protected String[] getColumnLabels() {
    String[] result = new String[myWidth];
    for (int i = 0; i < myWidth; i++) {
      result[i] = "?";
    }
    return result;
  }

  protected String[] getRowLabels() {
    String[] result = new String[myHeight];
    for (int i = 0; i < myHeight; i++) {
      result[i] = "?";
    }
    return result;
  }

  public Doc getMatrixLabel(PrettyPrinterConfig ppConfig) {
    return DocFactory.nullDoc();
  }

  static char rToChar(R r) {
    switch (r) {
      case Equal:
        return '=';
      case LessThan:
        return '<';
      default:
        return '?';
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(DocStringBuilder.build(getMatrixLabel(PrettyPrinterConfig.DEFAULT))).append('\n');
    String[] columnLabels = getColumnLabels();
    String[] rowLabels = getRowLabels();
    int max = 0;
    for (String label : rowLabels) {
      if (max < label.length()) {
        max = label.length();
      }
    }
    max++;

    result.append(" ".repeat(Math.max(0, max)));

    for (int j = 0; j < myWidth; j++) {
      result.append(columnLabels[j]).append(' ');
    }
    result.append('\n');

    for (int i = 0; i < myHeight; i++) {
      result.append(StringFormat.rightPad(max, rowLabels[i]));
      for (int j = 0; j < myWidth; j++) {
        result.append(StringFormat.rightPad(columnLabels[j].length() + 1, rToChar(getValue(i, j))));
      }
      result.append('\n');
    }

    return result.toString();
  }

  public String convertToTestCallMatrix() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < myHeight; i++) {
      boolean first = true;
      boolean found = false;
      for (int j = 0; j < myWidth; j++) {
        BaseCallMatrix.R value = getValue(i, j);
        if (value != R.Unknown) {
          if (!first) {
            result.append(", '-'");
          }
          if (result.length() > 0) result.append(", ");
          if (value == R.Equal) result.append("'='"); else if (value == R.LessThan) result.append("'<'");
          result.append(", ").append(j);
          first = false;
          found = true;
        }
      }

      if (!found) {
        if (result.length() > 0) result.append(", ");
        result.append("'?'");
      }
    }
    return result.toString();
  }
}
