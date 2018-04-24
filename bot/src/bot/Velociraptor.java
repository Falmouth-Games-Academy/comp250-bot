/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
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
import rts.ResourceUsage;
import rts.UnitAction;
import rts.UnitActionAssignment;
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
    private List<Pair<PlayerAction, Float>> m_OrderedActionList;

    List<PlayerAction> harvestAndAttackList = new ArrayList<>();
    List<PlayerAction> harvestList = new ArrayList<>();
    List<PlayerAction> attackList = new ArrayList<>();
    List<PlayerAction> backUpList = new ArrayList<>();
    
    int m_CurrentActionIndex = 0;
    int m_MaxActionIndex;
    int m_MaxAmountOfNodeActionsToExamine = 15;
    
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
    public Node(Tree tree, int maxPlayer, int minPlayer, Node parent, GameState gameState, float evaluationBound, long endTime) throws Exception
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
	            m_OrderedActionList = tree.AnalyseAndSortActionSpace(m_ActionGenerator, m_GameState, endTime);
	        }
	        else if (m_GameState.canExecuteAnyAction(minPlayer))
	        {
	            m_ActionGenerator = new PlayerActionGenerator(gameState, minPlayer);
	            m_ActionGenerator.randomizeOrder();
	            m_OrderedActionList = tree.AnalyseAndSortActionSpace(m_ActionGenerator, m_GameState, endTime);
	        }
	            
            if (m_OrderedActionList.size() > m_MaxAmountOfNodeActionsToExamine) m_MaxActionIndex = m_MaxAmountOfNodeActionsToExamine;
            else m_MaxActionIndex = m_OrderedActionList.size();
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
            
            if (m_CurrentActionIndex < m_MaxActionIndex)
            {
	            m_Action = m_OrderedActionList.get(m_CurrentActionIndex).m_a;
	            
	            m_CurrentActionIndex++;
	            
	            if (m_Action != null)
	            {
        			m_SimulatedGameState = m_GameState.cloneIssue(m_Action);
	                
	    			// Constructor takes for new child takes 'this' as parent argument
	        		Node newChildNode = new Node(tree, playerNumber, 1 - playerNumber, this, m_SimulatedGameState.clone(), m_EvaluationBound, endTime);
	                
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
					return null;
    			}
            }
            else
            {
            	// Stop future iterations from trying to explore new actions from this node
                m_HasUnexploredActions = false;
                
                // This list kind of does the same thing as the unexplored actions bool
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
    	C = 0.05f;
    	
    	return node.getScore()/node.getVisitCount() + C * Math.sqrt(2 * Math.log(totalNodeVisits)/node.getVisitCount());
    }
}

// Contains information about the tree necessary for node pruning etc
class Tree
{
	private Node m_Root;
	private GameState m_GameState = null;
	
	private int m_PlayerNumber;
	private UnitType m_WorkerType;
	private UnitType m_BaseType;
	private Unit m_Base = null;
	
	private int m_FriendlyCount;
	private int m_HalfMapDistance;
	
    private List<Unit> m_ResourceUnitList = new ArrayList<>();
    private List<Unit> m_EnemyList = new ArrayList<>();
    
    private List<Node> m_NodesToExplore = new ArrayList<>();
	
	private float m_HarvestWeight;
	private float m_MoveToHarvestWeight;
	private float m_AttackWeight;
	private float m_MoveToAttackWeight;
	private float m_ProduceWeight;
    private int m_AttackDistance;
	
    public Node getRoot() { return m_Root; }
    public void addNodeToExplore(Node node) { m_NodesToExplore.add(node); }
    public void removeNodeToExplore(Node node) { m_NodesToExplore.remove(node); }
    public int getNumberOfNodesToExplore() { return m_NodesToExplore.size(); }
    public int getPlayerUnitDifference() { return m_FriendlyCount - m_EnemyList.size(); }
    public int getEnemyListSize() { return m_EnemyList.size(); }

/*------------------------------------------------------------------------*/   
    
