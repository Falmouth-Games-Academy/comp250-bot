/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.RandomBiasedAI;
//import ai.RandomBiasedAI;
//import ai.abstraction.AbstractAction;
//import ai.abstraction.AbstractionLayerAI;
//import ai.abstraction.Harvest;
//import ai.abstraction.cRush.RangedAttack;
//import ai.abstraction.pathfinding.FloodFillPathFinding;
//import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;

import rts.GameState;
import rts.PhysicalGameState;
//import rts.Player;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
//import rts.units.Unit;
//import rts.units.UnitType;
import rts.units.UnitTypeTable;
import util.Pair;
//import rts.units.*;


/**
 *
 * @author Stomps
 */

class Node
{
	// C needs a lot of tweaking
    private float C = 0.05f;
    private Node m_Parent;// = null;
    private GameState m_GameState;
    private int m_CurrentTreeDepth;// = 0;
    
    private boolean m_HasUnexploredActions = true;
    private PlayerActionGenerator m_ActionGenerator = null;
    private PlayerAction m_Action = null;
    private GameState m_SimulatedGameState = null;
    private List<Node> m_ChildrenList = new ArrayList<>();
    private float m_EvaluationBound = 0;
    private double m_Score = 0;
    private int m_VisitCount = 0;
    private Map<Node, PlayerAction> m_ActionMap = new HashMap<Node, PlayerAction> ();
    private int actionsPruned = 0;
    private int actionsPassed = 0;

    List<PlayerAction> harvestAndAttackList = new ArrayList<>();
    List<PlayerAction> harvestList = new ArrayList<>();
    List<PlayerAction> attackList = new ArrayList<>();
    List<PlayerAction> backUpList = new ArrayList<>();
    
/*------------------------------------------------------------------------*/    
    
    public double getScore() { return m_Score; }
    public int getVisitCount() { return m_VisitCount; }
    public int getDepth() { return m_CurrentTreeDepth; }
    public boolean getHasUnexploredActions() { return m_HasUnexploredActions; }
    public Node getParent() { return m_Parent; }
    public GameState getGameState() { return m_GameState; }
    public List<Node> getChildrenList() { return m_ChildrenList; }
    public PlayerAction getActionFromChildNode(Node child) { return m_ActionMap.get(child); }
    
    public void incrementVisitCount() { m_VisitCount++; }
    public void addScore(double score) { m_Score += score; }
    public void addChild (Node child) { m_ChildrenList.add(child); }
    public void addToActionMap(Node node, PlayerAction playerAction) { m_ActionMap.put(node, playerAction); }
    
/*------------------------------------------------------------------------*/   
    
    // Constructor
    public Node(int maxPlayer, int minPlayer, Node parent, GameState gameState, float evaluationBound) throws Exception
    {
        m_Parent = parent;
        m_GameState = gameState;
        m_EvaluationBound = evaluationBound;
        
        // The node initialised with a null parent is the tree's root node with depth 0, otherwise it is the next depth layer down from the parent's depth
        if (m_Parent == null) m_CurrentTreeDepth = 0;
        else m_CurrentTreeDepth = m_Parent.m_CurrentTreeDepth + 1;
        
        // While there is no winner and the game is not over, cycle the gameState until a player can make a move
        while(m_GameState.winner() == -1 && 
              !m_GameState.gameover() &&
              !m_GameState.canExecuteAnyAction(maxPlayer) && 
              !m_GameState.canExecuteAnyAction(minPlayer)) m_GameState.cycle();
        
        // Check that the gameState and winner is still valid for playing on
        if (m_GameState.winner() == -1 || !m_GameState.gameover())
        {
        	// Initialise and randomise the PlayerActionGenerator for this node based on the player number
	        if (m_GameState.canExecuteAnyAction(maxPlayer))
	        {
	            m_ActionGenerator = new PlayerActionGenerator(gameState, maxPlayer);
	            m_ActionGenerator.randomizeOrder();
	        }
	        else if (m_GameState.canExecuteAnyAction(minPlayer))
	        {
	            m_ActionGenerator = new PlayerActionGenerator(gameState, minPlayer);
	            m_ActionGenerator.randomizeOrder();
	        }
        }
    }
    
