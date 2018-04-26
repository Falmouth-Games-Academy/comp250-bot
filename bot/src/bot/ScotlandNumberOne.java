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
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Steve :) 
 */
public class ScotlandNumberOne extends AbstractionLayerAI {
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
    int rangedCount;
    int unitCount;
    
    int mapSize = 0;


    public ScotlandNumberOne(UnitTypeTable a_utt) 
    {
        this(a_utt, new AStarPathFinding());
    }
    
    
    public ScotlandNumberOne(UnitTypeTable a_utt, PathFinding a_pf)
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
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
        rangedType = utt.getUnitType("Ranged");
    }      

    public AI clone() {
        return new ScotlandNumberOne(utt, pf);
    }

//Main Function Of The Game
    public PlayerAction getAction(int player, GameState gs)
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);   
        
        //Gets size of the map
        mapSize = pgs.getWidth() * pgs.getHeight();
        
        // behavior of bases:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null)
            {
                baseBehavior(u, p, pgs);
            }
        }

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null)
            {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) 
            {
                meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) 
        {
            if (u.getType().canHarvest
                    && u.getPlayer() == player)
            {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, gs);

        // This method simply takes all the unit actions executed so far, and packages them into a PlayerAction
        return translateActions(player, gs);
        
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) 
    {
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) 
            {
                nworkers++;
            }
        }
        if (p.getResources() >= workerType.cost && nworkers < 3) 
        {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs)
    {
    	 if (p.getResources() >= rangedType.cost && rangedCount < heavyCount) 
  	    {
  	        train(u, rangedType);
  	        rangedCount++;
  	        unitCount++;
  	    }
    	 else
    	 {
    		 train(u, heavyType);
    		 heavyCount++;
    		 unitCount++;
    	 }
    	
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) 
    {
        
    	Unit enemyBase = null;
    	Unit Base = null;
    	
    	
    	PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits())
        {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) 
            {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance)
                {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        
        for(Unit eBase:pgs.getUnits()) 
        {
	            if (eBase.getPlayer()>=0 && eBase.getPlayer()!=p.getID() && eBase.getType() == baseType)
	            {
	            	enemyBase = eBase;
	            }
	    for (Unit baseUnit:pgs.getUnits()) 
	    {
	        	if (baseUnit.getType()==baseType && 
	        		baseUnit.getPlayer() == p.getID())
	        	{
	        		Base = baseUnit;
	        	}

        
	        	
	    if (enemyBase == null)
	    {
	    	attack(u, closestEnemy);
	    }
	        	
	    if (p.getResources() <= 5)
	    {
	    	attack(u, enemyBase);
	    	attack(u, closestEnemy);
	    }
	    else if (closestDistance < 6)
        {
            attack(u, closestEnemy);
        }
        else if (Base != null && enemyBase != null)
        {
        	int Ran1 = ThreadLocalRandom.current().nextInt(-2,4);
        	if (Base.getX() < enemyBase.getX())
			{
        		if (Ran1 > 1)
        		{
        			move(u, ( Base.getX() + Ran1), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(-3,3));
        		}
        		else
        		{
        			move(u, ( Base.getX() + Ran1), Base.getY() + 
    						ThreadLocalRandom.current().nextInt(3,4));
        		}
			}
			else
			{
				if (Ran1 > 2)
        		{
        			move(u, ( Base.getX() - Ran1), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(-3,3));
        		}
        		else
        		{
        			move(u, ( Base.getX() - Ran1), Base.getY() - 
    						ThreadLocalRandom.current().nextInt(3,4));
        		}
			}
        	}
	    }
        }
        	
    }

    public void workersBehavior(List<Unit> workers, Player p, GameState gs) {
    	PhysicalGameState pgs = gs.getPhysicalGameState();
        int nbases = 0;
        int nbarracks = 0;
        int harvestingWorkers = 0;
        int maxHarvestingWorkers = 3;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> attackingWorkers = new LinkedList<Unit>();
        //freeWorkers.addAll(workers);
        
        if (mapSize <=64)
        {
        	maxHarvestingWorkers = 1;
        }

        for (Unit u : workers)
        {
        	if (harvestingWorkers < maxHarvestingWorkers)
        	{
        		freeWorkers.add(u);
        		harvestingWorkers++;
        	}
        	else
        	{
        		attackingWorkers.add(u);
        	}
        }
        
        if (workers.isEmpty()) 
        {
            return;
        }

        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType() == baseType
                    && u2.getPlayer() == p.getID()) 
            {
                nbases++;
            }
            if (u2.getType() == barracksType
                    && u2.getPlayer() == p.getID())
            {
                nbarracks++;
            }
        }
        

        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases == 0 && !freeWorkers.isEmpty()) 
        {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed)
            {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;
                
            }
        }
        
        if (nbarracks == 0) 
        {
            // build a barracks:
            if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) 
            {
                Unit u = freeWorkers.remove(0);
                if (u.getPlayer() == 0)
                {
                    buildIfNotAlreadyBuilding(u,barracksType,u.getX()+1,u.getY()+1,reservedPositions,p,pgs);
                	resourcesUsed += barracksType.cost;
                }
                else
                {
                    buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
                	resourcesUsed += barracksType.cost;
                }
                	
            }
        }
        
        for (Unit unit : attackingWorkers) 
        {
        	workerUnitDefence(unit, p, gs);
        }

        
        // harvest with all the free workers:
        for (Unit u : freeWorkers)
        {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isResource) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) 
                    {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits())
            {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) 
                    {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) 
            {
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) 
                {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } 
                else 
                {
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
        
        
        if (closestDistance <= 7) 
        {
        	attack(u,closestEnemy); 

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