    public Tree(int halfMapDistance, UnitType baseType, UnitType workerType) throws Exception
	{
//		m_Root = new Node(this, playerNumber, 1-playerNumber, null, m_GameState, evaluationBound, endTime);
		m_HalfMapDistance = halfMapDistance;
		m_BaseType = baseType;
		m_WorkerType = workerType;
	}
    
    public void initRoot(int playerNumber, GameState gameState, float evaluationBound, long endTime) throws Exception
    {
		m_Root = new Node(this, playerNumber, 1-playerNumber, null, gameState, evaluationBound, endTime);
		m_NodesToExplore.add(m_Root);
		m_GameState = gameState;
		m_PlayerNumber = playerNumber;
    }
    
    public void setAnalysisWeightings(float harvestWeight, float moveToHarvestWeight, float attackWeight, float produceWeight, float moveToAttackWeight, int attackDistance)
    {
    	m_HarvestWeight = harvestWeight;
    	m_MoveToHarvestWeight = moveToHarvestWeight;
    	m_AttackWeight = attackWeight;
    	m_MoveToAttackWeight = moveToAttackWeight;
    	m_ProduceWeight = produceWeight;
    	m_AttackDistance = attackDistance;
    }
	
	void analyseGameState()
	{
		m_FriendlyCount = 0;
		// Spend some loops getting the player's base locations and all resource locations
		
		// Eventually only need to get locations once? Can check if they are still available at the start of each getAction()
    	for (Unit unit : m_GameState.getUnits())
    	{
    		if (unit.getPlayer() == m_PlayerNumber)
    		{
    			m_FriendlyCount++;
    			
    			if (unit.getType() == m_BaseType)
	    		{
	    			if (m_Base == null)
	    			{
	    				m_Base = unit;
	    			}
	    		}
    		}
    		if (unit.getPlayer() == 1-m_PlayerNumber)
    		{
    			// Set something to do with enemy locations, add to list maybe, ...
    			m_EnemyList.add(unit);
    		}
    	}
    	if (m_Base != null)
    	{
    		// If base is available then check for harvesting actions
	    	for (Unit unit : m_GameState.getUnits())
	    	{
    			// Do a distance check on resource carrying units to just find ones near to the base
    			if (unit.getType().isResource)
    			{
    				if (Math.abs(unit.getX() - m_Base.getX()) + Math.abs(unit.getY() - m_Base.getY()) < m_HalfMapDistance)
    				{
	    				m_ResourceUnitList.add(unit);
    				}
    			}
    		}
    	}
	}
	
    public List<Pair<PlayerAction, Float>> AnalyseAndSortActionSpace(PlayerActionGenerator actionGenerator, GameState gameState, long cutOffTime) throws Exception
    {
    	List<Pair<PlayerAction, Float>> actionList = new ArrayList<Pair<PlayerAction, Float>>();
    	PlayerAction playerAction = null;
    	
    	while (true)
    	{
    		// Go through all the playerActions generated on construction
    		playerAction = actionGenerator.getNextAction(cutOffTime);
    		
    		if (playerAction != null)
	    	{
    			// Simulate the action into a cloned gameState
	    		GameState simulatedGameState = gameState.cloneIssue(playerAction);
	    		
	    		// Store as a new Pair in the actionList
	    		actionList.add(new Pair<>(playerAction, analyseAction(simulatedGameState, playerAction)));
    		}
    		// if null then no more actions to process
    		else break;
    	}
    	
    	if (actionList.size() > 0)
    	{
	    	Collections.sort(actionList, new Comparator<Pair<PlayerAction, Float>>()
	    			{
	    				@Override
	    				public int compare(final Pair<PlayerAction, Float> o1, final Pair<PlayerAction, Float> o2)
	    				{
	    					if (o1.m_b > o2.m_b) return -1;
	    		            else if (o1.m_b.equals(o2.m_b)) return 0;//(o1.m_b == o2.m_b) return 0; //
	    		            else return 1;
	    				}
	    			});
    	}
    	
    	actionList.add(new Pair<>(null, 0.0f));
    	
		return actionList;
    }
	
