package org.arend.typechecking.termination;

import org.arend.typechecking.computation.ComputationRunner;
import java.util.*;

public abstract class BaseCallGraph<T> {
  private final HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> myGraph = new HashMap<>();

  BaseCallGraph() {
  }

  public HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> getGraph() {
    return myGraph;
  }

  public void add(Set<BaseCallMatrix<T>> set) {
    for (BaseCallMatrix<T> cm : set) {
      append(cm, myGraph);
    }
  }

  protected abstract String getLabel(T vertex);

  protected abstract void formErrorMessage(T vertex, Set<RecursiveBehavior<T>> behavior);

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (T vDom : myGraph.keySet()) {
      for (T vCodom : myGraph.get(vDom).keySet()) {
        result.append(getLabel(vDom)).append(" -> ").append(getLabel(vCodom)).append("\n ");
        for (BaseCallMatrix<T> cm : myGraph.get(vDom).get(vCodom)) {
          result.append(cm.toString()).append("\n");
        }
      }
    }
    return result.toString();
  }

  public String toTestScenario(String edgeSetName) {
    StringBuilder result = new StringBuilder();
    for (T v : myGraph.keySet()) {
      String label = getLabel(v);
      String[] parameterLabels = null;
      for (T v2 : myGraph.get(v).keySet()) for (BaseCallMatrix<T> cm : myGraph.get(v).get(v2)) {
        parameterLabels = cm.getColumnLabels();
        break;
      }
      if (parameterLabels == null) throw new IllegalArgumentException();
      result.append("TestVertex ").append(label).append(" = new TestVertex(\"").append(label).append("\",");
      for (int i = 0; i < parameterLabels.length; i++) {
        result.append('"').append(parameterLabels[i]).append('"');
        if (i < parameterLabels.length-1) result.append(", ");
      }
      result.append(");\n");
    }

    Integer counter = 0;
    for (T v : myGraph.keySet()) {
      String domLabel = getLabel(v);
      for (T v2 : myGraph.get(v).keySet())
        for (BaseCallMatrix<T> edge : myGraph.get(v).get(v2)) {
          String codomLabel = getLabel(v2);
          result.append(edgeSetName).append(".add(new TestCallMatrix(\"e").append(counter).append("\", ").append(domLabel).append(", ").append(codomLabel).append(", ");
          result.append(edge.convertToTestCallMatrix());
          result.append("));\n");
          counter++;
      }
    }
    return result.toString();
  }

  private static <T> boolean append(BaseCallMatrix<T> cm, HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> graph) {
    HashSet<BaseCallMatrix<T>> set;
    HashMap<T, HashSet<BaseCallMatrix<T>>> map;
    if (!(graph.containsKey(cm.getDomain()))) {
      map = new HashMap<>();
      set = new HashSet<>();
      set.add(cm);
      map.put(cm.getCodomain(), set);
      graph.put(cm.getDomain(), map);
      return true;
    } else {
      map = graph.get(cm.getDomain());
      if (!(map.containsKey(cm.getCodomain()))) {
        set = new HashSet<>();
        set.add(cm);
        map.put(cm.getCodomain(), set);
        return true;
      } else {
        set = map.get(cm.getCodomain());
        boolean alreadyContainsSmaller = set.contains(cm);

        if (!alreadyContainsSmaller) for (BaseCallMatrix<T> arrow : set)
          if (arrow.compare(cm) != BaseCallMatrix.R.Unknown) {
            alreadyContainsSmaller = true;
            break;
          }

        if (!alreadyContainsSmaller) {
          HashSet<BaseCallMatrix<T>> toRemove = new HashSet<>();
          for (BaseCallMatrix<T> arrow : set)
            if (cm.compare(arrow) == BaseCallMatrix.R.LessThan)
              toRemove.add(arrow);
          set.removeAll(toRemove);
          set.add(cm);
        }

        return !alreadyContainsSmaller;
      }
    }
  }

  public boolean checkTermination() {
    HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> newGraph;
    HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> oldGraph = myGraph;
    int myNewEdges;
    boolean result = true;

    do {
      myNewEdges = 0;
      newGraph = new HashMap<>();

      for (T vDom : oldGraph.keySet()) {
        HashMap<T, HashSet<BaseCallMatrix<T>>> outboundArrows = oldGraph.get(vDom);
        for (T vCodom : outboundArrows.keySet()) {
          for (BaseCallMatrix<T> edge : outboundArrows.get(vCodom)) {
            append(edge, newGraph);
          }
        }
      }

      for (T vDom : oldGraph.keySet()) {
        HashMap<T, HashSet<BaseCallMatrix<T>>> outboundEdges = oldGraph.get(vDom);
        for (T vCodom : outboundEdges.keySet()) {
          for (BaseCallMatrix<T> arrow : outboundEdges.get(vCodom)) {
            HashMap<T, HashSet<BaseCallMatrix<T>>> outboundEdges2 = oldGraph.get(arrow.getCodomain());
            if (outboundEdges2 != null) {
              ComputationRunner.checkCanceled();
              for (HashSet<BaseCallMatrix<T>> homSet : outboundEdges2.values()) {
                for (BaseCallMatrix<T> arrow2 : homSet) {
                  if (append(new CompositeCallMatrix<>(arrow, arrow2), newGraph)) {
                    myNewEdges++;
                  }
                }
              }
            }
          }
        }
      }
      oldGraph = newGraph;

      for (T v : newGraph.keySet()) {
        RecursiveBehaviors<T> rbs = new RecursiveBehaviors<>(newGraph, v);
        List<String> order = rbs.findTerminationOrderAnnotated();
        if (order == null) {
          HashSet<RecursiveBehavior<T>> rbs2 = new HashSet<>();
          if (rbs.myBestRbAttained != null) {
            rbs2.addAll(rbs.myBestRbAttained.onlyMinimalElements());
          }
          formErrorMessage(v, rbs2);
          result = false;
        }
      }
    } while (myNewEdges > 0 && result);

    return result;
  }

  private static class RecursiveBehaviors<T> {
    private T myBasepoint = null;
    private final Set<RecursiveBehavior<T>> myBehaviors = new HashSet<>();
    private int myLength = -1;
    private RecursiveBehaviors<T> myBestRbAttained = null;

    private RecursiveBehaviors(HashMap<T, HashMap<T, HashSet<BaseCallMatrix<T>>>> graph, T v) {
      this(graph.get(v).get(v));
      myBasepoint = v;
    }

    private RecursiveBehaviors(Set<BaseCallMatrix<T>> callMatrices) {
      if (callMatrices != null)
        for (BaseCallMatrix<T> m : callMatrices) myBehaviors.add(new RecursiveBehavior<>(m));
      if (!myBehaviors.isEmpty()) {
        Iterator<RecursiveBehavior<T>> i = myBehaviors.iterator();
        myLength = i.next().getLength();
        while (i.hasNext()) if (myLength != i.next().getLength()) throw new IllegalArgumentException();
      }
    }

    private RecursiveBehaviors() {
    }

    private RecursiveBehaviors<T> createShorterBehavior(int i) {
      RecursiveBehaviors<T> result = new RecursiveBehaviors<>();
      for (RecursiveBehavior<T> rb : myBehaviors) {
        switch (rb.behavior.get(i)) {
          case LessThan:
            continue;
          case Equal:
            result.myBehaviors.add(new RecursiveBehavior<>(rb, i));
            continue;
          case Unknown:
            return null;
        }
      }
      result.myLength = myLength - 1;
      result.myBasepoint = myBasepoint;
      return result;
    }

    private List<Integer> findTerminationOrder(RecursiveBehaviors<T> recBehaviors, List<Integer> indices) {
      if (recBehaviors == null) throw new IllegalArgumentException();
      ComputationRunner.checkCanceled();

      if (recBehaviors.myBehaviors.isEmpty()) return indices;
      if (recBehaviors.myLength == 0) return null;

      if (myBestRbAttained == null || myBestRbAttained.myLength > recBehaviors.myLength)
        myBestRbAttained = recBehaviors;

      for (int i = 0; i < recBehaviors.myLength; i++) {
        RecursiveBehaviors<T> shorterBehavior = recBehaviors.createShorterBehavior(i);
        if (shorterBehavior != null) {
          List<Integer> shorterIndices = new LinkedList<>(indices);
          shorterIndices.remove(i);
          List<Integer> termOrder = findTerminationOrder(shorterBehavior, shorterIndices);
          if (termOrder != null) {
            termOrder.add(0, indices.get(i));
            return termOrder;
          }
        }
      }

      return null;
    }

    private List<Integer> findTerminationOrder() {
      List<Integer> indices = new LinkedList<>();
      for (int i = 0; i < myLength; i++) indices.add(i);
      return findTerminationOrder(this, indices);
    }

    private List<String> findTerminationOrderAnnotated() {
      List<Integer> to = findTerminationOrder();
      if (to == null) return null;
      if (!myBehaviors.isEmpty()) {
        RecursiveBehavior<T> rb = myBehaviors.iterator().next();
        List<String> result = new ArrayList<>();
        for (Integer i : to) result.add(rb.labels.get(i));
        return result;
      }
      return new ArrayList<>();
    }

    private Set<RecursiveBehavior<T>> onlyMinimalElements() {
      Set<RecursiveBehavior<T>> result = new HashSet<>();
      for (RecursiveBehavior<T> rb : myBehaviors) {
        boolean containsSmaller = false;
        Set<RecursiveBehavior<T>> greater = new HashSet<>();
        for (RecursiveBehavior<T> rb2 : result)
          if (rb2.leq(rb)) {
            containsSmaller = true;
            break;
          } else if (rb.leq(rb2)) {
            greater.add(rb2);
          }
        result.removeAll(greater);
        if (!containsSmaller) result.add(rb);
      }
      return result;
    }

  }

}
