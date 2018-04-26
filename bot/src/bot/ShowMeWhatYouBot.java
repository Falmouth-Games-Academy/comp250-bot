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
    	
    	// Flag for the base to tell the barracks to start building
    	boolean enoughWorkers = false;
    	
    	//System.out.println(gameInfo.get(baseType));	
    	
    	// Initialise map of unit lists
    	Map <String, List<Unit>> friendlyUnits = new HashMap<String, List<Unit>>();
    	// Create unit lists and add to friendlyUnit map
    	// Static units
    	List<Unit> bases = new LinkedList<Unit>();
    	friendlyUnits.put("bases", bases);
    	List<Unit> barracks = new LinkedList<Unit>();
    	friendlyUnits.put("barracks", barracks);
    	List<Unit> resources = new LinkedList<Unit>();
    	friendlyUnits.put("resources", resources);
    	// Mobile units
        List<Unit> workers = new LinkedList<Unit>();
        friendlyUnits.put("workers", workers);
        List<Unit> light = new LinkedList<Unit>();
        friendlyUnits.put("light", light);
        List<Unit> heavy = new LinkedList<Unit>();
        friendlyUnits.put("heavy", heavy);
        List<Unit> ranged = new LinkedList<Unit>();
        friendlyUnits.put("ranged", ranged);
        
        // Populate unit lists
        AddUnitsToLists(friendlyUnits, playerNumber, physicalGameState);
        
        // Create map of unit numbers
    	Map <UnitType, Integer> gameInfo = new HashMap<UnitType, Integer>();
    	// Fill game information using unit lists
    	gameInfo.put(baseType, bases.size());
    	gameInfo.put(barracksType, barracks.size());
    	gameInfo.put(resourceType, resources.size());
    	gameInfo.put(workerType, workers.size());
    	gameInfo.put(lightType, light.size());
    	gameInfo.put(heavyType, heavy.size());
    	gameInfo.put(rangedType, ranged.size());
    	
        
        // Control base
        BaseController(bases, gameInfo, enoughWorkers);
        
        // Prioritise creating enough workers
        if(enoughWorkers == true) {
        	// Control barracks (must be done after base)
            BarracksController(barracks, gameInfo, enoughWorkers);
        }
        
        // Move workers
        WorkerController(workers, resources, bases, gameInfo, player, physicalGameState);
        
        return translateActions(playerNumber, gameState);
    }
    
    // Increases map value by 1
    public void IncrementMapValue(Map <UnitType, Integer> map, UnitType key) 
    {
    	map.replace(key, map.get(key) + 1);
    }
    
    // Adds all my units to their respective lists
    public void AddUnitsToLists(Map <String, List<Unit>> friendlyUnits, int playerNumber, PhysicalGameState physicalGameState) 
    {
    	
    	for (Unit unit : physicalGameState.getUnits()) 
    	{
    		// Get unit type
			UnitType unitType = unit.getType();
			
    		// Check if they are my units
    		if(unit.getPlayer() == playerNumber) 
    		{	
    			// Add base
    			if(unitType == baseType) 
    			{
    				friendlyUnits.get("bases").add(unit);
    			}
    			// Ass barracks
    			else if(unitType == barracksType) 
    			{
    				friendlyUnits.get("barracks").add(unit);
    			}
    			// Add harvesting workers
    			else if(unitType.canHarvest) 
    			{
    				friendlyUnits.get("workers").add(unit);
    			}
    			// Add light units
    			else if(unitType == lightType)
    			{
    				friendlyUnits.get("light").add(unit);
    			}
    			// Add heavy units
    			else if(unitType == heavyType)
    			{
    				friendlyUnits.get("heavy").add(unit);
    			}
    			// Add ranged units
    			else if(unitType == rangedType)
    			{
    				friendlyUnits.get("ranged").add(unit);
    			}
    		}
    		else 
    		{
    			if(unitType == resourceType) 
    			{
    				friendlyUnits.get("resources").add(unit);
    			}
    		}
    	}
    }
    
    // __START OF UNIT CONTROLLERS__
    
    // Control base unit production
    public void BaseController(List<Unit> bases,  Map <UnitType, Integer> gameInfo, boolean enoughWorkers)
    {
    	// Return if no bases in list
    	if(bases.isEmpty()) {return;}
    	
    	// Variables for creating workers based on resources
    	int totalFarmableResourcesNumber = gameInfo.get(resourceType);
    	int numberOfWorkers = gameInfo.get(workerType);
    	double targetNumberOfWorkers = Math.ceil(totalFarmableResourcesNumber/2) + 1;
    	
    	
    	// Check if there are enough workers
    	if(numberOfWorkers < targetNumberOfWorkers) {
    		// Create a worker
    		train(bases.get(0), workerType);
    	} 
    	else
    	{
    		// Let barracks train workers
    		enoughWorkers = true;
    		//System.out.println("Enough workers");
    	}
    }
    
 // Control base unit production
    public void BarracksController(List<Unit> barracks,  Map <UnitType, Integer> gameInfo, boolean enoughWorkers)
    {
    	// Return if no bases in list
    	if(barracks.isEmpty()) {return;}
    	
    	// Variables for creating workers based on resources
    	int totalFarmableResourcesNumber = gameInfo.get(resourceType);
    	int numberOfWorkers = gameInfo.get(workerType);
    	double targetNumberOfWorkers = Math.ceil(totalFarmableResourcesNumber/2) + 1;
    	
    	// Variables for creating defensive ranged units
    	int numberOfRanged = gameInfo.get(rangedType);
    	int targetNumberOfRanged = 2;
    	
    	// Check if there are enough workers
    	if(numberOfWorkers < targetNumberOfWorkers) {
    		// Do nothing
    	} 
    	else if(numberOfRanged < targetNumberOfRanged)
    	{
    		System.out.println(numberOfRanged);
    		train(barracks.get(0), rangedType);
    	} 
    	else 
    	{
    		System.out.println("Enough ranged");
    	}
    	
    	
    	//System.out.println(totalFarmableResourcesNumber);
    }
    
    // Control the workers
    public void WorkerController(List<Unit> workers, List<Unit> resources, List<Unit> bases, Map <UnitType, Integer> gameInfo, Player player, PhysicalGameState physicalGameState)
	{
    	
		//List<Unit> freeWorkers = new LinkedList<Unit>();
		//freeWorkers.addAll(workers);
    	
    	// Return if no workers in list
		if(workers.isEmpty()) {return;}
		
		// harvesters is half the number of total resources
		//double numberOfHarvesters = Math.ceil(gameInfo.get(resources));

		//List<Unit> harvesters = new LinkedList<Unit>();
		
		// Tell each worker what to do
		for (Unit worker : workers)
		{
			// Init closest objects
			Unit closestBase = null;
			Unit closestResource = null;
			
			// Init value to store distance value
			int closestDistance = 0;
			
			// Find closes resource
			for(Unit resource : resources)
			{
					int distanceToResource = Math.abs(resource.getX() - worker.getX()) + Math.abs(resource.getY() - worker.getY());
					if(closestResource == null || distanceToResource < closestDistance)
					{
						closestResource = resource;
						closestDistance = distanceToResource;
					}
			}
			
			// Reset distance value
			closestDistance = 0;
			
			// Find closest base
			for(Unit base : bases)
			{
				int distanceToBase = Math.abs(base.getX() - worker.getX()) + Math.abs(base.getY() - worker.getY());
				if(closestBase == null || distanceToBase < closestDistance)
				{
					closestBase = base;
					closestDistance = distanceToBase;
				}
			}
			
			// Harvest resources
			if(closestResource != null && closestBase != null)
			{
				harvest(worker, closestResource, closestBase);
			}
		}
		
	}
    // __END OF UNIT CONTROLLERS__
    
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
