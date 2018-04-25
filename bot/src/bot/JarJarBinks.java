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

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author santi
 */
public class JarJarBinks extends AbstractionLayerAI {
	Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;
    UnitType lightType;
    UnitType rangedType;
    
    int lightCount;
    int heavyCount;
    int unitCount;
    
    int basePosX;
    int basePosY;
    
    int enemyWorkers = 0;
    
    int mapSize = 0;
    
    int atkRating = 0;
    int enemyAtkRating = 0;

    // Strategy implemented by this class:
    // If we have any "heavy": send it to attack to the nearest enemy unit
    // If we have a base: train worker until we have 1 workers
    // If we have a barracks: train heavy and light
    // If we have a worker: do this if needed: build base, build barracks, harvest resources

    public JarJarBinks(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }
    
    
    public JarJarBinks(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
    	super.reset();
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

    public AI clone() {
        return new JarJarBinks(utt, pf);
    }

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
//        System.out.println("HeavyRushAI for player " + player + " (cycle " + gs.getTime() + ")");   
        
        mapSize = pgs.getWidth() * pgs.getHeight();
        
        if (mapSize == 144) { enemyWorkers = 2; }
        else { enemyWorkers = 2; }
        
        //System.out.println("enemy " + enemyRating(player, gs));
        //System.out.println("me " + playerRating(player, gs));
        
        // Update Ratings
        enemyAtkRating = enemyRating(player, gs);
        atkRating = playerRating(player, gs);
        
        
        
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
        workersBehavior(workers, p, gs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
        
    }
    
    public int enemyRating(int player, GameState gs) {
    	
    	int rating = 0;
    	int workers = 0;
    	
    	 PhysicalGameState pgs = gs.getPhysicalGameState();
         Player p = gs.getPlayer(player);
         
    	// Check enemies
        for(Unit unit:pgs.getUnits()) {
            if  (unit.getPlayer()>=0 && unit.getPlayer()!=p.getID()) 
            {
            	if (unit.getType() == workerType )
            	{
            		rating ++;
            		workers++;
            		//System.out.println("enRat = " + enemyRating);
            	}
            	else if (unit.getType() == lightType)
            	{
            		rating += 2;
            	}
            	else if (unit.getType() == rangedType)
            	{
            		rating += 3;
            	}
            	else if (unit.getType() == heavyType)
            	{
            		rating += 4;
            	}
            }
        }
        if (mapSize != 72)
        {
        	enemyWorkers = workers + 1;
        }
    	
    	return rating;
    }
    
    public int playerRating(int player, GameState gs) {
    	
    	int rating = 0;
    	
    	 PhysicalGameState pgs = gs.getPhysicalGameState();
         Player p = gs.getPlayer(player);
    	
    	// Check enemies
        for(Unit unit:pgs.getUnits()) {
            if  (unit.getPlayer()==p.getID()) 
            {
            	if (unit.getType() == workerType )
            	{
            		rating ++;
            		//System.out.println("enRat = " + enemyRating);
            	}
            	else if (unit.getType() == lightType)
            	{
            		rating += 2;
            	}
            	else if (unit.getType() == rangedType)
            	{
            		rating += 3;
            	}
            	else if (unit.getType() == heavyType)
            	{
            		rating += 4;
            	}
            }
        }
    	
    	return rating;
    }
    
    

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }
        
        
        if (p.getResources() >= workerType.cost && nworkers < enemyWorkers) { // nworkers <= unitCount/3 &&  // nworkers <= atkRating/3 && 
            train(u, workerType);
            // System.out.println("workers = " + enemyWorkers);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	 /* if (p.getResources() >= lightType.cost && lightCount <= heavyCount) 
 	    {
 	        train(u, lightType);
 	        lightCount++;
 	        unitCount++;
 	    }
 	    else if (p.getResources() >= heavyType.cost)
 	    {
 	    	train(u, heavyType);
 	    	heavyCount++;
 	    	unitCount++;
 	    } */
    	 if (p.getResources() >= rangedType.cost) 
  	    {
  	        train(u, rangedType);
  	    }
    	
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        
    	Unit enemyBase = null;
    	Unit Base = null;
    	
    	
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
        
        for(Unit eBase:pgs.getUnits()) {
	            if (eBase.getPlayer()>=0 && eBase.getPlayer()!=p.getID() && eBase.getType() == baseType) {
	            	enemyBase = eBase;
	            }
	    for (Unit baseUnit:pgs.getUnits()) {
	        	if (baseUnit.getType()==baseType && 
	        		baseUnit.getPlayer() == p.getID()) {
	        		Base = baseUnit;
	        	}

        
        if (closestDistance < 4 || atkRating > (enemyAtkRating + 5) || p.getResources() == 0) { // closestEnemy != null
//            System.out.println("HeavyRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
            attack(u, closestEnemy);
        }
        else if (Base != null && enemyBase != null)// if (u.getY() < 7)
        {
        	// Random number between -1 and 3
        	int RanX = ThreadLocalRandom.current().nextInt(-1,3);
        	// if player base is on left side 
        	if (Base.getX() < enemyBase.getX())
			{
        		// if the 
        		if (RanX > 1)
        		{
        			move(u, ( Base.getX() + RanX), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(-1,1));
        		}
        		else
        		{
        			move(u, ( Base.getX() + RanX), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(2,3));
        		}
			}
			else
			{
				if (RanX > 1)
        		{
        			move(u, ( Base.getX() - RanX), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(-1,1));
        		}
        		else
        		{
        			move(u, ( Base.getX() - RanX), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(2,3));
        		}
			}
        	}
	    }
        }
        	
        /* else
        {
        	move(u, ( 13 - ThreadLocalRandom.current().nextInt(-1,3)), 11);
        }*/
    }

