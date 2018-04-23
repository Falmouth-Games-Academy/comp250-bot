/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
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
    private MyPlayerActionGenerator m_ActionGenerator = null;
    private PlayerAction m_Action = null;
    private GameState m_SimulatedGameState = null;
    private List<Node> m_ChildrenList = new ArrayList<>();
    private float m_EvaluationBound = 0;
    private double m_Score = 0;
    private int m_VisitCount = 0;
    private Map<Node, PlayerAction> m_ActionMap = new HashMap<Node, PlayerAction> ();

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
	            m_ActionGenerator = new MyPlayerActionGenerator(gameState, maxPlayer);
	            m_ActionGenerator.randomizeOrder();
	        }
	        else if (m_GameState.canExecuteAnyAction(minPlayer))
	        {
	            m_ActionGenerator = new MyPlayerActionGenerator(gameState, minPlayer);
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
    			if (tree.actionSurvivesPruning(m_GameState, m_Action))//((m_CurrentTreeDepth == 0 && tree.actionSurvivesPruning(m_GameState, m_Action)) || m_CurrentTreeDepth > 0)
    			{
    				
    				System.out.println("GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGHHHJ");
    				
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
    				backUpList.add(m_Action);
    				// No more actions in generator
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
    	C = 0.050f;
    	
    	// Trying to prioritise non visited Nodes
    	if (node.getVisitCount() <= 1)
    	{
    		return 1;
    	}
    	
    	return node.getScore()/node.getVisitCount() + C * Math.sqrt(2 * Math.log(totalNodeVisits)/node.getVisitCount());
    }
}

class MyPlayerActionGenerator
{
    static Random r = new Random();
    
    GameState m_GameState;
    PhysicalGameState m_PhysicalGameState;
    ResourceUsage m_ResourceUsage;
    List<Pair<Unit,List<UnitAction>>> m_Choices;
    PlayerAction lastAction = null;
    long size = 1;  // this will be capped at Long.MAX_VALUE;
    long generated = 0;
    int choiceSizes[] = null;
    int currentChoice[] = null;
    boolean moreActions = true;
    
    public MyPlayerActionGenerator(GameState gameState, int playerNumber) throws Exception
    {
        // Generate the reserved resources:
        m_ResourceUsage = new ResourceUsage();
        m_GameState = gameState;
        m_PhysicalGameState = m_GameState.getPhysicalGameState();
        
        for(Unit u:m_PhysicalGameState.getUnits())
        {
            UnitActionAssignment uaa = m_GameState.getUnitActions().get(u);
            if (uaa!=null)
            {
                ResourceUsage ru = uaa.action.resourceUsage(u, m_PhysicalGameState);
                m_ResourceUsage.merge(ru);
            }
        }
        
        m_Choices = new ArrayList<>();
        
        for (Unit unit : m_PhysicalGameState.getUnits())
        {
            if (unit.getPlayer()==playerNumber)
            {
                if (m_GameState.getUnitActions().get(unit) == null)
                {
                    List<UnitAction> unitActions = unit.getUnitActions(m_GameState);
                    m_Choices.add(new Pair<>(unit,unitActions));
                    // make sure we don't overflow:
                    long tempUnitActionsSize = unitActions.size();
                    if (Long.MAX_VALUE / size <= tempUnitActionsSize)
                    {
                        size = Long.MAX_VALUE;
                    }
                    else
                    {
                        size *= (long)unitActions.size();
                    }
                }
            }
        }

        if (m_Choices.size() == 0)
        {
            System.err.println("Problematic game state:");
            System.err.println(gameState);
            throw new Exception("Move generator for player " + playerNumber + " created with no units that can execute actions! (status: " + gameState.canExecuteAnyAction(0) + ", " + gameState.canExecuteAnyAction(1) + ")");
        }

        choiceSizes = new int[m_Choices.size()];
        currentChoice = new int[m_Choices.size()];
        int i = 0;
        for(Pair <Unit, List<UnitAction>> choice : m_Choices)
        {
            choiceSizes[i] = choice.m_b.size();
            currentChoice[i] = 0;
            i++;
        }
    }
    
    public void randomizeOrder()
    {
        for(Pair<Unit, List<UnitAction>> choice : m_Choices)
        {
            List<UnitAction> tmp = new LinkedList<>();
            tmp.addAll(choice.m_b);
            choice.m_b.clear();
            while(!tmp.isEmpty())
            {
            	choice.m_b.add(tmp.remove(r.nextInt(tmp.size())));
           	}
        }
    }
    
