/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import util.Pair;


/**
 *
 * @author Stomps
 */

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
	
	private float m_HarvestWeight;
	private float m_MoveToHarvestWeight;
	private float m_AttackWeight;
	private float m_MoveToAttackWeight;
	private float m_ProduceWeight;
    private int m_AttackDistance;
	
    public int getPlayerUnitDifference() { return m_FriendlyCount - m_EnemyList.size(); }
    public int getEnemyListSize() { return m_EnemyList.size(); }

/*------------------------------------------------------------------------*/   
    
    public Analysis(int playerNumber, GameState gameState, int halfMapDistance, UnitType baseType, UnitType workerType) throws Exception
	{
		m_HalfMapDistance = halfMapDistance;
		m_BaseType = baseType;
		m_WorkerType = workerType;
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
	
	
    public List<Pair<PlayerAction, Float>> AnalyseAndSortActionSpace(PlayerActionGenerator actionGenerator, GameState gameState, long cutOffTime) throws Exception
    {
    	List<Pair<PlayerAction, Float>> actionList = new ArrayList<Pair<PlayerAction, Float>>();
    	PlayerAction playerAction = null;
    	
    	while (gameState.getTime() < cutOffTime)// true)
    	{
    		// Go through all the playerActions generated on construction
    		playerAction = actionGenerator.getNextAction(cutOffTime);
    		
    		if (playerAction != null)
	    	{
    			// Simulate the action into a cloned gameState
	    		GameState simulatedGameState = gameState.cloneIssue(playerAction);
	    		
	    		// Store as a new Pair in the actionList
	    		actionList.add(new Pair<>(playerAction, analyseAction(simulatedGameState, playerAction, cutOffTime)));
    		}
    		// if null then no more actions to process
    		else break;
    	}
    	
    	// Heap to avoid sort
    	
    	// Sorted set here
    	// Write own clone function that overwites not creates new
    	
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
    	
   // 	actionList.add(new Pair<>(null, 0.0f));
    	
		return actionList;
    }
	
    public float analyseAction(GameState gameState, PlayerAction potentialAction, long cutOffTime)
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
		
		// Sanity check as this can become a time expensive function
		while (gameState.getTime() < cutOffTime)
		{
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
		return actionScore;
    }
    
}