    // Returns a new Node linked to a new unexplored player action in the m_ActionMap as the PlayerAction's key
    public Node selectNewAction(Tree tree, int playerNumber, long endTime, int maxTreeDepth) throws Exception
    {
        // Do a depth check. This AI will explore up to a predefined depth as the end of the game is often too far away
        if (m_CurrentTreeDepth >= maxTreeDepth) return this;
        
        // If this node has unexplored actions, get the next randomised action .
    	if (m_HasUnexploredActions)
        {
    		// If no more actions
            if (m_ActionGenerator == null) return this;
            
            // Move to the next (randomised order on initialisation) action available 
    		try
    		{
    			// Will eventually return null;
    			m_Action = m_ActionGenerator.getNextAction(endTime);
    		}
    		catch (Exception e)
    		{
    			m_Action = null;
    		}
		
    		// Check if last action that is available has been reached (next will be null)
    		if (m_Action != null)
            {
    			// leaf pruning
    			if ((m_CurrentTreeDepth == 0 && tree.actionSurvivesPruning(m_GameState, m_Action)) || m_CurrentTreeDepth > 0)
    			{
    				actionsPassed++;

        			m_SimulatedGameState = m_GameState.cloneIssue(m_Action);
	                
	    			// Constructor takes for new child takes 'this' as parent argument
	        		Node newChildNode = new Node(playerNumber, 1 - playerNumber, this, m_SimulatedGameState.clone(), m_EvaluationBound);
	                
	        		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
	    			m_ActionMap.put(newChildNode, m_Action);
	        		
	    			// Add to children list. This is later cycled through to find the best child of a node
	                tree.getRoot().addChild(newChildNode);
	                
	                // Add to the list of Nodes worth exploring more
	                tree.addNodeToExplore(newChildNode);
	                
	                return newChildNode;       
    			}
    			else
    			{
  //  				System.out.println("Action did not survive pruning.		Tree potentials size = " + tree.getNumberOfNodesToExplore());
    				actionsPruned++;
    				
					return null;//selectNewAction(tree, playerNumber, endTime, maxTreeDepth);
    			}
            }
            else
            {
            	// Stop future iterations from trying to explore new actions from this node
//            	System.out.println("No more actions for this Node. Explored: Pruned: " + actionsPruned + " passed: " + actionsPassed);
                m_HasUnexploredActions = false;
                
                tree.removeNodeToExplore(this);
                return null;
//                Deque foo;//14th may 12:20
            }
        }
    	return null;
    }  

    
    public double UCTScore(Node node, int totalNodeVisits)
    {
    	// Tweak the constant. Dynamic? How...
    	C = 1.0f;
    	
    	// Trying to prioritise non visited Nodes
    	if (node.getVisitCount() <= 1)
    	{
    		return 1;
    	}
    	
    	return node.getScore()/node.getVisitCount() + C * Math.sqrt(2 * Math.log(totalNodeVisits)/node.getVisitCount());
    }
}

// Contains information about the tree necessary for node pruning etc
class Tree
{
	private Node m_Root;
	private GameState m_GameState;
	private int m_PlayerNumber;
	private UnitType m_BaseType;
	private UnitType m_WorkerType;
	private int m_WorkerCount = 0; 
	private int m_AttackUnitsCount = 0;
//    private List<Pair<Integer, Integer>> m_ResourceLocationList = new ArrayList<>();
    private List<Unit> m_ResourceUnitList = new ArrayList<>();
    private List<Unit> m_EnemyList = new ArrayList<>();
    
    private List<Node> m_NodesToExplore = new ArrayList<>();
	
	private boolean m_BaseIsAvailable = false;
	private boolean m_ResourceIsAvailable = false;
	private int m_HalfMapDistance;
	
	private int m_PlayerBaseLocationX;
	private int m_PlayerBaseLocationY;
	
    private int m_AttackDistance = 20;
    
    public Node getRoot() { return m_Root; }
    public void addNodeToExplore(Node node) { m_NodesToExplore.add(node); }
    public void removeNodeToExplore(Node node) { m_NodesToExplore.remove(node); }
    public int getNumberOfNodesToExplore() { return m_NodesToExplore.size(); }

/*------------------------------------------------------------------------*/   
    