    public void incrementCurrentChoice(int startPosition)
    {
        for (int i = 0; i < startPosition; i++) currentChoice[i] = 0;
        currentChoice[startPosition]++;
        if (currentChoice[startPosition]>=choiceSizes[startPosition])
        {
            if (startPosition<currentChoice.length-1)
            {
                incrementCurrentChoice(startPosition+1);
            }
            else
            {
                moreActions = false;
            }
        }
    }
    
    public PlayerAction getNextAction(long cutOffTime) throws Exception
    {
        int count = 0;
        while(moreActions)
        {
            boolean consistent = true;
            PlayerAction pa = new PlayerAction();
            pa.setResourceUsage(m_ResourceUsage.clone());
            
            int i = m_Choices.size();
            
            if (i == 0) throw new Exception("Move generator created with no units that can execute actions!");
            
            while(i > 0)
            {
                i--;
                Pair <Unit, List<UnitAction>> unitChoices = m_Choices.get(i);
                int choice = currentChoice[i];
                Unit u = unitChoices.m_a;
                UnitAction ua = unitChoices.m_b.get(choice);
                
                ResourceUsage resUsage = ua.resourceUsage(u, m_PhysicalGameState);
                
                if (pa.getResourceUsage().consistentWith(resUsage, m_GameState))
                {
                    pa.getResourceUsage().merge(resUsage);
                    pa.addUnitAction(u, ua);
                }
                else
                {
                    consistent = false;
                    break;
                }
            }
            
            incrementCurrentChoice(i);
            if (consistent)
            {
                lastAction = pa;
                generated++;                
                return pa;
            }
            
            // check if we are over time (only check once every 1000 actions, since currenttimeMillis is a slow call):
            if (cutOffTime > 0 && (count%1000==0) && System.currentTimeMillis()>cutOffTime)
            {
                lastAction = null;
                return null;
            }
            count++;
        }
        lastAction = null;
        return null;
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
	
	private Unit m_HarvesterUnit = null;
    
    public Node getRoot() { return m_Root; }
    public void addNodeToExplore(Node node) { m_NodesToExplore.add(node); }
    public void removeNodeToExplore(Node node) { m_NodesToExplore.remove(node); }
    public int getNumberOfNodesToExplore() { return m_NodesToExplore.size(); }

/*------------------------------------------------------------------------*/   
    
    public Tree(int playerNumber, GameState gameState, int halfMapDistance, float evaluationBound, UnitType baseType, UnitType workerType, Unit harvesterUnit, boolean harvesterUnitFound) throws Exception
	{
		m_GameState = gameState;//.clone();
		m_Root = new Node(playerNumber, 1-playerNumber, null, m_GameState, evaluationBound);
		m_HalfMapDistance = halfMapDistance;
		m_PlayerNumber = playerNumber;
		m_BaseType = baseType;
		m_WorkerType = workerType;
		
		m_NodesToExplore.add(m_Root);
		
		if (harvesterUnitFound) m_HarvesterUnit = harvesterUnit;
		
        analyseGameState(harvesterUnit, harvesterUnitFound);
	}
	
	void analyseGameState(Unit harvesterUnit, boolean harvesterUnitFound)
	{
		// Spend some loops getting the player's base locations and all resource locations
		
		// Eventually only need to get locations once, can check if they are still available at the start of each getAction()
    	for (Unit unit : m_GameState.getUnits())
    	{
    		if(!m_BaseIsAvailable)
    		{
	    		if (unit.getType() == m_BaseType)
	    		{
	    			if(unit.getPlayer() == m_PlayerNumber)
		    		{
		    			// Get base location
		    			m_PlayerBaseLocationX = unit.getX();
		    			m_PlayerBaseLocationY = unit.getY();
		    			m_BaseIsAvailable = true;
		    			unit.setHitPoints(20);  
		    		}
	    			else
	    			{
	    				unit.setResources(0);
	    			}
	    		}
    		}
    		if (unit.getPlayer() == 1-m_PlayerNumber)
    		{
    			// Set something to do with enemy locations, add to list maybe, ...
    			m_EnemyList.add(unit);
    			unit.setHitPoints(1);
    		}
    		else
    		{
    			unit.setHitPoints(500);
    		}
    		if (unit.getPlayer() == m_PlayerNumber && unit.getType() == m_WorkerType)
    		{
    			if (!harvesterUnitFound)
    			{
    				harvesterUnit = unit;
    				m_HarvesterUnit = unit;
    				harvesterUnitFound = true;
    				System.out.println("found");
    			}
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
	        			unit.setResources(50);
    				}
    				else
    					unit.setResources(0);
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
    	if (m_ResourceIsAvailable && m_BaseIsAvailable && m_GameState.getTime() < 500)
    	{
    		// Prune for harvesting actions
			for (Unit unit : simulatedGameState.getUnits())
			{
				// Sanity check for nulls
				if (potentialAction != null && potentialAction.getAction(unit) != null)
				{
					// Look at our player
					if (unit == m_HarvesterUnit)//unit.getPlayer() == m_PlayerNumber)
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
								int directionIdentifier = potentialAction.getAction(unit).getDirection();
								
								// If it's packing resources
								if (unit.getResources() > 0)
								{
									// Check if moving back towards the base
									if (m_PlayerBaseLocationX- unit.getX() < 0 && directionIdentifier == 3) return true;
									if (m_PlayerBaseLocationX- unit.getX() > 0 && directionIdentifier == 1) return true;
									if (m_PlayerBaseLocationY- unit.getY() < 0 && directionIdentifier == 0) return true;
									if (m_PlayerBaseLocationY- unit.getY() < 0 && directionIdentifier == 2) return true;
									
									
								}
								// else if it's looking for resources
								else
								{
									for (Unit resourceUnit : m_ResourceUnitList)
				    				{
										
										
										// If it needs to go left etc
										if (resourceUnit.getX() - unit.getX() < 0 && directionIdentifier == 3/*UnitAction.DIRECTION_LEFT*/) { return true; }
										if (resourceUnit.getX() - unit.getX() > 0 && directionIdentifier == 1/*UnitAction.DIRECTION_RIGHT*/) { return true; }
										if (resourceUnit.getY() - unit.getY() < 0 && directionIdentifier == 0/*UnitAction.DIRECTION_UP*/) { return true; }
										if (resourceUnit.getY() - unit.getY() > 0 && directionIdentifier == 2/*UnitAction.DIRECTION_DOWN*/) { return true; }
									}
								}
							}
						}
					}
				}
			}
			return false;
    	}
    	else
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
						if (potentialAction.getAction(unit).getType() == 5)// UnitAction.TYPE_ATTACK_LOCATION)
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
									
