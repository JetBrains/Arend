package org.arend.ext.concrete.level;

import org.arend.ext.concrete.ConcreteSourceNode;

public interface ConcreteLevel extends ConcreteSourceNode {
  <P, R> R accept(ConcreteLevelVisitor<? super P, ? extends R> visitor, P params);
}
