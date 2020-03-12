package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCReferable;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypecheckerState implements TypecheckerState {
  private final Map<GlobalReferable, Definition> myTypechecked;

  public SimpleTypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  @Override
  public Definition record(TCReferable def, Definition res) {
    return myTypechecked.putIfAbsent(def, res);
  }

  @Override
  public void rewrite(TCReferable def, Definition res) {
    myTypechecked.put(def, res);
  }

  @Override
  public Definition getTypechecked(TCReferable def) {
    assert def != null;
    return myTypechecked.get(def);
  }

  @Override
  public Definition reset(TCReferable def) {
    return myTypechecked.remove(def);
  }

  @Override
  public void reset() {
    myTypechecked.clear();
  }
}
