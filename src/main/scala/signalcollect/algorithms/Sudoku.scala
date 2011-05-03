/*
 *  @author Daniel Strebel
 *  
 *  Copyright 2011 University of Zurich
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

package signalcollect.algorithms

import signalcollect.api._
import collection.mutable.{HashMap, SynchronizedMap}
import collection.mutable.ListMap
import signalcollect.interfaces.{ComputationStatistics, ComputeGraph}

/**
 * Represents all associated Sudoku cells that have to be taken into account to determine
 * the value of a cell
 *
 */
class SudokuAssociation(s: Any, t: Any) extends OptionalSignalEdge(s, t) {
  type SourceVertexType = SudokuCell
  def signal = source.state
}

/**
 * Cell in a Sudoku grid.
 *
 * @param id ID of the cell, where top left cell has id=0 top right has id of 8 and bottom right has id 80
 *
 */
class SudokuCell(id: Int, initialState: Option[Int] = None) extends SignalMapVertex(id, initialState) {

  type UpperSignalTypeBound = Int

  var possibleValues = SudokuHelper.legalNumbers
  if(initialState.isDefined) possibleValues=Set(initialState.get)

  def collect: Option[Int] = {

    //make a list of all possible values
    possibleValues = possibleValues -- mostRecentSignals.toSet

    //If own value is determined i.e. if only one possible value is left choose own value
    if (possibleValues.size == 1) {
      Some(possibleValues.head)
    }
    else
      state
  }
}

object Sudoku extends App {
    //Setting the values that are given, rest has default value 'None'

    //Very simple Sudoku
    val sudoku1 = Map(
      4 -> 9,
      5 -> 6,
      8 -> 5,
      10 -> 9,
      11 -> 4,
      13 -> 2,
      14 -> 1,
      15 -> 8,
      16 -> 6,
      19 -> 1,
      21 -> 4,
      24 -> 3,
      25 -> 2,
      29 -> 3,
      31 -> 4,
      34 -> 7,
      36 -> 1,
      38 -> 6,
      42 -> 4,
      44 -> 2,
      46 -> 4,
      49 -> 6,
      51 -> 5,
      55 -> 5,
      56 -> 2,
      59 -> 4,
      61 -> 1,
      64 -> 6,
      65 -> 1,
      66 -> 2,
      67 -> 3,
      69 -> 7,
      70 -> 8,
      72 -> 4,
      75 -> 8,
      76 -> 1)

    //bad-ass Sudoku Puzzle
    val sudoku2 = Map(
      0->9, 8->4,
      11->5, 13->3, 15->8, 16->9,
      21->6, 24 -> 2,
      28->9, 31->8, 33->3, 35->7,
      38->1, 42->4,
      45->7, 47->3, 49->2, 52->8,
      56->9, 59->6,
      64->7, 65->8, 67->5, 69->1,
      72->6, 80->3
    )


    //select a sudoku puzzle
    val initialSeed = sudoku2

    var cg = computeGraphFactory(initialSeed)

