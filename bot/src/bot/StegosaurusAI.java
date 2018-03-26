/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.cRush.RangedAttack;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

/**
 *
 * @author Cristiano D'Angelo
 */
public class StegosaurusAI extends AbstractionLayerAI 
{
    Random rand = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType lightType;

    ArrayList <UnitType> trainingQueue;
    UnitType nextToTrain;
    int trainingQueueElement;

    public StegosaurusAI(UnitTypeTable a_utt) 
    {
        this(a_utt, new FloodFillPathFinding());//AStarPathFinding());
    }

    public StegosaurusAI(UnitTypeTable a_utt, PathFinding a_pf) 
    {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() 
    {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt) 
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
        
        trainingQueue = new ArrayList <UnitType> (Arrays.asList(heavyType, lightType, rangedType, lightType, rangedType));
        nextToTrain = trainingQueue.get(0);
        trainingQueueElement = 0;
    }

    public AI clone() 
    {
        return new StegosaurusAI(utt, pf);
    }

    boolean buildingRacks = false;
    int resourcesUsed = 0;
    
    
    public PlayerAction getAction(int player, GameState gameState) 
    {
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        Player thisPlayer = gameState.getPlayer(player);
        boolean isRush = false;
        
        if ((physicalGameState.getWidth() * physicalGameState.getHeight()) <= 144)
        {
        	// Temporary measure
        	if (physicalGameState.getWidth() != 9 && physicalGameState.getHeight() != 8) isRush = true;
        }
        
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // Maybe use ArrayList for efficiency? - Check
        List <Unit> workers = new LinkedList <Unit> ();
        
        // Fill workers list with player units that can harvest
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType().canHarvest && unit.getPlayer() == player) 
            {
                workers.add(unit);
            }
        }
        
        // Behaviour of workers
        if(isRush)
        {
            rushWorkersBehavior(workers, thisPlayer, physicalGameState, gameState);
        } 
        else 
        {
            workersBehavior(workers, thisPlayer, physicalGameState, gameState);
        }

        // behaviour of bases
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType() == baseType && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) 
            {
                if(isRush)
                {
                    rushBaseBehavior(unit, thisPlayer, physicalGameState);
                }
                else 
                {
                    baseBehavior(unit, thisPlayer, physicalGameState);
                }
            }
        }

        // behaviour of barracks
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType() == barracksType && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) 
            {
                barracksBehavior(unit, thisPlayer, physicalGameState);
            }
        }

        // behaviour of attack units
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType().canAttack && !unit.getType().canHarvest && unit.getPlayer() == player && gameState.getActionAssignment(unit) == null) {
                
            	// Sort out ranged attack from melee attacks
            	if (unit.getType() == rangedType) 
                {
                    rangedUnitBehavior(unit, thisPlayer, gameState);
                } 
                else 
                {
                    meleeUnitBehavior(unit, thisPlayer, gameState);
                }
            }
        }
        return translateActions(player, gameState);
    }

    public void baseBehavior(Unit baseUnit, Player player, PhysicalGameState physicalGameState) 
    {
        int baseCount = 0;
        int barracksCount = 0;
        int workerCount = 0;
        int resources = player.getResources();
        
        // Update counts
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType() == workerType && unit.getPlayer() == player.getID()) 
            {
                workerCount++;
            }
            else if (unit.getType() == barracksType && unit.getPlayer() == player.getID()) 
            {
                barracksCount++;
            }
            else if (unit.getType() == baseType && unit.getPlayer() == player.getID()) 
            {
                baseCount++;
            }
        }
        
        // Train workers
        if (workerCount < (baseCount + 1) && player.getResources() >= workerType.cost) 
        {
            train(baseUnit, workerType);
        }

        // Buffers the resources that are being used for barracks
        if (resourcesUsed != barracksType.cost * barracksCount) 
        {
            resources -= barracksType.cost;
        }
        
        // Train workers
        if (buildingRacks && (resources >= workerType.cost + rangedType.cost)) 
        {
            train(baseUnit, workerType);
        }
    }

    public void barracksBehavior(Unit barracksUnit, Player player, PhysicalGameState playerGameState) 
    {
        if (player.getResources() >= nextToTrain.cost) 
        {
        	// Train unit type in queue
            train(barracksUnit, nextToTrain);
            if (trainingQueueElement + 1 == trainingQueue.size()) trainingQueueElement = 0;
            else trainingQueueElement++;
            nextToTrain = trainingQueue.get(trainingQueueElement);
        }
    }

    public void meleeUnitBehavior(Unit meleeUnit, Player player, GameState gameState) 
    {
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        
        for (Unit unit : physicalGameState.getUnits()) 
        {
        	// If owned by a player and is enemy player
            if (unit.getPlayer() >= 0 && unit.getPlayer() != player.getID()) 
            {
            	// Check the smallest dX + dY against tempVar
                int tempDistance = Math.abs(unit.getX() - meleeUnit.getX()) + Math.abs(unit.getY() - meleeUnit.getY());
                if (closestEnemy == null || tempDistance < closestDistance) 
                {
                    closestEnemy = unit;
                    closestDistance = tempDistance;
                }
            }
        }
        
        // Sanity check.
        if (closestEnemy != null) 
        {
            attack(meleeUnit, closestEnemy);
        }
    }

    public void rangedUnitBehavior(Unit rangedUnit, Player player, GameState gameState) 
    {
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        Unit closestEnemy = null;
        Unit closestBarracks = null;
        int closestDistance = 0;
        
        for (Unit unit : physicalGameState.getUnits()) 
        {
        	// If owned by a player and is enemy player
            if (unit.getPlayer() >= 0 && unit.getPlayer() != player.getID()) 
            {
            	// Check the smallest dX + dY against tempVar
                int tempDistance = Math.abs(unit.getX() - rangedUnit.getX()) + Math.abs(unit.getY() - rangedUnit.getY());
                if (closestEnemy == null || tempDistance < closestDistance) 
                {
                    closestEnemy = unit;
                    closestDistance = tempDistance;
                }
            }
            
            // Why does rangedAttack() need closest barracks?
            if (unit.getType() == barracksType && unit.getPlayer() == player.getID()) 
            {
            	// Check the smallest dX + dY against tempVar
                int tempDistance = Math.abs(unit.getX() - rangedUnit.getX()) + Math.abs(unit.getY() - rangedUnit.getY());
                if (closestBarracks == null || tempDistance < closestDistance) 
                {
                    closestBarracks = unit;
                    closestDistance = tempDistance;
                }
            }
        }
        
        // Sanity check. Why need closest barracks though?
        if (closestEnemy != null) 
        {
            rangedAttack(rangedUnit, closestEnemy, closestBarracks);
        }
    }

    public void workersBehavior(List<Unit> workers, Player player, PhysicalGameState physicalGameState, GameState gameState) 
    {
        int baseCount = 0;
        int barracksCount = 0;
        int workerCount = 0;
        resourcesUsed = 0;
        
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> battleWorkers = new LinkedList<Unit>();
        
        // Update counts
        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType() == baseType && unit.getPlayer() == player.getID()) 
            {
                baseCount++;
            }
            if (unit.getType() == barracksType && unit.getPlayer() == player.getID()) 
            {
                barracksCount++;
            }
            if (unit.getType() == workerType && unit.getPlayer() == player.getID()) 
            {
                workerCount++;
            }
        }

        if (workers.size() > (baseCount + 1)) 
        {
            for (int n = 0; n < (baseCount + 1); n++) 
            {
                freeWorkers.add(workers.get(0));
                workers.remove(0);
            }
            battleWorkers.addAll(workers);
        } 
        else 
        {
            freeWorkers.addAll(workers);
        }

        if (workers.isEmpty()) 
        {
            return;
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (baseCount == 0 && !freeWorkers.isEmpty()) 
        {
            // build a base:
            if (player.getResources() >= baseType.cost) 
            {
                Unit unit = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(unit, baseType, unit.getX(), unit.getY(), reservedPositions, player, physicalGameState);
                //resourcesUsed += baseType.cost;
            }
        }
        if ((barracksCount == 0) && (!freeWorkers.isEmpty()) && workerCount > 1 && player.getResources() >= barracksType.cost) 
        {
            
            int resources = player.getResources();
            Unit u = freeWorkers.remove(0);   
            buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,player,physicalGameState);
            resourcesUsed += barracksType.cost;
            buildingRacks = true;
                
                //The problem with this right now is that we can only track when a build command is sent
                //Not when it actually starts building the building.
        } 
        else 
        {
            resourcesUsed =  barracksType.cost * barracksCount;
        }
        
        if (barracksCount > 1) 
        {
            buildingRacks = true;
        }

        for (Unit unit : battleWorkers) 
        {
            meleeUnitBehavior(unit, player, gameState);
        }

        // harvest with all the free workers:
        for (Unit freeWorkerUnit : freeWorkers) 
        {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit unit : physicalGameState.getUnits()) 
            {
                if (unit.getType().isResource) 
                {
                    int tempDistance = Math.abs(unit.getX() - freeWorkerUnit.getX()) + Math.abs(unit.getY() - freeWorkerUnit.getY());
                    if (closestResource == null || tempDistance < closestDistance) 
                    {
                        closestResource = unit;
                        closestDistance = tempDistance;
                    }
                }
            }
            closestDistance = 0;
            for (Unit unit : physicalGameState.getUnits()) 
            {
                if (unit.getType().isStockpile && unit.getPlayer() == player.getID()) 
                {
                    int tempDistance = Math.abs(unit.getX() - freeWorkerUnit.getX()) + Math.abs(unit.getY() - freeWorkerUnit.getY());
                    if (closestBase == null || tempDistance < closestDistance) 
                    {
                        closestBase = unit;
                        closestDistance = tempDistance;
                    }
                }
            }
            if (closestResource != null && closestBase != null) 
            {
                AbstractAction abstractAction = getAbstractAction(freeWorkerUnit);
                if (abstractAction instanceof Harvest) 
                {
                    Harvest harvestAbstractAction = (Harvest) abstractAction;
                    if (harvestAbstractAction.getTarget() != closestResource || harvestAbstractAction.getBase() != closestBase) 
                    {
                        harvest(freeWorkerUnit, closestResource, closestBase);
                    }
                } 
                else 
                {
                    harvest(freeWorkerUnit, closestResource, closestBase);
                }
            }
        }
    }
    
    
    public void rushBaseBehavior(Unit thisBaseUnit, Player player, PhysicalGameState physicalGameState) 
    {
        if (player.getResources() >= workerType.cost) train(thisBaseUnit, workerType);
    }
    
    public void rushWorkersBehavior(List<Unit> workers, Player player, PhysicalGameState physicalGameState, GameState gameState) 
    {
        int baseCount = 0;
        int workerCount = 0;
        resourcesUsed = 0;
        
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> battleWorkers = new LinkedList<Unit>();

        for (Unit unit : physicalGameState.getUnits()) 
        {
            if (unit.getType() == baseType && unit.getPlayer() == player.getID()) 
            {
                baseCount++;
            }
            if (unit.getType() == workerType && unit.getPlayer() == player.getID()) 
            {
                workerCount++;
            }
        }
        if (player.getResources() == 0)
        {
            battleWorkers.addAll(workers);
        } 
        else if (workers.size() > (baseCount)) 
        {
            for (int n = 0; n < (baseCount); n++) 
            {
                freeWorkers.add(workers.get(0));
                workers.remove(0);
            }
            battleWorkers.addAll(workers);
        } 
        else 
        {
            freeWorkers.addAll(workers);
        }

        if (workers.isEmpty()) 
        {
            return;
        }

        List <Integer> reservedPositions = new LinkedList<Integer>();
        if (baseCount == 0 && !freeWorkers.isEmpty()) 
        {
            // build a base:
            if (player.getResources() >= baseType.cost) 
            {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, player, physicalGameState);
                //resourcesUsed += baseType.cost;
            }
        }
        
        for (Unit battleWorkerUnit : battleWorkers) 
        {
            meleeUnitBehavior(battleWorkerUnit, player, gameState);
        }

        // harvest with all the free workers:
        for (Unit freeWorkerUnit : freeWorkers) 
        {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit unit : physicalGameState.getUnits()) 
            {
                if (unit.getType().isResource) 
                {
                    int tempDistance = Math.abs(unit.getX() - freeWorkerUnit.getX()) + Math.abs(unit.getY() - freeWorkerUnit.getY());
                    if (closestResource == null || tempDistance < closestDistance) 
                    {
                        closestResource = unit;
                        closestDistance = tempDistance;
                    }
                }
            }
            closestDistance = 0;
            for (Unit unit : physicalGameState.getUnits()) 
            {
                if (unit.getType().isStockpile && unit.getPlayer() == player.getID()) 
                {
                    int tempDistance = Math.abs(unit.getX() - freeWorkerUnit.getX()) + Math.abs(unit.getY() - freeWorkerUnit.getY());
                    if (closestBase == null || tempDistance < closestDistance) 
                    {
                        closestBase = unit;
                        closestDistance = tempDistance;
                    }
                }
            }
            if (closestResource != null && closestBase != null) 
            {
                AbstractAction abstractAction = getAbstractAction(freeWorkerUnit);
                if (abstractAction instanceof Harvest) 
                {
                    Harvest harvestAbstractAction = (Harvest) abstractAction;
                    if (harvestAbstractAction.getTarget() != closestResource || harvestAbstractAction.getBase() != closestBase) 
                    {
                        harvest(freeWorkerUnit, closestResource, closestBase);
                    }
                } 
                else 
                {
                    harvest(freeWorkerUnit, closestResource, closestBase);
                }
            }
        }
    }
    
    
    public void rangedAttack(Unit rangedUnit, Unit targetUnit, Unit barracksUnit) 
    {
        actions.put(rangedUnit, new RangedAttack(rangedUnit, targetUnit, barracksUnit, pf));
    }
    
    

    @Override
    public List<ParameterSpecification> getParameters() 
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new FloodFillPathFinding()));//AStarPathFinding()));

        return parameters;
    }
}
