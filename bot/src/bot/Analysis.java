/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.UnitActionAssignment;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
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
    
    private int m_OverflowCheckIterator = 0;
    
    

/*------------------------------------------------------------------------*/   
    PlayerAction lastAction = null;
    int currentChoice[] = null;
    boolean moreActions = true;
    GameState gs;
    PhysicalGameState pgs;
    ResourceUsage base_ru;
    List<Pair<Unit,List<UnitAction>>> choices;
    
    
/*------------------------------------------------------------------------*/   
	
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
	
	
    public List<Pair<PlayerAction, Float>> AnalyseAndSortActionSpace(MyPlayerActionGenerator actionGenerator, GameState gameState, long cutOffTime) throws Exception
    {
    	List<Pair<PlayerAction, Float>> actionList = new ArrayList<Pair<PlayerAction, Float>>();
    	
    	MyPlayerActionGenerator thisActionGenerator = actionGenerator;
    	float playerActionScore;
    	
    	m_OverflowCheckIterator = 0;
    	
    	while (/*m_OverflowCheckIterator < 1 || */gameState.getTime() < cutOffTime)// true)
    	{
    		m_OverflowCheckIterator++;
    		
    		// Go through all the playerActions generated on construction
    		PlayerAction playerAction = thisActionGenerator.getNextAction(cutOffTime);
    		
    		if (playerAction != null)
    		{
	    		for (Pair<Unit, UnitAction> unitPair : playerAction.getActions())
	    		{
	    			if (unitPair.m_a.getPlayer() == m_PlayerNumber)
	    			{
	    				playerActionScore = analyseAction(unitPair.m_a, unitPair.m_b, cutOffTime);
//	    				cleanThreadLocals();
	    				actionList.add(new Pair<>(playerAction, playerActionScore));
	    			}
	    		}
    		}
    		else break;   	
    	}
    	// Heap to avoid sort
    	
    	// Sorted set here
    	// Write own clone function that overwites not creates new
    	
    	if (actionList.size() > 1 && gameState.getTime() < cutOffTime)
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
    	
		return actionList;
    }
	
    public float analyseAction(Unit unit, UnitAction unitAction, long cutOffTime)//GameState gameState, PlayerAction potentialAction, long cutOffTime)
    {
		float actionScore = 0.0f;
        
		// For opening moves. The only action is to create more workers so all good
		// Maybe control the direction in which it is producing workers?
		if (unitAction == null || m_FriendlyCount == 0)//		if (potentialAction == null || m_FriendlyCount == 0)
		{
			return actionScore;
		}

					
	// Look at our player
//		if (potentialAction != null && potentialAction.getAction(unit) != null && gameState.getTime() < cutOffTime)
//		{
		
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
			if (unitAction.getType() == 4)
			{
				actionScore += m_ProduceWeight;
			}
			
			// Check for direct attacking (should always have a non zero weighting)
			if (unitAction.getType() == 5)// UnitAction.TYPE_ATTACK_LOCATION)
			{
				actionScore += m_AttackWeight;
			}
			else if (m_MoveToAttackWeight > 0 /*&& gameState.getTime() < cutOffTime*/)
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

    
    // https://stackoverflow.com/questions/3869026/how-to-clean-up-threadlocals?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
    private void cleanThreadLocals() {
        try {
            // Get a reference to the thread locals table of the current thread
            Thread thread = Thread.currentThread();
            Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalTable = threadLocalsField.get(thread);

            // Get a reference to the array holding the thread local variables inside the
            // ThreadLocalMap of the current thread
            Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = threadLocalMapClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalTable);

            // The key to the ThreadLocalMap is a WeakReference object. The referent field of this object
            // is a reference to the actual ThreadLocal variable
            Field referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);

            for (int i=0; i < Array.getLength(table); i++) {
                // Each entry in the table array of ThreadLocalMap is an Entry object
                // representing the thread local reference and its value
                Object entry = Array.get(table, i);
                if (entry != null) {
                    // Get a reference to the thread local object and remove it from the table
                    ThreadLocal<?> threadLocal = (ThreadLocal<?>)referentField.get(entry);
                    threadLocal.remove();
                }
            }
        } catch(Exception e) {
            // We will tolerate an exception here and just log it
            throw new IllegalStateException(e);
        }
    }
}

