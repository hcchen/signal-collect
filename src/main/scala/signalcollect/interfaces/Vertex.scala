/*
 *  @author Philip Stutz
 *  
 *  Copyright 2010 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package signalcollect.interfaces

/**
 * This class represents the framework's view of a vertex.
 *
 * This trait requires to always be extended by something that implements {@link Vertex}
 * which gives us access to methods and fields in {@link Vertex}.
 */
 trait Vertex[IdType, StateType] extends MessageRecipient[Signal[_, _, _]] with Comparable[Vertex[_, _]] {

  def compareTo(other: Vertex[_, _]): Int = {
	  scoreCollect.compareTo(other.scoreCollect)
  }
	
  /**
   * By default it is assumed that a vertex can receive signals of any type.
   * This type can be overridden to set an upper bound for the types of signals
   * that this vertex can receive. This occasionally allows for better  allows for more elegant implementations.
   */
  type UpperSignalTypeBound
	
  /**
   * Vertices are assigned to worker threads that are each responsible for a part of the graph.
   * We use a hash function on the vertex ids for the mapping of vertices to workers.
   */
  override val hashCode = this.id.hashCode

  override def equals(other: Any): Boolean = {
	  other match {
	 	  case v: Vertex[_, _] => v.id == id
	 	  case _ => false
	  }
  }
 
  /** @return the identifier of this {@link FrameworkVertex}. */
  val id: IdType

  /** @return the object that stores the current state for this {@link Vertex}. */
  var state: StateType

  /**
   * The abstract "collect" function is algorithm specific and has to be implemented by a user of the API
   * this function will be called during algorithm execution. It is meant to calculate a new vertex state
   * based on the {@link Signal}s received by this vertex.
   */
  def collect: StateType

  /** Setter for {@link #_messageBus} over which this vertex is communicating with its outgoing edges. */
  def setMessageBus(mb: MessageBus[Any, Any])

  /**
   * Adds a new outgoing {@link Edge} to this {@link Vertex}.
   * @param e the edge to be added.
   */
  def addOutgoingEdge(e: Edge[_, _])

  /**
   * Removes an outgoing {@link Edge} from this {@link Vertex}.
   * @param e the edge to be added..
   * @return returns if an edge was removed
   */
  def removeOutgoingEdge(edgeId: (Any, Any, String)): Boolean

  /**
   * Removes all outgoing {@link Edge}s from this {@link Vertex}.
   */
  def removeAllOutgoingEdges
  
  /**
   * Informs this vertex that there is a new incoming edge.
   * @param edgeId the id of the new incoming edge
   */
  def addIncomingEdge(edgeId: (Any, Any, String))

  /**
   * Informs this vertex that an incoming edge was removed.
   * @param edgeId the id of the incoming edge that was removed
   * @return Optionally returns if an edge was removed
   */
  def removeIncomingEdge(edgeId: (Any, Any, String)): Option[Boolean]
   
  /**
   * This method tells this {@link FrameworkVertex} to execute the signal operation
   * on all its outgoing {@Edge}s. This method is going to be
   * called by the SignalCollect framework during its execution (i.e. the
   * {@link Worker} implementations).
   *
   * @see Worker
   * @see Edge#executeSignalOperation
   */
  def executeSignalOperation

  /**
   * Executes the {@link #collect} method on this vertex.
   * @see #collect
   */
  def executeCollectOperation

  /**
   * This method is used by the framework in order to decide if the vertex' collect operation
   * should be executed.
   *
   * @return the score value. The meaning of this value depends on the thresholds set in the framework.
   */
  def scoreCollect: Double

  /**
   * This method is used by the framework in order to decide if the vertex' signal operation should be executed.
   * The higher the returned value the more likely the vertex will be scheduled for executing its signal method.
   * @return the score value. The meaning of this value depends on the thresholds set in {@link ComputeGraph#execute}.
   */
  def scoreSignal: Double

  /** @return optionally the number of outgoing edges of this {@link Vertex} */
  def outgoingEdgeCount: Int
  
  /** This method gets called by the framework after the vertex has been fully initialized. */
  def afterInitialization

}