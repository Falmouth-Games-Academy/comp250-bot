/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
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
 * @author santi
 */
public class TheDestroyer extends AbstractionLayerAI {
	private Random rng;
	
	Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;
    UnitType lightType;
    UnitType rangedType;
    
    // Number of units
    public int heavyCount;
    public int lightCount;
    
    public TheDestroyer(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }
    
    
    public TheDestroyer(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }
    
    public void reset(UnitTypeTable a_utt)  
    {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
        rangedType = utt.getUnitType("Ranged");
    }
	
    public TheDestroyer(UnitTypeTable utt, int timebudget) {
    	super(new AStarPathFinding());
    	rng = new Random();
    }
    
    
    @Override
    public void reset() {
    	super.reset();
    }

    
    @Override
    public AI clone() {
    	return new TheDestroyer(utt, pf);
    }
   // lol 
    
	    /*
	    This is the main function of the AI. It is called at each game cycle with the most up to date game state and
	    returns which actions the AI wants to execute in this cycle.
	    The input parameters are:
	    - player: the player that the AI controls (0 or 1)
	    - gs: the current game state
	    This method returns the actions to be sent to each of the units in the gamestate controlled by the player,
	    packaged as a PlayerAction.
	 */
	public PlayerAction getAction(int player, GameState gs) {
		
	    PhysicalGameState pgs = gs.getPhysicalGameState();
	    Player p = gs.getPlayer(player);
	//    System.out.println("HeavyRushAI for player " + player + " (cycle " + gs.getTime() + ")");
	
	    // behavior of bases:
	    for (Unit u : pgs.getUnits()) {
	        if (u.getType() == baseType
	                && u.getPlayer() == player
	                && gs.getActionAssignment(u) == null) {
	            baseBehavior(u, p, pgs);
	        }
	    }
	
	    // behavior of barracks:
	    for (Unit u : pgs.getUnits()) {
	        if (u.getType() == barracksType
	                && u.getPlayer() == player
	                && gs.getActionAssignment(u) == null) {
	            barracksBehavior(u, p, pgs);
	        }
	    }
	
	    // behavior of melee units:
	    for (Unit u : pgs.getUnits()) {
	        if (u.getType().canAttack && !u.getType().canHarvest
	                && u.getPlayer() == player
	                && gs.getActionAssignment(u) == null) {
	            meleeUnitBehavior(u, p, gs);
	        }
	    }
	
	    // behavior of workers:
	    List<Unit> workers = new LinkedList<Unit>();
	    for (Unit u : pgs.getUnits()) {
	        if (u.getType().canHarvest
	                && u.getPlayer() == player) {
	            workers.add(u);
	        }
	    }
	    workersBehavior(workers, p, pgs);
	
	    // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
	    return translateActions(player, gs);
	}
	
	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
	    int nworkers = 0;
	    for (Unit u2 : pgs.getUnits()) {
	        if (u2.getType() == workerType
	                && u2.getPlayer() == p.getID()) {
	            nworkers++;
	        }
	    }
	    if (nworkers < 1 && p.getResources() >= workerType.cost) {
	        train(u, workerType);
	    }
	}
	
	// Alternates between light and heavy units
	
	public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
	    
	    if (p.getResources() >= lightType.cost && lightCount <= heavyCount) 
	    {
	        train(u, lightType);
	        lightCount++; 
	    }
	    else if (p.getResources() >= lightType.cost)
	    {
	    	train(u, heavyType);
	    	heavyCount++;
	    }
	}
	
	public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
	    PhysicalGameState pgs = gs.getPhysicalGameState();
	    Unit closestEnemy = null;
	    int closestDistance = 0;
	    for (Unit u2 : pgs.getUnits()) {
	        if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
	            int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	            if (closestEnemy == null || d < closestDistance) {
	                closestEnemy = u2;
	                closestDistance = d;
	            }
	        }
	    }
	    if (closestEnemy != null) {
	//        System.out.println("HeavyRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
	        attack(u, closestEnemy);
	    }
	}
	
	public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
	    int nbases = 0;
	    int nbarracks = 0;
	
	    int resourcesUsed = 0;
	    List<Unit> freeWorkers = new LinkedList<Unit>();
	    freeWorkers.addAll(workers);
	
	    if (workers.isEmpty()) {
	        return;
	    }
	
	    for (Unit u2 : pgs.getUnits()) {
	        if (u2.getType() == baseType
	                && u2.getPlayer() == p.getID()) {
	            nbases++;
	        }
	        if (u2.getType() == barracksType
	                && u2.getPlayer() == p.getID()) {
	            nbarracks++;
	        }
	    }
	
	    List<Integer> reservedPositions = new LinkedList<Integer>();
	    if (nbases == 0 && !freeWorkers.isEmpty()) {
	        // build a base:
	        if (p.getResources() >= baseType.cost + resourcesUsed) {
	            Unit u = freeWorkers.remove(0);
	            buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
	            resourcesUsed += baseType.cost;
	        }
	    }
	
	    if (nbarracks == 0) {
	        // build a barracks:
	        if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
	            Unit u = freeWorkers.remove(0);
	            buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
	        	resourcesUsed += barracksType.cost;
	        }
	    }
	
	
	    // harvest with all the free workers:
	    for (Unit u : freeWorkers) {
	        Unit closestBase = null;
	        Unit closestResource = null;
	        int closestDistance = 0;
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType().isResource) {
	                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                if (closestResource == null || d < closestDistance) {
	                    closestResource = u2;
	                    closestDistance = d;
	                }
	            }
	        }
	        closestDistance = 0;
	        for (Unit u2 : pgs.getUnits()) {
	            if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
	                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                if (closestBase == null || d < closestDistance) {
	                    closestBase = u2;
	                    closestDistance = d;
	                }
	            }
	        }
	        if (closestResource != null && closestBase != null) {
	            AbstractAction aa = getAbstractAction(u);
	            if (aa instanceof Harvest) {
	                Harvest h_aa = (Harvest)aa;
	                if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
	            } else {
	                harvest(u, closestResource, closestBase);
	            }
	        }
	    }
	}
	
	
	@Override
	public List<ParameterSpecification> getParameters()
	{
	    List<ParameterSpecification> parameters = new ArrayList<>();
	    
	    parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
	
	    return parameters;
	}
    
}
