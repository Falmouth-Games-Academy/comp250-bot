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
//import rts.units.Unit;
//import rts.units.UnitType;
import rts.units.UnitTypeTable;
//import rts.units.*;


/**
 *
 * @author Stomps
 */

class PteroNode
{
	// C needs a lot of tweaking
    private float C = 990.05f;
    private PteroNode m_Parent;// = null;
    private GameState m_GameState;
    private int m_CurrentTreeDepth;// = 0;
    
    private boolean m_HasUnexploredActions = true;
    private PlayerActionGenerator m_ActionGenerator = null;
    private List<PteroNode> m_ChildrenList = new ArrayList<>();
    private float m_EvaluationBound = 0;
    private double m_Score = 0;
    private int m_VisitCount = 0;
    private Map<PteroNode, PlayerAction> m_ActionMap = new HashMap<PteroNode, PlayerAction> ();
    
/*------------------------------------------------------------------------*/    
    
    public double getScore() { return m_Score; }
    public void addScore(double score) { m_Score += score; }
    public PteroNode getParent() { return m_Parent; }
    public GameState getGameState() { return m_GameState; }
    public int getVisitCount() { return m_VisitCount; }
    public void incrementVisitCount() { m_VisitCount++; }
    public List<PteroNode> getChildrenList() { return m_ChildrenList; }
    public void addChild (PteroNode child) { m_ChildrenList.add(child); }
    public PlayerAction getActionFromChildNode(PteroNode child) { return m_ActionMap.get(child); }
    public int getDepth() { return m_CurrentTreeDepth; }
    
/*------------------------------------------------------------------------*/   
    
    // Constructor
    public PteroNode(int maxPlayer, int minPlayer, PteroNode parent, GameState gameState, float evaluationBound) throws Exception
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
    
    public boolean checkIfFavourableGameState(GameState gameState, PlayerAction potentialAction)
    {
		// Clone the gameState from after the command
		GameState simulatedGameState = gameState.cloneIssue(potentialAction);   
		
    	// Leaf pruning for harvesting
		for (Unit unit : simulatedGameState.getUnits())
		{
			if (potentialAction != null && potentialAction.getAction(unit) != null && (potentialAction.getAction(unit).getType() == 2 || potentialAction.getAction(unit).getType() == 3))
			{
				return true;
			}
		}
		return false;
    }
    
