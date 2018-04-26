/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

/**
 *
 * @author newtoto
 */
public class ShowMeWhatYouBot extends AbstractionLayerAI {
	
	Random r = new Random();
	protected UnitTypeTable utt;
	// Static units
	UnitType baseType;
	UnitType barracksType;
	UnitType resourceType;
	// Mobile units
	UnitType workerType;
	UnitType rangedType;
	UnitType lightType;
	UnitType heavyType;
	
    public ShowMeWhatYouBot(UnitTypeTable a_utt) {
    	this(a_utt, new AStarPathFinding());
    }
    
    public ShowMeWhatYouBot(UnitTypeTable utt, AStarPathFinding aStarPathFinding) {
    	super(aStarPathFinding);
    	reset(utt);
    }
    
    public void reset() {
    	super.reset();
    }
    
    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        // Add static units to type table
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        resourceType = utt.getUnitType("Resource");
        // Add mobile units to type table
        workerType = utt.getUnitType("Worker");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
    }
    
    public AI clone() {
        return new ShowMeWhatYouBot(utt);
    }
   
    // This is what gets executed during the game
    public PlayerAction getAction(int playerNumber, GameState gameState) {
    	PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
    	Player player = gameState.getPlayer(playerNumber);
    	
    	//System.out.println(gameInfo.get(baseType));	
    	
    	// TODO super list of all unit lists
    	//List<List<Unit>> myUnits = new ArrayList<List<Unit>>();
    	
    	// Create unit lists
        List<Unit> workers = new LinkedList<Unit>();
        List<Unit> light = new LinkedList<Unit>();
        List<Unit> heavy = new LinkedList<Unit>();
        List<Unit> ranged = new LinkedList<Unit>();
        // Populate unit lists
        AddAllUnitsToLists(workers, light, heavy, ranged, playerNumber, physicalGameState);
        
        // Create map of unit numbers
    	Map <UnitType, Integer> gameInfo = new HashMap<UnitType, Integer>();
    	// Fill game information using unit lists
    	gameInfo.put(workerType, workers.size());
    	gameInfo.put(lightType, light.size());
    	gameInfo.put(heavyType, heavy.size());
    	gameInfo.put(rangedType, ranged.size());
    	
    	// Get info for remaining units
    	GetGameInfo(gameInfo, playerNumber, physicalGameState);
        
    	// Move workers
        WorkerController(workers, player, physicalGameState);
        
        return translateActions(playerNumber, gameState);
    }
    
    // Increases map value by 1
    public void IncrementMapValue(Map <UnitType, Integer> map, UnitType key) 
    {
    	map.replace(key, map.get(key) + 1);
    }
    
    // Fill map with number or each unit I own
    public void GetGameInfo(Map <UnitType, Integer> gameInfo, int playerNumber, PhysicalGameState physicalGameState) 
    {
    	// Fill map with 0 values for unknown unit values
    	gameInfo.put(baseType, 0);
    	gameInfo.put(barracksType, 0);
    	gameInfo.put(resourceType, 0);
    	
    	for (Unit unit : physicalGameState.getUnits()) 
    	{
    		// Get unit type
			UnitType unitType = unit.getType();
			
    		// Check unit is mine
    		if(unit.getPlayer() == playerNumber) 
    		{
    			// Add my bases to gameInfo
    			if(unitType == baseType) 
    			{
    				IncrementMapValue(gameInfo, baseType);
    			}
    			// Add my barracks to gameInfo
    			else if(unitType == barracksType) 
    			{
    				IncrementMapValue(gameInfo, barracksType);
    			}
    		}
    		else 
    		{
    			// Add resource number to gameInfo
    			if(unitType == resourceType)
    			{
    				IncrementMapValue(gameInfo, resourceType);
    			}
    		}
    	}
    }
    
    // Adds all my units to their respective lists
    public void AddAllUnitsToLists(List<Unit>workers, List<Unit>light, List<Unit> heavy, List<Unit> ranged, int playerNumber, PhysicalGameState physicalGameState) 
    {
    	for (Unit unit : physicalGameState.getUnits()) 
    	{
    		// Check if they are my units
    		if(unit.getPlayer() == playerNumber) 
    		{
    			// Get unit type
    			UnitType unitType = unit.getType();
    			
    			// Add harvesting workers
    			if(unitType.canHarvest) 
    			{
    				workers.add(unit);
    			}
    			// Add light units
    			else if(unitType == lightType) 
    			{
    				light.add(unit);
    			}
    			// Add heavy units
    			else if(unitType == heavyType)
    			{
    				heavy.add(unit);
    			}
    			// Add ranged units
    			else if(unitType == ranged)
    			{
    				ranged.add(unit);
    			}
    		}
    	}
    }
    
    // __START OF UNIT CONTROLLERS__
    // Control the workers
    public void WorkerController(List<Unit> workers, Player player, PhysicalGameState physicalGameState)
	{
    	
		//List<Unit> freeWorkers = new LinkedList<Unit>();
		//freeWorkers.addAll(workers);
		
		if(workers.isEmpty()) {return;}
		
		for (Unit u : workers)
		{
			Unit closestBase = null;
			Unit closestResource = null;
			int closestDistance = 0;
			for(Unit u2 : physicalGameState.getUnits())
			{
				if(u2.getType().isResource)
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(closestResource == null || d < closestDistance)
					{
						closestResource = u2;
						closestDistance = d;
					}
				}
			}
			closestDistance = 0;
			for(Unit u2 : physicalGameState.getUnits())
			{
				if(u2.getType().isStockpile && u2.getPlayer() == player.getID())
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(closestBase == null || d < closestDistance)
					{
						closestBase = u2;
						closestDistance = d;
					}
				}
			}
			
			if(closestResource != null && closestBase != null)
			{
				AbstractAction aa = getAbstractAction(u);
				if(aa instanceof Harvest)
				{
					Harvest h_aa = (Harvest)aa;
					if(h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {harvest (u, closestResource, closestBase);}
				}else {harvest(u, closestResource, closestBase);}
			}
		}
		
	}
    // __END OF UNIT CONTROLLERS__
    
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
