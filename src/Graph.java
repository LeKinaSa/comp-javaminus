
import java.util.*;

public class Graph<T> {
    private final Map<T, Set<T>> map = new HashMap<>();

    public Graph() {

    }

    public Graph(Graph<T> other) {
        for (T source : other.getVertices()) {
            addVertex(source);
            for (T destination : other.getEdges(source)) {
                addEdge(source, destination, false);
            }
        }
    }

    public void addVertex(T vertex) {
        map.put(vertex, new HashSet<>());
    }

    public void addEdge(T source, T destination, boolean bidirectional) {
        if (!map.containsKey(source)) addVertex(source);
        if (!map.containsKey(destination)) addVertex(destination);

        map.get(source).add(destination);
        if (bidirectional) {
            map.get(destination).add(source);
        }
    }

    public void removeVertex(T vertex) {
        map.remove(vertex);
        for (T other : map.keySet()) {
            removeEdge(other, vertex);
        }
    }

    public void removeEdge(T source, T destination) {
        if (map.containsKey(source)) {
            map.get(source).remove(destination);
        }
    }

    public Set<T> getVertices() {
        return map.keySet();
    }

    public Set<T> getEdges(T vertex) {
        return map.get(vertex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<T, Set<T>> entry : map.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return builder.toString();
    }
}