    // Returns a new Node linked to a new unexplored player action in the m_ActionMap as the PlayerAction's key
    public PteroNode selectNewAction(int maxPlayer, int minPlayer, long endTime, int maxTreeDepth, int totalNodeVisits/*, Node tree*/) throws Exception
    {
        // Do a depth check. This AI will explore up to a predefined depth as the end of the game is often too far away
        if (m_CurrentTreeDepth >= maxTreeDepth) return this;        
        
        // If this node has unexplored actions, else look at best child child determined by UCB.
    	if (m_HasUnexploredActions)
        {
    		PlayerAction nextAction = null;
    		boolean validNextMoveFound = false;
    		
			// Pruning
//    		while (!validNextMoveFound)// && nextAction != null
//    		{
	    		
	    		// If no more actions
	            if (m_ActionGenerator == null) 
	            {
	            	//validNextMoveFound = true;
	            	return this;
	            }
	            
	            // Move to the next (randomised order on initialisation) action available 
	    		/*PlayerAction*/ nextAction = m_ActionGenerator.getNextAction(endTime);
    		
/***********************************/    		
    		
	    		
    				
/*   				
    				switch (unit.getHarvestAmount())// .getResources())
    				{
    				case 0:
    					// nextAction has at least one harvesting unitAction
	    				if (nextAction != null && nextAction.getAction(unit) != null)
	    				{
	    					if (nextAction.getAction(unit).getType() == 2)
	    					{
	    						validNextMoveFound = true;
	    						break;
	    					}
	    				}
    					break;
    					
    				case 1:
    					if (nextAction != null && nextAction.getAction(unit) != null)
	    				{
	    					if (nextAction.getAction(unit).getType() == 3)// TYPE_RETURN
	    					{
	    						validNextMoveFound = true;
	    						break;
	    					}
	    				}
    					break;
    					
    				default:
    					System.out.println("You don't know how resources work: " + unit.getResources());
    					break;
    				
    				}
*/    				
    				
    				
    			
			

/***********************************/    		
    		

            
    		// Check if last action that is available has been reached (next will be null)
    		if (nextAction != null)
            {
    			if (!checkIfFavourableGameState(m_GameState, nextAction))
    			{
    				selectNewAction(maxPlayer, minPlayer, endTime, maxTreeDepth, totalNodeVisits);
    			}
    			else
    			{
	    			// Clone the gameState from after the command
	    			GameState simulatedGameState = m_GameState.cloneIssue(nextAction);                
	                
	    			// Constructor takes for new child takes 'this' as parent argument
	        		PteroNode newChildNode = new PteroNode(maxPlayer, minPlayer, this, simulatedGameState.clone(), m_EvaluationBound);
	                
	        		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
	    			m_ActionMap.put(newChildNode, nextAction);
	        		
	    			// Add to children list. This is later cycled through to find the best child of a node
	                m_ChildrenList.add(newChildNode);
	                
	                //tree.addChild(newChildNode);
	                
	                return newChildNode;       
    			}
            }
            else
            {
            	// Stop future iterations from trying to explore new actions from this node
                m_HasUnexploredActions = false;
            }
        
        }
        
        // Temporary variables
        PteroNode tempBestNode = null;
        double tempBestScore = 0;
        
        // Find the child with the best UCB score
        for (PteroNode childNode : m_ChildrenList)//tree.getChildrenList())
        {
            double childNodeScore = UCBScore(childNode, totalNodeVisits);
            if (tempBestNode == null || childNodeScore > tempBestScore)
            {
                tempBestNode = childNode;
                tempBestScore = childNodeScore;
            }
        } 
        
        // Sanity check, or if no children
        if (tempBestNode == null) return this;
        
//        System.out.println(tempBestNode.getDepth());
        
        // Explore that child for new unexplored PlayerActions
        return tempBestNode.selectNewAction(maxPlayer, minPlayer, endTime, maxTreeDepth, totalNodeVisits);
    }    
      
    public double UCBScore(PteroNode child, int totalNodeVisits)
    {
    	// Tweak the constant. Dynamic? How...
    	//C = 0.707f;
    	if (child.getVisitCount() == 1)
    	{
    		//System.out.println("fuck you dinosaurs");
    		//return -100.0;
    	}
    	
    	return child.getScore()/child.getVisitCount() + C * Math.sqrt(2 * Math.log(totalNodeVisits/*(double)child.getParent().getVisitCount()*/)/child.getVisitCount());
    }
}


// The AI class
public class Pterodactyl extends AI//WithComputationBudget implements InterruptibleAI
{
	// Game evaluation function that returns a value based on units and resources available
    EvaluationFunction EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
    
    // Simulations require an opponent to play out against, RandomBiasedAI is a slightly stronger opponent than RandomAI, Or maybe choose stronger?
    //Brontosaurus simulationEnemyAI;
    AI simulationEnemyAI = new RandomBiasedAI();
    
    // 
    GameState initialGameState = null;
    PteroNode tree = null;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 1000000;//1024; // 100?
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH; //10;
    
    int totalNodeVisits = 0;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    
    // for epsilon greedy?
    Random random = new Random();
    
    int rushCountdownToMCTS = 5; //50;
    
    //int DEBUG_NUMBER_OF_ACTIONS_LOOKED_AT = 0;
    boolean HasCalculatedMaxTreeDepth = false;
    