    public float analyseAction(GameState gameState, PlayerAction potentialAction)
    {
    	GameState simulatedGameState = gameState.cloneIssue(potentialAction);
    	
		float actionScore = 0.0f;
        
		// For opening moves. The only action is to create more workers so all good
		// Maybe control the direction in which it is producing workers?
		if (potentialAction == null || m_FriendlyCount == 0)
		{
//			System.out.println("THIS SHOULD NOT HAPPEN AFTER FIRST MOVE");
			return actionScore;
		}
		
		// Look at every Unit
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
						// If there is a base then check for harvesting actions
						if (m_Base != null)
						{
							// If its worth checking this
							if (m_HarvestWeight > 0)
							{
								// Check for harvesting or returning actions in this playerAction
								if (potentialAction.getAction(unit).getType() == 2 /*UnitAction.TYPE_HARVEST*/ || potentialAction.getAction(unit).getType() == 3 /*UnitAction.TYPE_RETURN*/)
								{
									actionScore += m_HarvestWeight;
								}
							}
							else if (m_MoveToHarvestWeight > 0)
							{
								if (potentialAction.getAction(unit).getType() == UnitAction.TYPE_MOVE)
								{
									int directionIdentifier = potentialAction.getAction(unit).getDirection();
									
									// If it's packing resources
									if (unit.getResources() > 0)
									{
										// Check if moving back towards the base
										if ((m_Base.getX() - unit.getX() < 0 && directionIdentifier == 3)
												|| (m_Base.getX() - unit.getX() > 0 && directionIdentifier == 1)
												|| (m_Base.getY() - unit.getY() < 0 && directionIdentifier == 0) 
												|| (m_Base.getY() - unit.getY() < 0 && directionIdentifier == 2)) actionScore += m_MoveToHarvestWeight;
									}
									// else if it's looking for resources
									else
									{
										int tempDistance;
										
										for (Unit resourceUnit : m_ResourceUnitList)
					    				{
											tempDistance = Math.abs(unit.getX() - resourceUnit.getX()) + Math.abs(unit.getY() - resourceUnit.getY());
											
											// Only add to score if a close worker is moving towards resource
											if (tempDistance < 8)
											{
												// If it needs to go left etc
												if ((resourceUnit.getX() - unit.getX() < 0 && directionIdentifier == 3/*UnitAction.DIRECTION_LEFT*/)
													|| (resourceUnit.getX() - unit.getX() > 0 && directionIdentifier == 1/*UnitAction.DIRECTION_RIGHT*/)
													|| (resourceUnit.getY() - unit.getY() < 0 && directionIdentifier == 0/*UnitAction.DIRECTION_UP*/)
													|| (resourceUnit.getY() - unit.getY() > 0 && directionIdentifier == 2/*UnitAction.DIRECTION_DOWN*/)) actionScore += m_MoveToHarvestWeight;
											}
										}
									}
								}
							}
						}
					}
					// Check for producing (one call not worth a m_ProduceWeight non zero check
					if (potentialAction.getAction(unit).getType() == 4)
					{
						actionScore += m_ProduceWeight;
					}
					
