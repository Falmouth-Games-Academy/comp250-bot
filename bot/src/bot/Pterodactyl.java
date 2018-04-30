package bot;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import java.util.Random;

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
    private float C = 0.05f;
    private PteroNode m_Parent;
    private GameState m_GameState;
    private int m_CurrentTreeDepth;
    
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
    public PlayerAction getActionFromChildNode(PteroNode child) { return m_ActionMap.get(child); }
    
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
    
    // Returns a new PteroNode linked to a new unexplored player action in the m_ActionMap as the PlayerAction's key
    public PteroNode selectNewAction(int maxPlayer, int minPlayer, long endTime, int maxTreeDepth) throws Exception
    {
        // Do a depth check. This AI will explore up to a predefined depth as the end of the game is often too far away
        if (m_CurrentTreeDepth >= maxTreeDepth) return this;        
        
        // If this node has unexplored actions, else look at best child child determined by UCB.
    	if (m_HasUnexploredActions)
        {
    		// If no more actions
            if (m_ActionGenerator == null) return this;
            
            // Move to the next (randomised order on initialisation) action available
    		PlayerAction nextAction = m_ActionGenerator.getNextAction(endTime);
            
    		// Check if last action that is available has been reached (next will be null)
    		if (nextAction != null)
            {
    			// Clone the gameState from after the command
    			GameState simulatedGameState = m_GameState.cloneIssue(nextAction);                
                
    			// Constructor takes for new child takes 'this' as parent argument
        		PteroNode newChildNode = new PteroNode(maxPlayer, minPlayer, this, simulatedGameState.clone(), m_EvaluationBound);
                
        		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
    			m_ActionMap.put(newChildNode, nextAction);
        		
    			// Add to children list. This is later cycled through to find the best child of a node
                m_ChildrenList.add(newChildNode);
                
                return newChildNode;                
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
        for (PteroNode childNode : m_ChildrenList)
        {
            double childNodeScore = UCBScore(childNode);
            if (tempBestNode == null || childNodeScore > tempBestScore)
            {
                tempBestNode = childNode;
                tempBestScore = childNodeScore;
            }
        } 
        
        // Sanity check, or if no children
        if (tempBestNode == null) return this;
        
        // Explore that child for new unexplored PlayerActions
        return tempBestNode.selectNewAction(maxPlayer, minPlayer, endTime, maxTreeDepth);
    }    
      
    public double UCBScore(PteroNode child)
    {
    	// Tweak the constant. Dynamic? How...
    	//C = 0.707f;
    	
    	return child.getScore()/child.getVisitCount() + C * Math.sqrt(2 * Math.log((double)child.getParent().getVisitCount())/child.getVisitCount());
    }
}


/// The AI class. Initialises all values to be used by the getAction function.
public class Pterodactyl extends AI
{
	// Game evaluation function that returns a value based on units and resources available
    EvaluationFunction EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
    
    // Simulations require an opponent to play out against
    AI simulationEnemyAI = new RandomBiasedAI();
    
    GameState initialGameState = null;
    PteroNode tree = null;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 100;
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    
    
    public Pterodactyl(UnitTypeTable utt) {
    	unitTypeTable = utt;
    }      
    
    
    public Pterodactyl() {
    }
    
    
    public void reset() {
        initialGameState = null;
        tree = null;
    }
    
    
    public void resetSearch() {
        tree = null;
        initialGameState = null;
    }
    
    
    public AI clone() {
        return new Pterodactyl();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
    	// 
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Used to estimate the look ahead max tree depth heuristic
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        
        // Cartesian derived heuristic for a lookahead amount
        MAX_TREE_DEPTH = physicalGameState.getWidth() + physicalGameState.getHeight();
        
        // This just returns 1 as far as I can tell
        float evaluation_bound = EVALUATION_FUNCTION.upperBound(gameState);
        
        playerNumber = player;
        initialGameState = gameState;
        
        // Initialise the tree as a new PteroNode with parent = null
        tree = new PteroNode(playerNumber, 1-playerNumber, null, gameState.clone(), evaluation_bound);
        
        // Time stuff can be done better
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 100;
        
        // Main loop
        while(true)
        {
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) break;
            
        	// Tries to get a new unexplored action from the tree
            PteroNode newNode = tree.selectNewAction(playerNumber, 1-playerNumber, endTime, MAX_TREE_DEPTH);
            
            // If no new actions then null is returned
            if (newNode != null)
            {
            	// Clone the gameState for use in the simulation
                GameState gameStateClone = newNode.getGameState().clone();
                
                // Simulate a play out of that gameState
                simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
                
                // Not too sure here, the evaluation tends towards zero as the time increases
                int time = gameStateClone.getTime() - initialGameState.getTime();
                double evaluation = EVALUATION_FUNCTION.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);

                // Back propagation, cycle though each node's parents until the tree root is reached
                while(newNode != null)
                {
                    newNode.addScore(evaluation);
                    newNode.incrementVisitCount();
                    newNode = newNode.getParent();
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
        
        // m_ActionMap getter
        return tree.getActionFromChildNode(tempMostVisited);
    }
      
    
    
/*    
 * ---------------------------------
 * This could be a better simulation environment? Averaging N play outs.
 * ---------------------------------
 * 
 * 
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float getBestActionEvaluation(GameState gs, int player, int N) throws Exception {
        PlayerAction pa = getBestActionSoFar();
        
        if (pa==null) return 0;
        float accum = 0;
        for(int i = 0;i<N;i++) {
            GameState gs2 = gs.cloneIssue(pa);
            GameState gs3 = gs2.clone();
            simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);
            int time = gs3.getTime() - gs2.getTime();
            // Discount factor:
            accum += (float)(EVALUATION_FUNCTION.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0));
        }
            
        return accum/N;
    }    
*/    
    
    
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
                
//                gameState.issue(simuationEnemyAI.getSimulatedAction(0, gameState));
//                gameState.issue(simuationEnemyAI.getSimulatedAction(1, gameState));
            }
        }while(!gameover && gameState.getTime() < time);   
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}