    public void workersBehavior(List<Unit> workers, Player p, GameState gs) {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	
    	int nbases = 0;
        int nbarracks = 0;
        int maxResourceWorkers = 2;
        int resourceWorkers = 0;

        if (mapSize == 72)
        {
        	maxResourceWorkers = 5;
        }
        
        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> defenceWorkers = new LinkedList<Unit>();
        //freeWorkers.addAll(workers);
        
        for (Unit u : workers)
        {
        	if (resourceWorkers < maxResourceWorkers)
        	{
        		freeWorkers.add(u);
        		resourceWorkers++;
        		//System.out.println("workers = " + resourceWorkers);
        	}
        	else
        	{
        		defenceWorkers.add(u);
        	}
        }
        
        
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

        if (nbarracks == 0 && enemyWorkers < defenceWorkers.size() + 3) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
            	resourcesUsed += barracksType.cost;
            }
        }
        
        for (Unit unit : defenceWorkers) 
        {
        	workerUnitDefence(unit, p, gs);
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
    
    public void workerUnitDefence(Unit u, Player p, GameState gs) {    	
    	
    	Unit enemyBase = null;
    	Unit Base = null;
    	
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
        
        for(Unit eBase:pgs.getUnits()) {
            if (eBase.getPlayer()>=0 && eBase.getPlayer()!=p.getID() && eBase.getType() == baseType) {
            	enemyBase = eBase;
            }
            }
        for (Unit baseUnit:pgs.getUnits()) {
        	if (baseUnit.getType()==baseType && 
        		baseUnit.getPlayer() == p.getID()) {
        		Base = baseUnit;
        	}
        	}
        
        /*if (closestEnemy!=null) 
        {
            attack(u,closestEnemy);
        }*/
        
        if (closestDistance <= 7) 
        {
	        if (closestEnemy!=null) 
	        {
	            attack(u,closestEnemy);
	        }
        }
        else if (Base != null && enemyBase != null)// if (u.getY() < 7)
        {
        	// Random number between -1 and 3
        	int RanX = ThreadLocalRandom.current().nextInt(-2,5);
        	// if player base is on left side 
        	if (Base.getX() < enemyBase.getX())
			{
        		// if the 
        		if (RanX > 1)
        		{
        			move(u, ( Base.getX() + RanX), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(-1,3));
        		}
        		else
        		{
        			move(u, ( Base.getX() + RanX), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(3,5));
        		}
			}
			else
			{
				if (RanX > 1)
        		{
        			move(u, ( Base.getX() - RanX), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(-1,3));
        		}
        		else
        		{
        			move(u, ( Base.getX() - RanX), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(3,5));
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