					// Check for direct attacking (should always have a non zero weighting)
					if (potentialAction.getAction(unit).getType() == 5)// UnitAction.TYPE_ATTACK_LOCATION)
					{
						actionScore += m_AttackWeight;
					}
					else if (m_MoveToAttackWeight > 0)
					{
						// Check for en route attackers
						// Check against closest distance check set beforehand
						int tempDistance;
						
						for (Unit enemyUnit : m_EnemyList)
						{
							tempDistance = Math.abs(unit.getX() - enemyUnit.getX()) + Math.abs(unit.getY() - enemyUnit.getY());
							
							if (tempDistance < m_AttackDistance)
							{
								int directionIdentifier = potentialAction.getAction(unit).getDirection();
								
								if ((enemyUnit.getX() - unit.getX() < 0 && directionIdentifier == 3/* UnitAction.DIRECTION_LEFT*/)
										|| (enemyUnit.getX() - unit.getX() > 0 && directionIdentifier == 1/* UnitAction.DIRECTION_RIGHT*/)
										|| (enemyUnit.getY() - unit.getY() < 0 && directionIdentifier == 0/* UnitAction.DIRECTION_UP*/)
										|| (enemyUnit.getY() - unit.getY() > 0 && directionIdentifier == 2/* UnitAction.DIRECTION_DOWN*/)) actionScore += m_MoveToAttackWeight;
							}
						}
					}
				}
			}
    	}
		return actionScore;
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
//    EvaluationFunction ORIGINAL_EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
    DinoEvaluation evaluationFunction;
    
    // Simulations require an opponent to play out against, RandomBiasedAI is a slightly stronger opponent than RandomAI, Or maybe choose stronger?
    AI simulationEnemyAI = new RandomBiasedAI();
    
    Tree tree;
    GameState initialGameState;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 100;
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH;
    
    // If doing a NSimulate evaluation then average random play outs over this many simulations
    int SIMULATION_PLAYOUTS;
    
    int totalNodeVisits;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // For finding the near side resources
    int halfMapDistance;
    
    int playerNumberDifference;
    
    // for epsilon greedy?