    public Tree(int playerNumber, GameState gameState, int halfMapDistance, float evaluationBound, UnitType baseType, UnitType workerType) throws Exception
	{
		m_GameState = gameState;//.clone();
		m_Root = new Node(playerNumber, 1-playerNumber, null, m_GameState, evaluationBound);
		m_HalfMapDistance = halfMapDistance;
		m_PlayerNumber = playerNumber;
		m_BaseType = baseType;
		m_WorkerType = workerType;
		
		m_NodesToExplore.add(m_Root);
		
        analyseGameState();
	}
	
	void analyseGameState()
	{
		// Spend some loops getting the player's base locations and all resource locations
		
		// Eventually only need to get locations once, can check if they are still available at the start of each getAction()
    	for (Unit unit : m_GameState.getUnits())
    	{
    		if(!m_BaseIsAvailable)
    		{
	    		if (unit.getPlayer() == m_PlayerNumber && unit.getType() == m_BaseType)
	    		{
	    			// Get base location
	    			m_PlayerBaseLocationX = unit.getX();
	    			m_PlayerBaseLocationY = unit.getY();
	    			m_BaseIsAvailable = true;
	    		}
    		}
    		if (unit.getPlayer() == 1-m_PlayerNumber)
    		{
    			// Set something to do with enemy locations, add to list maybe, ...
    			m_EnemyList.add(unit);
    		}
    		if (unit.getPlayer() == m_PlayerNumber && unit.getType() == m_WorkerType)
    		{
    			m_WorkerCount++;
    		}
    	}
    	
    	for (Unit unit : m_GameState.getUnits())
    	{
    		// If base is available then check for harvesting actions
    		if (m_BaseIsAvailable)
    		{
    			// Do a distance check on resource carrying units to just find ones near to the base
    			if (unit.getType().isResource)// .getResources() > 0)
    			{
    				if (Math.abs(unit.getX() - m_PlayerBaseLocationX) + Math.abs(unit.getY() - m_PlayerBaseLocationY) < m_HalfMapDistance + 1)
    				{
	    				m_ResourceUnitList.add(unit);
	        			m_ResourceIsAvailable = true;
    				}
    			}
    		}
    	}
	}
	