									if (enemyUnit.getX() - unit.getX() < 0 && ua == 3/* UnitAction.DIRECTION_LEFT*/) 	{ m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }
									if (enemyUnit.getX() - unit.getX() > 0 && ua == 1/* UnitAction.DIRECTION_RIGHT*/) 	{ m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }
									if (enemyUnit.getY() - unit.getY() < 0 && ua == 0/* UnitAction.DIRECTION_UP*/) 		{ m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }
									if (enemyUnit.getY() - unit.getY() > 0 && ua == 2/* UnitAction.DIRECTION_DOWN*/) 	{ m_AttackUnitsCount++; if (m_AttackUnitsCount>m_WorkerCount/1.3) return true; }
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
    
    Unit harvesterUnit = null;
    boolean harvesterUnitFound = false;
    
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
        tree = new Tree(playerNumber, gameState, halfMapDistance, evaluationBound, baseType, workerType, harvesterUnit, harvesterUnitFound);
        
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
 * 1. Call selectNewAction() function on the current nodeToExplore to get a new Node, linked to the PlayerAction in the tree root node's action dictionary, and stored in the tree's children list.
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
                boolean printOnce = true;
                
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
      //      else
        //    	System.out.println("No good action found");
            
            Node tempNodeToExplore = tree.findNewNodeWithBestUCTScore(totalNodeVisits);
            
            if (tempNodeToExplore != null)
            {
            	nodeToExplore = tempNodeToExplore;
            }
            else
            {
//---------------------------------------------HERE       
            	System.out.println(nodeToExplore.backUpList.size());
            	
            	int randIndex = random.nextInt(nodeToExplore.backUpList.size());
            	GameState tempSimulatedGameState = nodeToExplore.getGameState();//.cloneIssue(nodeToExplore.backUpList.get(randIndex));
                
    			// Constructor takes for new child takes 'nodeToExplore' as parent argument
        		Node newChildNode = new Node(playerNumber, 1 - playerNumber, nodeToExplore, tempSimulatedGameState, evaluationBound);
                
        		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
    			nodeToExplore.addToActionMap(newChildNode, nodeToExplore.backUpList.get(randIndex));
        		
    			// Add to children list. This is later cycled through to find the best child of a node
                tree.getRoot().addChild(newChildNode);
                
                // Add to the list of Nodes worth exploring more
                tree.addNodeToExplore(newChildNode);
                
                nodeToExplore = newChildNode;   
            	
            	
   //         	System.out.println("FUUUUUUUUUUU");
   //         	return simulationEnemyAI.getAction(player, gameState);//break;
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

        System.out.println("    Tree children size: 	" + tree.getNumberOfNodesToExplore());
        System.out.println("    Total nodes examined: 	" + DEBUG_TOTAL_NODES_EXAMINED);
        System.out.println("    Total Nodes explored: 	" + totalNodeVisits);
        System.out.println("    Root Node visits: 		" + tree.getRoot().getVisitCount());
        
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


