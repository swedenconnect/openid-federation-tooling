/*
 * Copyright 2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package se.swedenconnect.oidf.tooling.oidftooling.tree;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a graph with nodes ant edges
 *
 * @author Per Fredre Plars
 */
@Getter
public class Graph {
  private final List<Node> nodes = new ArrayList<>();
  private final List<Edge> edges = new ArrayList<>();

  /**
   * Default constructor
   */
  public Graph() {
  }

  /**
   * Adds a node to the graph.
   *
   * @param node the node to be added to the graph
   */
  public void addNode(final Node node) {
    this.nodes.add(node);
  }

  /**
   * Adds an edge to the graph's edge list.
   *
   * @param edge the edge to be added to the graph
   */
  public void addEdge(final Edge edge) {
    this.edges.add(edge);
  }

  /**
   * Node
   */
  @Getter
  public static class Node {
    private final String id;
    private final String label;

    /**
     * Constructs a Node with the specified identifier and label.
     *
     * @param id the unique identifier of the node
     * @param label the descriptive label of the node
     */
    public Node(final String id, final String label) {
      this.id = id;
      this.label = label;
    }

  }

  /**
   * Edge
   */
  @Getter
  public static class Edge {
    private final String source;
    private final String target;

    /**
     * Creates an edge representing a connection between two nodes in a graph.
     *
     * @param source the identifier of the source node
     * @param target the identifier of the target node
     */
    public Edge(final String source, final String target) {
      this.source = source;
      this.target = target;
    }

  }
}