    public boolean actionSurvivesPruning(GameState gameState, PlayerAction potentialAction)
    {
    	//GameState tempGameState = gameState.clone();
    	GameState simulatedGameState = gameState.cloneIssue(potentialAction);
    	//m_WorkerCount = 0;
        
		// For opening moves. The only action is to create more workers so all good
		// Maybe control the direction in which it is producing workers?
		if (m_WorkerCount == 0)
		{
//			System.out.println("THIS SHOULD NOT HAPPEN AFTER FIRST MOVE");
			return true;
		}
    	
    	// If harvesting is on the cards
    	if (m_ResourceIsAvailable && m_BaseIsAvailable)// && m_GameState.getTime() < 900)
    	{
    		// Prune for harvesting actions
			for (Unit unit : simulatedGameState.getUnits())
			{
				// Sanity check for nulls
				if (potentialAction != null && potentialAction.getAction(unit) != null)
				{
					// Look at our player
					if (unit.getPlayer() == m_PlayerNumber)
					{
						// Look at the worker units first
						if (unit.getType() == m_WorkerType)
						{
							//m_WorkerCount++;
							
							// Leaf pruning for harvesting or returning actions in this playerAction
							if (potentialAction.getAction(unit).getType() == UnitAction.TYPE_HARVEST || potentialAction.getAction(unit).getType() == UnitAction.TYPE_RETURN)
							{
								return true;
							}
							// Otherwise check for an en route worker
							else if (potentialAction.getAction(unit).getType() == UnitAction.TYPE_MOVE)
							{
								// If it's packing resources
								if (unit.getResources() > 0)
								{
									// Check if moving back towards the base
									if (m_PlayerBaseLocationX- unit.getX() < 0 && potentialAction.getAction(unit).getDirection() == UnitAction.DIRECTION_LEFT) { return true; }//thisMoveCanHarvest = true;
									if (m_PlayerBaseLocationX- unit.getX() > 0 && potentialAction.getAction(unit).getDirection() == UnitAction.DIRECTION_RIGHT) { return true; }//thisMoveCanHarvest = true;
									if (m_PlayerBaseLocationY- unit.getY() < 0 && potentialAction.getAction(unit).getDirection() == UnitAction.DIRECTION_UP) { return true; }//thisMoveCanHarvest = true;
									if (m_PlayerBaseLocationY- unit.getY() < 0 && potentialAction.getAction(unit).getDirection() == UnitAction.DIRECTION_DOWN) { return true; }//thisMoveCanHarvest = true;
									
									
								}
								// else if it's looking for resources
								else
								{
									for (Unit resourceUnit : m_ResourceUnitList)
				    				{
										int ua = potentialAction.getAction(unit).getDirection();
										
										// If it needs to go left etc
										if (resourceUnit.getX() - unit.getX() < 0 && ua == 3/*UnitAction.DIRECTION_LEFT*/) { return true; }//thisMoveCanHarvest = true;
										if (resourceUnit.getX() - unit.getX() > 0 && ua == 1/*UnitAction.DIRECTION_RIGHT*/) { return true; }//thisMoveCanHarvest = true;
										if (resourceUnit.getY() - unit.getY() < 0 && ua == 0/*UnitAction.DIRECTION_UP*/) { return true; }//thisMoveCanHarvest = true;
										if (resourceUnit.getY() - unit.getY() > 0 && ua == 2/*UnitAction.DIRECTION_DOWN*/) { return true; }//thisMoveCanHarvest = true;
									}
								}
							}
						}
					}
				}
			}
			return false;
    	}
//    	else
    	// Attack the enemy! Check for attack moves first, 
    	{
    		m_AttackUnitsCount = 0;
    		
    		// Prune for attacking playerActions
			for (Unit unit : simulatedGameState.getUnits())
			{
				// Sanity checks for null pointers
				if (potentialAction != null && potentialAction.getAction(unit) != null)
				{
					// Look at our player
					if (unit.getPlayer() == m_PlayerNumber)
					{
						// Check for direct attacking
						if (potentialAction.getAction(unit).getType() == UnitAction.TYPE_ATTACK_LOCATION)
						{
							return true;
						}
						else
						{
							// Check for en route attackers
							// Check against closest distance check set beforehand
							int tempDistance;
							
							for (Unit enemyUnit : m_EnemyList)
							{
								tempDistance = Math.abs(unit.getX() - enemyUnit.getX()) + Math.abs(unit.getY() - enemyUnit.getY());
								
								if (tempDistance < m_AttackDistance)
								{
									int ua = potentialAction.getAction(unit).getDirection();
									
									if (enemyUnit.getX() - unit.getX() < 0 && ua == 3/* UnitAction.DIRECTION_LEFT*/) { m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }//return true;//thisMoveCanAttack = true;
									if (enemyUnit.getX() - unit.getX() > 0 && ua == 1/* UnitAction.DIRECTION_RIGHT*/) { m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }//return true;//thisMoveCanAttack = true;
									if (enemyUnit.getY() - unit.getY() < 0 && ua == 0/* UnitAction.DIRECTION_UP*/) { m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }//return true;//thisMoveCanAttack = true;
									if (enemyUnit.getY() - unit.getY() > 0 && ua == 2/* UnitAction.DIRECTION_DOWN*/) { m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }//return true;//thisMoveCanAttack = true;
								}
							}
						}
					}
				}
			}
    	}
    	// Not a harvesting or attacking move
		return false;
    }

    public Node findNewNodeWithBestUCTScore(int totalNodeVisits)
    {
        // Temporary variables
        Node tempBestNode = null;
        double tempBestScore = -999;
        
        // Find the Node discovered so far with the best UCT score
        for (Node potentialNode : m_NodesToExplore)// m_Root.getChildrenList())
        {
            double childNodeScore = potentialNode.UCTScore(potentialNode, totalNodeVisits);
            if (potentialNode.getHasUnexploredActions())
            {
	            if (tempBestNode == null || childNodeScore > tempBestScore) //|| (childNodeScore/(double)potentialNode.getVisitCount() > tempBestNode.getScore()/(double)tempBestNode.getVisitCount())) // 
	            {
	            //	return childNode;
	                tempBestNode = potentialNode;
	                tempBestScore = childNodeScore;
	            }
            }
        } 
        
        // Sanity check, or if no children
        if (tempBestNode == null)
        {
//        	System.out.println("There are no children in the tree...???");
        	return null;//m_Root;
       	}
        
        // Explore that child for new unexplored PlayerActions
        return tempBestNode;//.selectNewAction(maxPlayer, minPlayer, endTime, maxTreeDepth, totalNodeVisits, tree);
    }  
}