//    Random random = new Random();
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    PhysicalGameState physicalGameState;
    
    float evaluationBound = 1;
    long endTime;
    
    UnitType baseType;
    UnitType workerType;
    
    public Velociraptor(UnitTypeTable utt) {
    	unitTypeTable = utt;
        baseType = utt.getUnitType("Base");
        workerType = utt.getUnitType("Worker");
        evaluationFunction = new DinoEvaluation(unitTypeTable);
    }      
    
    
    public Velociraptor() {
    }
    
    
    public void reset() {
        tree = null;
        initialGameState = null;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
    }
    
    
    public void resetSearch() {
        tree = null;
        initialGameState = null;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
    }
    
    
    public AI clone() {
        return new Velociraptor();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
        playerNumber = player;
        initialGameState = gameState;
    	totalNodeVisits = 0;
    	
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Used to estimate the look ahead max tree depth heuristic
        physicalGameState = gameState.getPhysicalGameState();
        
        // Epsilon greedy?
 //       if (random.nextFloat() < 0.07f) return new PlayerActionGenerator(gameState, player).getRandom();
        
        // Simulate against the best heuristic quick time algorithm possible / available
//        simulationEnemyAI = new Brontosaurus(unitTypeTable);
        
        
        // Cartesian derived heuristic for a lookahead amount, halfway plus a bit
        MAX_TREE_DEPTH = 10;// (physicalGameState.getWidth() * 2);// + physicalGameState.getHeight());///2 + 2;
        SIMULATION_PLAYOUTS = 30;
        
        // This just returns 1 as far as I can tell
//        float evaluation_bound = EVALUATION_FUNCTION.upperBound(gameState);
        
        // Time limit
        endTime = System.currentTimeMillis() + MAXSIMULATIONTIME;
        
        // For determining nearby resources
        halfMapDistance = (physicalGameState.getWidth() + physicalGameState.getHeight()) / 2 + 1;
        
        // Initialise the tree
        tree = new Tree(halfMapDistance, baseType, workerType);
        tree.initRoot(playerNumber, gameState, evaluationBound, endTime);
        tree.analyseGameState();
/*        
        int gameStateTime = gameState.getTime();
        
        if 		(gameStateTime < 100) 	tree.setAnalysisWeightings(100.0f,	1.0f,	100.0f,	0.0f,	0.0f,	6);
        else if (gameStateTime < 300)	tree.setAnalysisWeightings(50.0f,	1.0f,	100.0f,	5.0f,	100.0f,	8);
        else if (gameStateTime < 600)	tree.setAnalysisWeightings(10.0f,	0.2f,	100.0f,	8.0f,	50.0f,	halfMapDistance);
        else if (gameStateTime < 1000)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	20.0f,	halfMapDistance*2);
        else if (gameStateTime < 2000)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	10.0f,	halfMapDistance*2);
        else if (gameStateTime < 5000)	tree.setAnalysisWeightings(0.0f,	0.0f,	10.0f,	1.0f,	0.0f,	halfMapDistance*2);
*/        
        playerNumberDifference = tree.getPlayerUnitDifference();
        
        if		(tree.getEnemyListSize() <= 2 && gameState.getTime() > 1000)	tree.setAnalysisWeightings(0.0f,	0.0f,	100.0f,	10.0f,	0.0f,	halfMapDistance*2);
        else if (playerNumberDifference < 2) 	tree.setAnalysisWeightings(100.0f,	10.0f,	40.0f,	0.0f,	0.0f,	6);
        else if (playerNumberDifference < 3)	tree.setAnalysisWeightings(50.0f,	1.0f,	100.0f,	5.0f,	100.0f,	8);
        else if (playerNumberDifference < 4)	tree.setAnalysisWeightings(10.0f,	0.2f,	100.0f,	8.0f,	50.0f,	halfMapDistance);
        else if (playerNumberDifference < 5)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	20.0f,	halfMapDistance*2);
        else if (playerNumberDifference < 6)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	10.0f,	halfMapDistance*2);
        else if (gameState.getTime() > 4000)	tree.setAnalysisWeightings(0.0f,	0.0f,	100.0f,	10.0f,	0.0f,	halfMapDistance*2);
        
        Node nodeToExplore = tree.getRoot();
        
        // Main loop
        while (true)
        {
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) { /*System.out.println("Outta time");*/ break; }
            
            nodeToExplore = tree.findNewNodeWithBestUCTScore(totalNodeVisits);
            
        	// Tries to get a new unexplored action from the tree
            Node newNode = nodeToExplore.selectNewAction(tree, playerNumber, endTime, MAX_TREE_DEPTH);
    		
            // If no new actions then null is returned
            if (newNode != null)
            {
            	totalNodeVisits++;
            	
            	// Clone the gameState for use in the simulation
                GameState gameStateClone = newNode.getGameState().clone();
                
                // Not too sure here, decay, the evaluation tends towards zero as the time increases
//                int time = gameStateClone.getTime() - initialGameState.getTime();
                
                // Simulate a play out of that gameState
//                simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
//                double evaluation = evaluationFunction/*EVALUATION_FUNCTION*/.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);//EvalFunction
                double evaluation = NSimulate(gameStateClone, player, SIMULATION_PLAYOUTS);
                
                // Back propagation, cycle though each node's parents until the tree root is reached
                boolean printOnce = false;//true;
                
                while(newNode != null)
                {
                	if (printOnce)
                	{
                		System.out.println("Node Depth is: " + newNode.getDepth());
                		System.out.println("");
                		printOnce = false;
                	}
                    newNode.addScore(evaluation);
                    newNode.incrementVisitCount(); 
                    newNode = newNode.getParent();
                }
            }
            
            Node tempNodeToExplore = tree.findNewNodeWithBestUCTScore(totalNodeVisits);
            
            if (tempNodeToExplore != null)
            {
            	nodeToExplore = tempNodeToExplore;
            }
            else
            {
            	System.out.println("Shit snacks");
            }
        }
        
        // Sanity check
        if (tree.getRoot().getChildrenList() == null)
        {
        	System.out.println("This should not happen.");
        	return simulationEnemyAI.getAction(player, gameState);
       	}
        
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
        	System.out.println("This should definitely not happen.");
        	return simulationEnemyAI.getAction(player, gameState);
       	}