    //print initial Sudoku
    var seed = new HashMap[Int, Option[Int]]()
    cg.foreach { v => seed += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
    SudokuHelper.printSudoku(seed)

    val stats = cg.execute

    //If simple constraint propagation did not solve the problem apply a dept search algorithm to find a suitable solution
    if(!isDone(cg)) {
      cg = tryPossibilities(cg)
      if(cg == null) {
        println()
        println("Sorry this Sudoku is not solvable")
        sys.exit(5)
      }
    }
    println(stats)
    println()

    var result = new HashMap[Int, Option[Int]]() with SynchronizedMap[Int, Option[Int]]
    cg.foreach { v => result += Pair(v.id.asInstanceOf[Int], v.state.asInstanceOf[Option[Int]]) }
    cg.shutDown
    SudokuHelper.printSudoku(result)

  /**
   * Check if all cells have a value assigned to it
   */
  def isDone(cg: ComputeGraph): Boolean = {
    var isDone=true
    cg.foreach(v => if(v.state.asInstanceOf[Option[Int]] == None) isDone = false)
    isDone
  }


  /**
   * Recursive depth first search for possible values
   */
  def tryPossibilities(cg: ComputeGraph): ComputeGraph = {

    val possibleValues = new ListMap[Int, Set[Int]]()
    cg.foreach(v => possibleValues.put(v.id.asInstanceOf[Int], v.asInstanceOf[SudokuCell].possibleValues))
    cg.shutDown
    
    // Try different values for the cell with the highest probability for each possible value i.e. the one with
    // the smallest number of alternatives.
    def mostConstrainedCell: (Int, Set[Int]) = {
    	//Sort all cells that are not yet decided
    	val possibilities=possibleValues.toList.sortBy(_._2.size).filter(_._2.size>1) // Just selects the smallest set of possible values among the cells
    	if(possibilities.size == 0) {
    		println("No solution found")
    	      System.exit(-1)
    	}
    	possibilities.head
    }
    

    var solutionFound = false


    val candidate = mostConstrainedCell
    
    val iterator = candidate._2.iterator
    while(iterator.hasNext && !solutionFound) {
      var determinedValues = possibleValues.filter(_._2.size==1).map(x => (x._1, x._2.head)).toMap[Int, Int]
      determinedValues+=(candidate._1 -> iterator.next)
      var cgTry = computeGraphFactory(determinedValues)
      cgTry.execute
      if(isDone(cgTry)) {
        solutionFound = true
        return cgTry
      }
      else {
        val result = tryPossibilities(cgTry)
        if(result != null)
          return result //Search was successful
      }
    }
    null
  }

  def computeGraphFactory(seed: Map[Int, Int]): ComputeGraph = {
    var cg = new AsynchronousComputeGraph

    //Add all Cells for Sudoku
    for (index <- 0 to 80) {
      val seedValue = seed.get(index)
      cg.addVertex(classOf[SudokuCell], index, seedValue)
    }

    //Determine neighboring cells for each cell and draw the edges between them
    for (index <- 0 to 80) {
      SudokuHelper.cellsToConsider(index).foreach({ i =>
        cg.addEdge(classOf[SudokuAssociation], i, index)
      })
    }
    cg
  }

}

/**
 * Provides useful utilites for dealing with sudoku grids
 *
 */
object SudokuHelper {
  //All possible numbers for a cell
  val legalNumbers = {
    var numbers = (1 to 9).toSet
    println
    numbers
  }

  //Get Rows, Columns and Bocks from ID
  def getRow(id: Int) = id / 9
  def getColumn(id: Int) = id % 9
  def getBlock(id: Int) = getRow(id) * 3 + getColumn(id)

  /**
   * Returns all the neighboring cells that influence a cell's value
   */
  def cellsToConsider(id: Int): List[Int] = {
    var neighborhood = List[Int]()

    //Same row
    for (col <- 0 to 8) {
      val otherID = getRow(id) * 9 + col
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same column
    for (row <- 0 to 8) {
      val otherID = row * 9 + getColumn(id)
      if (otherID != id) {
        neighborhood = otherID :: neighborhood
      }
    }

    //Same block
    val topLeftRow = (getRow(id) / 3) * 3
    val topLeftColumn = (getColumn(id) / 3) * 3

    for (row <- topLeftRow to (topLeftRow + 2)) {
      for (column <- topLeftColumn to (topLeftColumn + 2)) {
        val otherID = row * 9 + column

        if (otherID != id && !neighborhood.contains(otherID)) {
          neighborhood = otherID :: neighborhood
        }
      }
    }

    neighborhood
  }

  /**
   * Formats the data in a classical sudoku layout
   */
  def printSudoku(data: HashMap[Int, Option[Int]]) = {

    println()
    println("Sudoku")
    println("======")
    println()
    println("=========================================")

    for (i <- 0 to 8) {
      val j = i * 9
      print("||")
      for (k <- j to j + 8) {
        data.get(k) match {
          case Some(Some(v)) => print(" " + v + " ")
          case v => print("   ") //Empty or Error
        }
        if (k % 3 == 2) {
          print("II")
        } else {
          print("|")
        }
      }
      println()
      if (i % 3 == 2) {
        println("=========================================")
      }
    }
  }
}