// The AI class
public class Velociraptor extends AI
{
	// C needs a lot of tweaking
    float C = 0.05f;
	
	// Game evaluation function that returns a value based on units and resources available
    EvaluationFunction EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
    
    // Simulations require an opponent to play out against, RandomBiasedAI is a slightly stronger opponent than RandomAI, Or maybe choose stronger?
    AI simulationEnemyAI = new RandomBiasedAI();
    
    GameState initialGameState;
    
    Tree tree;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 100;
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH;
    
    int totalNodeVisits = 0;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    
    // for epsilon greedy?
    Random random = new Random();
    
    int rushCountdownToMCTS = 5;
    float evaluationBound = 1;
    long endTime;
    PhysicalGameState physicalGameState;
    int DEBUG_TOTAL_NODES_EXAMINED = 0;
    
    UnitType baseType;
    UnitType workerType;
    
    // For finding the near side resources
    int halfMapDistance;
    
    public Velociraptor(UnitTypeTable utt) {
    	unitTypeTable = utt;

        baseType = utt.getUnitType("Base");
        workerType = utt.getUnitType("Worker");
    }      
    
    
    public Velociraptor() {
    }
    
    
    public void reset() {
        initialGameState = null;
        tree = null;
        rushCountdownToMCTS = 5;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
        DEBUG_TOTAL_NODES_EXAMINED = 0;
    }
    
    
    public void resetSearch() {
        tree = null;
        initialGameState = null;
        rushCountdownToMCTS = 5;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
        DEBUG_TOTAL_NODES_EXAMINED = 0;
    }
    
    
    public AI clone() {
        return new Velociraptor();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
    	totalNodeVisits = 0;
        rushCountdownToMCTS = 5;
    	
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Used to estimate the look ahead max tree depth heuristic
        physicalGameState = gameState.getPhysicalGameState();
/*        
        // Rush on larger maps
        if (physicalGameState.getWidth() > 11 && rushCountdownToMCTS != 0) 
        	{
        		rushCountdownToMCTS--;
        		return new Brontosaurus(unitTypeTable).getSimulatedAction(player, gameState);
        	}
*/        
        
        // Epsilon greedy?
        if (random.nextFloat() < 0.07f) return new PlayerActionGenerator(gameState, player).getRandom();
        
        // Simulate against the best heuristic quick time algorithm possible / available
//        simulationEnemyAI = new Brontosaurus(unitTypeTable);
        
        
        // Cartesian derived heuristic for a lookahead amount, halfway plus a bit
        MAX_TREE_DEPTH = 15;// (physicalGameState.getWidth() * 2);// + physicalGameState.getHeight());///2 + 2;
        
        // This just returns 1 as far as I can tell
//        float evaluation_bound = EVALUATION_FUNCTION.upperBound(gameState);
        
        playerNumber = player;
        initialGameState = gameState;//.clone();
        
        // Initialise the tree
        tree = new Tree(playerNumber, gameState, halfMapDistance, evaluationBound, baseType, workerType);
        
        // Time stuff can be done better
        endTime = System.currentTimeMillis() + MAXSIMULATIONTIME;

        DEBUG_TOTAL_NODES_EXAMINED = 0;
        
        Node nodeToExplore = tree.getRoot();
        
        halfMapDistance = (physicalGameState.getWidth() + physicalGameState.getHeight()) / 2 + 1;
        
        // Main loop
        while (true)
        {

            //System.out.println("stRTING LOOP");
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) { /*System.out.println("Outta time");*/ break; }
/*
 * 
 * 1. Call selectNewAction() function on the current nodeToExplore to get a new Node, linked to the PlayerAction in the tree node's action dictionary, and stored in the tree node's children list.
 *             
 */
            nodeToExplore = tree.findNewNodeWithBestUCTScore(totalNodeVisits);
            
        	// Tries to get a new unexplored action from the tree
            Node newNode = nodeToExplore.selectNewAction(tree, playerNumber, endTime, MAX_TREE_DEPTH);
    		
            // If no new actions then null is returned
            if (newNode != null)
            {
 //           	System.out.println("Good action found");
            	DEBUG_TOTAL_NODES_EXAMINED++;
            	
            	totalNodeVisits++;
            	
            	// Clone the gameState for use in the simulation
                GameState gameStateClone = newNode.getGameState().clone();
                
                // Simulate a play out of that gameState
                //simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
                double evaluation = NSimulate(gameStateClone, player, 10);
                
                // Not too sure here, decay, the evaluation tends towards zero as the time increases
                int time = gameStateClone.getTime() - initialGameState.getTime();
                
                //double evaluation = EVALUATION_FUNCTION.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);//EvalFunction
                
//                System.out.println("Node Evaluation score: " + evaluation);
                
                // Back propagation, cycle though each node's parents until the tree root is reached
                boolean printOnce = false;//true;
                
                while(newNode != null)
                {
                	if (printOnce)
                	{
                		System.out.println("			Node Depth is: " + newNode.getDepth());
                		System.out.println("");
                		printOnce = false;
                	}
                    newNode.addScore(evaluation);
                    newNode.incrementVisitCount();
                    newNode = newNode.getParent();
                }
            }
      //      else
        //    	System.out.println("No good action found");
            
            nodeToExplore = tree.findNewNodeWithBestUCTScore(totalNodeVisits);
            
            if (nodeToExplore == null)
            {
//---------------------------------------------HERE        
   //         	System.out.println("FUUUUUUUUUUU");
            	return simulationEnemyAI.getAction(player, gameState);//break;
            }
            
        }
        