/*
        System.out.println("    Tree children visits: 	" + tempMostVisited.getVisitCount());
        System.out.println("    Winning child score: 	" + tempMostVisited.getScore());
        System.out.println("    Total Nodes explored: 	" + totalNodeVisits);
        System.out.println("    Root Node visits: 		" + tree.getRoot().getVisitCount());
        System.out.println("");
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
//            accum += (float)(ORIGINAL_EVALUATION_FUNCTION.evaluate(player, 1-player, thisNGS)*Math.pow(0.99,time/10.0));
            accum += (float)(evaluationFunction.evaluate(player, 1-player, thisNGS)*Math.pow(0.99,time/10.0));
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

class DinoEvaluation extends EvaluationFunction
{   
	// Value of the player's resource in base
    public static float RESOURCE_VALUE = 40;
    // Value of each unit's carried resource
    public static float RESOURCE_IN_WORKER = 20;
    // Value modifier for owning a unit
    public static float UNIT_BONUS_MULTIPLIER = 50;
    
    static float HARVEST_VALUE = 150;
    static float PRODUCE_VALUE = 100;
    static float BASE_VALUE = 30;
    static float BARRACKS_VALUE = 60;
    static float RANGED_VALUE = 200;
    static float LIGHT_VALUE = 200;
    
    UnitType m_WorkerType;
    UnitType m_BaseType;
    UnitType m_BarracksType;
    UnitType m_RangedType;
    UnitType m_LightType;
    
    public DinoEvaluation(UnitTypeTable unitTypeTable)
    {
    	m_WorkerType = unitTypeTable.getUnitType("Worker");
        m_BaseType = unitTypeTable.getUnitType("Base");
        m_BarracksType = unitTypeTable.getUnitType("Barracks");
        m_RangedType = unitTypeTable.getUnitType("Ranged");
        m_LightType = unitTypeTable.getUnitType("Light");
    }
    
    public float evaluate(int maxplayer, int minplayer, GameState gs)
    {
        float score1 = baseScore(maxplayer, gs);
        float score2 = baseScore(minplayer, gs);
        
        if (score1 + score2 == 0) return 0.5f;
        return (2 * score1 / (score1 + score2)) - 1;
    }
    
    public float baseScore(int player, GameState gameState)
    {
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        
        // Initialise score float with player resource value
        float score = gameState.getPlayer(player).getResources() * RESOURCE_VALUE;
        
        boolean playerHasUnits = false;
        for(Unit unit : gameState.getUnits())// physicalGameState.getUnits())
        {
            if (unit.getPlayer() == player) 
            {
                playerHasUnits = true;
                score += unit.getResources() * RESOURCE_IN_WORKER;
                score += UNIT_BONUS_MULTIPLIER * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
 /*               
                if (unit.getType() == m_WorkerType)
                {
                	if (unit. == UnitAction.TYPE_HARVEST || gameState.getUnitAction(unit) == 3) score += BASE_VALUE * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
                	else if (gameState.getUnitAction(unit) == 4) score += BASE_VALUE * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
                }
 */               
                if 		(unit.getType() == m_BaseType) 		score += BASE_VALUE 	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
	            else if (unit.getType() == m_BarracksType)	score += BARRACKS_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
                else if (unit.getType() == m_RangedType)	score += RANGED_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
	            else if (unit.getType() == m_LightType)		score += LIGHT_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
            }
            else if (unit.getType() == m_BaseType)			score -= BASE_VALUE * 99 * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
        }
        if (!playerHasUnits) return 0;
        return score;
    }    
    
    public float upperBound(GameState gs)
    {
        return 1.0f;
    }
}

