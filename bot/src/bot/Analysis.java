/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import util.Pair;

/**
 *
 * @author Stomps
 * 
 * Used to analyse a gameState or a playerAction
 * 
 */


// Class holds the playerAction with it's analysed score for easy adding to a sorted set
class ActionInfo implements Comparable<Object>
{
	private PlayerAction m_playerAction;
	private int m_Score;
	
	// Constructor
	public ActionInfo(PlayerAction playerAction, int score)
	{
		m_playerAction = playerAction;
		m_Score = score;
	}

	public PlayerAction getPlayerAction()
	{
		return m_playerAction;
	}

	// The sort action sorts by descending value
	@Override
	public int compareTo(Object o)
	{
		return ((ActionInfo)o).m_Score - this.m_Score;
	}
}


public class Analysis
{
	private GameState m_GameState;
	private int m_PlayerNumber;
	private UnitType m_WorkerType;
	private UnitType m_BaseType;
	private Unit m_Base = null;
	
	private int m_FriendlyCount;
	private int m_HalfMapDistance;
	
    private List<Unit> m_ResourceUnitList = new ArrayList<>();
    private List<Unit> m_EnemyList = new ArrayList<>();
	
    // Scores are set as ints in order to be comparable by the sortedSet of ActionInfo
	private int m_HarvestWeight;
	private int m_MoveToHarvestWeight;
	private int m_AttackWeight;
	private int m_MoveToAttackWeight;
	private int m_ProduceWeight;
    private int m_AttackDistance;
    
/*------------------------------------------------------------------------*/   
	
    public int getPlayerUnitDifference() { return m_FriendlyCount - m_EnemyList.size(); }
    public int getEnemyListSize() { return m_EnemyList.size(); }

/*------------------------------------------------------------------------*/   
    
    // Constructor
    public Analysis(int playerNumber, GameState gameState, int halfMapDistance, UnitType baseType, UnitType workerType) throws Exception
	{
		m_HalfMapDistance = halfMapDistance;
		m_BaseType = baseType;
		m_WorkerType = workerType;
		m_GameState = gameState;
		m_PlayerNumber = playerNumber;
	}
    
    // Sets the weighting values used to analyse a playerAction so that the weightings can be changed throughout the game
    public void setAnalysisWeightings(int harvestWeight, int moveToHarvestWeight, int attackWeight, int moveToAttackWeight, int produceWeight, int attackDistance)
    {
    	m_HarvestWeight = harvestWeight;
    	m_MoveToHarvestWeight = moveToHarvestWeight;
    	m_AttackWeight = attackWeight;
    	m_MoveToAttackWeight = moveToAttackWeight;
    	m_ProduceWeight = produceWeight;
    	m_AttackDistance = attackDistance;
    }
	