    float evaluationBound = 1;
    long endTime;
    PhysicalGameState physicalGameState;
    
    
    public Pterodactyl(UnitTypeTable utt) {
    	unitTypeTable = utt;
    }      
    
    
    public Pterodactyl() {
    }
    
    
    public void reset() {
        initialGameState = null;
        tree = null;
        rushCountdownToMCTS = 50;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();//new Brontosaurus(unitTypeTable);//
    }
    
    
    public void resetSearch() {
        tree = null;
        initialGameState = null;
        rushCountdownToMCTS = 50;
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();//new Brontosaurus(unitTypeTable);//
    }
    
    
    public AI clone() {
        return new Pterodactyl();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
    	// 
    	totalNodeVisits = 1;
    	
    	
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Used to estimate the look ahead max tree depth heuristic
        /*PhysicalGameState*/ physicalGameState = gameState.getPhysicalGameState();
/*        
        // Rush on larger maps
        if (physicalGameState.getWidth() > 11 && rushCountdownToMCTS != 0) 
        	{
        		rushCountdownToMCTS--;
        		return new Brontosaurus(unitTypeTable).getSimulatedAction(player, gameState);
        	}
        
        
        // Epsilon greedy?
        if (random.nextFloat() < 0.07f) return new PlayerActionGenerator(gameState, player).getRandom();
*/        
        // Simulate against the best heuristic quick time algorithm possible / available
//        simulationEnemyAI = new Brontosaurus(unitTypeTable);
        
        
        // Cartesian derived heuristic for a lookahead amount, halfway plus a bit
        MAX_TREE_DEPTH = (physicalGameState.getWidth() * 2);// + physicalGameState.getHeight());///2 + 2;
        
        // This just returns 1 as far as I can tell
//        float evaluation_bound = EVALUATION_FUNCTION.upperBound(gameState);
        
        playerNumber = player;
        initialGameState = gameState;
        
        // Initialise the tree as a new Node with parent = null
        tree = new PteroNode(playerNumber, 1-playerNumber, null, gameState.clone(), evaluationBound);
        
        // Time stuff can be done better
        //long startTime = System.currentTimeMillis();
        /*long*/ endTime = /*startTime*/System.currentTimeMillis() + 100;
        
        // Main loop
        while(true)
        {
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) break;
            
        	// Tries to get a new unexplored action from the tree
            PteroNode newNode = tree.selectNewAction(playerNumber, 1-playerNumber, endTime, MAX_TREE_DEPTH, totalNodeVisits);//, tree);
    		
            
            // If no new actions then null is returned
            if (newNode != null)
            {
            	tree.addChild(newNode);
            	
            	totalNodeVisits++;
            	
            	// Clone the gameState for use in the simulation
                GameState gameStateClone = newNode.getGameState().clone();
                
                // Simulate a play out of that gameState
                simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
                //double evaluation = NSimulate(gameStateClone, player, 5);
                
                // Not too sure here, the evaluation tends towards zero as the time increases
                int time = gameStateClone.getTime() - initialGameState.getTime();
                double evaluation = EVALUATION_FUNCTION.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);

//                System.out.println(evaluation);
                
                // Back propagation, cycle though each node's parents until the tree root is reached
                while(newNode != null)
                {
                    newNode.addScore(evaluation);
                    newNode.incrementVisitCount();
                    newNode = newNode.getParent();
                    //totalNodeVisits++;
                }
            }
        }
        
        // Sanity check
        if (tree.getChildrenList() == null) return new PlayerAction();
        
        // Temporary variable
        PteroNode tempMostVisited = null;
        
        for (PteroNode child : tree.getChildrenList())
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
        if (tempMostVisited == null) return new PlayerAction();
        
        System.out.println(totalNodeVisits);
        
        // m_ActionMap getter
        return tree.getActionFromChildNode(tempMostVisited);
    }
      
    
    
/*    
 * ---------------------------------
 * This could be a better simulation environment? Averaging N play outs.
 * ---------------------------------
 * 
 *     
*/
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float NSimulate(GameState gameStateClone, int player, int N) throws Exception {
        //PlayerAction pa = getBestActionSoFar();
        
        //if (pa==null) return 0;

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
                
//                gameState.issue(simulationEnemyAI.getSimulatedAction(0, gameState));
//                gameState.issue(simulationEnemyAI.getSimulatedAction(1, gameState));
            }
        }while(!gameover && gameState.getTime() < time);   
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}
/*
class EvaluationFunction
{
	
}
*/