        // Sanity check
        if (tree.getRoot().getChildrenList() == null) return simulationEnemyAI.getAction(player, gameState);// new PlayerAction();
        
        // Temporary variable
        Node tempMostVisited = null;
        
        for (Node child : tree.getRoot().getChildrenList())
        {
        	// if no other value has been assigned then assign child
            if (tempMostVisited == null ||
            		// or if child is better than temp variable then replace
            		child.getVisitCount() > tempMostVisited.getVisitCount() ||
            		// or if visits are the same but child's score is better then replace
            		(child.getVisitCount() == tempMostVisited.getVisitCount() && child.getScore() > tempMostVisited.getScore()))
            {
            	// Update temporary variable
                tempMostVisited = child;
            }
        }
        
        // Sanity check
        if (tempMostVisited == null)
        {
//        	System.out.println("FUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU");
        	return simulationEnemyAI.getAction(player, gameState);// new PlayerAction();
       	}
/*
        System.out.println("    Tree children size: 	" + tree.getNumberOfNodesToExplore());
        System.out.println("    Total nodes examined: 	" + DEBUG_TOTAL_NODES_EXAMINED);
        System.out.println("    Total Nodes explored: 	" + totalNodeVisits);
        System.out.println("    Root Node visits: 		" + tree.getRoot().getVisitCount());
*/        
        // m_ActionMap getter
        return tree.getRoot().getActionFromChildNode(tempMostVisited);
    }
    

/*    
 * ---------------------------------
 * This could be a better simulation environment? Averaging N play outs.
 * ---------------------------------
 *     
*/
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float NSimulate(GameState gameStateClone, int player, int N) throws Exception
    {
        float accum = 0;
        for(int i = 0; i < N; i++)
        {
            GameState thisNGS = gameStateClone.clone();
            simulate(thisNGS,thisNGS.getTime() + MAXSIMULATIONTIME);
            int time = thisNGS.getTime() - gameStateClone.getTime();
            // Discount factor:
            accum += (float)(EVALUATION_FUNCTION.evaluate(player, 1-player, thisNGS)*Math.pow(0.99,time/10.0));
        }
            
        return accum/N;
    }    
    
    
    public void simulate(GameState gameState, int time) throws Exception
    {
        boolean gameover = false;

        do
        {
            if (gameState.isComplete())
            {
                gameover = gameState.cycle();
            }
            else
            {
                gameState.issue(simulationEnemyAI.getAction(0, gameState));
                gameState.issue(simulationEnemyAI.getAction(1, gameState));
            }
        }while(!gameover && gameState.getTime() < time);   
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}