    // Populates the classes fields that will affect the analysis of a playerAction
	void analyseGameState()
	{
		m_FriendlyCount = 0;
		
		// Spend some loops getting the player's base location, All enemy locations, and all resource locations
    	for (Unit unit : m_GameState.getUnits())
    	{
    		if (unit.getPlayer() == m_PlayerNumber)
    		{
    			m_FriendlyCount++;
    			
    			// No need to keep looking for it once it has been found. Assumes one base per player
	    		if (m_Base == null)
	    		{
    				// Check for this player's base
	    			if (unit.getType() == m_BaseType)
	    			{
	    				m_Base = unit;
	    			}
	    		}
    		}
    		if (unit.getPlayer() == 1-m_PlayerNumber)
    		{
    			// Store the enemies in a list
    			m_EnemyList.add(unit);
    		}
    	}
    	// Once the initial pass has completed do another loop for resources if Base has been found
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
	
	// Analyses each playerAction in an action generator, scoring it based on the weightings set, and returns a sorted set compared by the analysis score
    public SortedSet<ActionInfo> AnalyseAndSortActionSpace(MyPlayerActionGenerator actionGenerator, GameState gameState, long cutOffTime) throws Exception
    {
    	// Sore the arguments passed in
    	MyPlayerActionGenerator thisActionGenerator = actionGenerator;
    	int playerActionScore;
    	
    	// Sorted set will sort the set as new playerActions are added. Quicker than a sort on potentially thousands of actions later
    	SortedSet<ActionInfo> sortedActionList = new TreeSet<>();
    	
    	while (true)
    	{
    		// Many of the timeouts happen around here so check regularly
    		if (System.currentTimeMillis() > cutOffTime) break;
    		
    		// step through each playerAction generated by the MyPlayerActionGenerator on construction The final generated action will return null
    		PlayerAction playerAction = thisActionGenerator.getNextAction(cutOffTime);
    		
    		// Have the actions run out?
    		if (playerAction != null)
    		{
    			// set the start accumulated value to zero
    			playerActionScore = 0;
    			
    			// Safer here to check each individual unit action then check the time rather than the whole potentially expensive action at a time
	    		for (Pair<Unit, UnitAction> unitPair : playerAction.getActions())
	    		{
	    			// Sanity check for the player number beiing correct
	    			if (unitPair.m_a.getPlayer() == m_PlayerNumber)
	    			{
	    				// Accumulate the score based on the value of each unit action
	    				playerActionScore += analyseAction(unitPair.m_a, unitPair.m_b, cutOffTime);
	    			}
	    		}
	    		// After analysing each unit action, add the whole player action and it's score to the sorted set
    			sortedActionList.add(new ActionInfo(playerAction, playerActionScore));
    		}
    		// Else no more actions, break out of the while loop
    		else break;   	
    	}
    	// return the sorted list
		return sortedActionList;
    }
	
    // Analyses an individual Unit action for a Unit
    public int analyseAction(Unit unit, UnitAction unitAction, long cutOffTime)
    {
    	// Zero the initial score
		int actionScore = 0;
        
		// For opening moves. The only action is to create more workers
		if (unitAction == null || m_FriendlyCount == 0)
		{
			return actionScore;
		}
		else
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
						if (unitAction.getType() == 2 /*UnitAction.TYPE_HARVEST*/ || unitAction.getType() == 3 /*UnitAction.TYPE_RETURN*/)
						{
							actionScore += m_HarvestWeight;
						}
					}
					else if (m_MoveToHarvestWeight > 0)
					{
						if (unitAction.getType() == UnitAction.TYPE_MOVE)
						{
							int directionIdentifier = unitAction.getDirection();
							
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
									
									// Only add to score if a close worker is moving towards nearby resource
									if (tempDistance < m_HalfMapDistance)
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
			// Check for producing (one call not worth a m_ProduceWeight non zero check)
			if (unitAction.getType() == 4)
			{
				actionScore += m_ProduceWeight;
			}
			
			// Check for direct attacking (should always have a non zero weighting)
			if (unitAction.getType() == 5)// UnitAction.TYPE_ATTACK_LOCATION)
			{
				actionScore += m_AttackWeight;
			}
			else if (m_MoveToAttackWeight > 0)
			{
				// Check for en route attackers
				// Check against closest distance check set beforehand
				int tempDistance;
				
				Unit enemyUnit = m_EnemyList.get(0);//for (Unit enemyUnit : m_EnemyList)
				{
					tempDistance = Math.abs(unit.getX() - enemyUnit.getX()) + Math.abs(unit.getY() - enemyUnit.getY());
					
					if (tempDistance < m_AttackDistance)
					{
						int directionIdentifier = unitAction.getDirection();
						
						if ((enemyUnit.getX() - unit.getX() < 0 && directionIdentifier == 3/* UnitAction.DIRECTION_LEFT*/)
								|| (enemyUnit.getX() - unit.getX() > 0 && directionIdentifier == 1/* UnitAction.DIRECTION_RIGHT*/)
								|| (enemyUnit.getY() - unit.getY() < 0 && directionIdentifier == 0/* UnitAction.DIRECTION_UP*/)
								|| (enemyUnit.getY() - unit.getY() > 0 && directionIdentifier == 2/* UnitAction.DIRECTION_DOWN*/)) actionScore += m_MoveToAttackWeight;
					}
				}
			}
		}
		return actionScore;
    }
}

