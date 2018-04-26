/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import util.Pair;


/**
 *
 * @author Stomps
 */

public class Node
{
	// C needs tweaking
    private float C = 0.05f;
    private Node m_Parent;
    private GameState m_GameState;
    private int m_CurrentTreeDepth;
    
    private MyPlayerActionGenerator m_ActionGenerator;
    private List<Node> m_ChildrenList = new ArrayList<>();
    private double m_Score = 0;
    private int m_VisitCount = 0;
    private Map<Node, PlayerAction> m_ActionMap = new HashMap<Node, PlayerAction> ();
    private List<Pair<PlayerAction, Float>> m_OrderedActionList = new ArrayList<Pair<PlayerAction, Float>>();
    private PlayerAction m_Action;
    private GameState m_SimulatedGameState;
    
    private int m_CurrentActionIndex = 0;
    private int m_MaxActionIndex;
    private int m_MaxAmountOfNodeActionsToExamine = 30;
    
    private Analysis m_Analysis;
    
/*------------------------------------------------------------------------*/    
    
    public double getScore() { return m_Score; }
    public void addScore(double score) { m_Score += score; }
    public Node getParent() { return m_Parent; }
    public GameState getGameState() { return m_GameState; }
    public int getVisitCount() { return m_VisitCount; }
    public void incrementVisitCount() { m_VisitCount++; }
    public List<Node> getChildrenList() { return m_ChildrenList; }
    public PlayerAction getActionFromChildNode(Node child) { return m_ActionMap.get(child); }
    
/*------------------------------------------------------------------------*/   
    
    // Constructor
    public Node(int maxPlayer, int minPlayer, Node parent, GameState gameState, Analysis analysis, long endTime) throws Exception
    {
        m_Parent = parent;
        m_GameState = gameState;
        m_Analysis = analysis;
        
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
        	// This then analyses the m_ActionGenerator, systematically simulating and analysing the playerAction. This returns a list of playerActions ordered by how favourable
        	// the Analysis has decided it is based on heuristics set in the opening of the getAction() being called
	        if (m_GameState.canExecuteAnyAction(maxPlayer))
	        {
	            m_ActionGenerator = new MyPlayerActionGenerator(gameState, maxPlayer);
	            m_ActionGenerator.randomizeOrder();
	            m_OrderedActionList = analysis.AnalyseAndSortActionSpace(m_ActionGenerator, m_GameState, endTime);
	            
	            // Set the max for the iterator. this ensures that simulations are only performed on the top scoring actions
	            if (m_OrderedActionList.size() > m_MaxAmountOfNodeActionsToExamine) m_MaxActionIndex = m_MaxAmountOfNodeActionsToExamine;
	            else m_MaxActionIndex = m_OrderedActionList.size();
	        }
	        else if (m_GameState.canExecuteAnyAction(minPlayer))
	        {
	            m_ActionGenerator = new MyPlayerActionGenerator(gameState, minPlayer);
	            m_ActionGenerator.randomizeOrder();
	            m_OrderedActionList = analysis.AnalyseAndSortActionSpace(m_ActionGenerator, m_GameState, endTime);
	            
	            if (m_OrderedActionList.size() > m_MaxAmountOfNodeActionsToExamine) m_MaxActionIndex = m_MaxAmountOfNodeActionsToExamine;
	            else m_MaxActionIndex = m_OrderedActionList.size();
	        }
        }
    }
    
    // Returns a new Node linked to a new unexplored player action in the m_ActionMap as the PlayerAction's key
    public Node selectNewAction(int maxPlayer, int minPlayer, long endTime, int maxTreeDepth) throws Exception
    {
        // Do a depth check. This AI will explore up to a predefined depth as the end of the game is often too far away
        if (m_CurrentTreeDepth >= maxTreeDepth/* || m_OrderedActionList.size() == 0*/) return this;
        
		// If no actions found
        if (m_ActionGenerator == null) return this;
        
        // Check the iterator against the max allowed amount
        if (m_CurrentActionIndex < m_MaxActionIndex)
        {
        	// Get the next action in the ordered list
            m_Action = m_OrderedActionList.get(m_CurrentActionIndex).m_a;
            
            // increment the iterator
            m_CurrentActionIndex++;
            
            // Sanity check, the list should not add a null value. Perhaps if there are no available actions
            if (m_Action == null) return null;
            
            // Clone the gameState from the action being issued
			m_SimulatedGameState = m_GameState.cloneIssue(m_Action);
            
			// New child node constructor takes 'this' as parent argument and the cloneIssued gameState as it's current state
    		Node newChildNode = new Node(maxPlayer, minPlayer, this, m_SimulatedGameState.clone(), m_Analysis, endTime);
            
    		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
			m_ActionMap.put(newChildNode, m_Action);
    		
			// Add to children list. This is later cycled through to find the best child of a node
            m_ChildrenList.add(newChildNode);
            
            return newChildNode;
        }
        
        // Temporary variables
        Node tempBestNode = null;
        double tempBestScore = 0;
        
        // Find the child with the best UCB score
        for (Node childNode : m_ChildrenList)
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
      
    public double UCBScore(Node child)
    {
    	// Tweak the constant. Dynamic? How...
    	//C = 0.0f;
    	
    	return child.getScore()/child.getVisitCount() + C * Math.sqrt(2 * Math.log((double)child.getParent().getVisitCount())/child.getVisitCount());
    }
}
