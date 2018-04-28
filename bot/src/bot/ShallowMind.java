/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;

import ai.core.AI;
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
public class ShallowMind extends AbstractionLayerAI {    
	
	protected UnitTypeTable utt;
	
	UnitType workerType;
	UnitType rangedType;
	UnitType heavyType;
	
	UnitType baseType;
	UnitType barracksType;
	
	static int workerLimit = 7;
	static int fightingUnitsBeforeAttack = 4;
	static int resourceWorkerAmount = 2;
	static int attackDistance = 4;
	static int distanceFromBase = 2;
	static int resourceCheckDistance = 6;
	static int resourcesBeforeBarracks = 7;
	static int mapSize  = 0;
	
	List<Integer> oldWorkerLocationX = new ArrayList<Integer>();
	List<Integer> oldWorkerLocationY = new ArrayList<Integer>();
	
	boolean readyForAttack = false;
	boolean builtBarracks = false;
	boolean troopTrainTypeToggle = true;
	
	Random r = new Random();
	
    public ShallowMind(UnitTypeTable a_utt) {
    	this(a_utt, new AStarPathFinding());
    }
    

    public ShallowMind(UnitTypeTable a_utt, PathFinding a_pf) {
    	super(a_pf);
        reset(a_utt);
    }
    
    
    @Override
    public void reset(UnitTypeTable a_utt) {
    	 utt = a_utt;
         if (utt!=null) {
             workerType = utt.getUnitType("Worker");
             baseType = utt.getUnitType("Base");
             barracksType = utt.getUnitType("Barracks");
             rangedType = utt.getUnitType("Ranged");
             heavyType = utt.getUnitType("Heavy");
         }
    }

    
    @Override
    public AI clone() {
        return new ShallowMind(utt, pf);
    }
   
    
    @Override
    public PlayerAction getAction(int player, GameState gs) {
    	
    		PhysicalGameState pgs = gs.getPhysicalGameState();
        	Player p = gs.getPlayer(player);
        	mapSize = pgs.getWidth();
        	int resourceAmount = 0;
        	int fightingUnits = 0;
        	int enemyWorkers = 0;
        	int currentWorkersAllowed = 0;
        	
        	// Checks how many resources are on map
        	 resourceAmount = checkNearResources(p, pgs);        
             if (resourceAmount != 0) resourceWorkerAmount = 2;

        	
            // check enemy workers
    		for(Unit unit:pgs.getUnits()) {
                if  (unit.getType().canHarvest && unit.getPlayer()>=0 && unit.getPlayer()!=p.getID()) { 
                	enemyWorkers ++;
                }
    		}
    		currentWorkersAllowed = enemyWorkers + 1;
    		if (currentWorkersAllowed >= workerLimit) currentWorkersAllowed = workerLimit;
    		
    	      // check how many fighting units
            for(Unit unit : pgs.getUnits()) {
         	   if ((unit.getType()==rangedType || unit.getType()==heavyType) && 
 		                unit.getPlayer() == player) {
         	   			fightingUnits ++;
         	   }
            }
            
            
            if (fightingUnits >= fightingUnitsBeforeAttack) readyForAttack = true;
            if (fightingUnits <= 1) readyForAttack = false;
            if (fightingUnits == 3) troopTrainTypeToggle = false;
            else troopTrainTypeToggle = true;

            
           
      

           // fighting units
           for(Unit unit : pgs.getUnits()) {
        	   if ((unit.getType()==rangedType || unit.getType()==heavyType) && 
		                unit.getPlayer() == player && 
		                gs.getActionAssignment(unit)==null) {
        		   		fightingUnitBehaviour(unit,p,gs);
        	   }
           }
           
       	   // barracks
           for (Unit unit : pgs.getUnits()) {
               if (unit.getType() == barracksType
                       && unit.getPlayer() == player
                       && gs.getActionAssignment(unit) == null) {
                   barracksBehaviour(unit, p, pgs);
                   builtBarracks = true;
               }
           }
           
            List<Unit> workers = new LinkedList<Unit>();
            for(Unit unit:pgs.getUnits()) {
                if (unit.getType().canHarvest && 
                	unit.getPlayer() == player) {
                    workers.add(unit);
                }        
            }
            
         // Bases
            if (workers.size() < currentWorkersAllowed) {
		    	for(Unit unit : pgs.getUnits()) {
		            if (unit.getType()==baseType && 
		                unit.getPlayer() == player && 
		                gs.getActionAssignment(unit)==null) {
		                baseBehaviour(unit,p,pgs);
		            }
		    	}
            }
            
            // Check for worker movement
           
            	int index = 0;
	            for (Unit worker : workers) {
	            	 if (oldWorkerLocationX.size() == index) {
	            		 oldWorkerLocationX.add(-1);
	            		 oldWorkerLocationY.add(-1);
	            	 }
	            	 
	            	 
	            	 if (worker.getX() == oldWorkerLocationX.get(index) && worker.getY() == oldWorkerLocationY.get(index))
	            	 {
	            		 moveRandomDirection(worker, p, pgs);
	            	 }
	         	 	oldWorkerLocationX.set(index, worker.getX());
	        	 	oldWorkerLocationY.set(index, worker.getY());
	        	 	index ++;
	            }

            
            workersBehaviour(workers,p,gs);
            return translateActions(player,gs);
        }
    
    
    public void workersBehaviour(List<Unit> workers, Player p, GameState gs) {
    	 PhysicalGameState pgs = gs.getPhysicalGameState();
    	 Unit harvestWorker = null;
    	 
    	 List<Unit> freeWorkers = new LinkedList<Unit>();
    	 List<Unit> resourceWorkers = new LinkedList<Unit>();
    	 List<Integer> reservedPositions = new LinkedList<Integer>();
    	 
    	 freeWorkers.addAll(workers);
    	 
    	 if (workers.isEmpty()) return;
    	 
    	 
    	 for (int x = 0; x <resourceWorkerAmount; x++) {
	    	 if (resourceWorkers.size() < resourceWorkerAmount) 	    	 {
	    		 if (freeWorkers.size()>0) harvestWorker = (freeWorkers.remove(0));
	    		 	resourceWorkers.add(harvestWorker);
	    	 }
    	 }
    	 // assigns resource workers
    	 for(Unit unit:resourceWorkers) {
        	 // assigns build worker
    		 if (p.getResources() >= resourcesBeforeBarracks && builtBarracks == false) {
     			int YPos = 0;
     			int XPos = 0;
     			
    			if (p.getID() == 0) YPos += 3;
    			else {
    				YPos = mapSize - 2;
    				XPos = mapSize - 1;
    			}
    			//System.out.println(XPos + " " + YPos);
    		 	buildIfNotAlreadyBuilding(unit, barracksType, XPos, YPos, reservedPositions, p, pgs);
        	 }
    		 else {
        		 workerHarvest(unit, p, pgs);
    		 }
    	 }
    	 
    	
    	 // Tells free workers to attack
    	 for (Unit unit:freeWorkers) {
    		 fightingUnitBehaviour(unit, p, gs);
    	 }
	}

	public void fightingUnitBehaviour(Unit unit, Player p, GameState gs) {
		  PhysicalGameState pgs = gs.getPhysicalGameState();
	        Unit closestEnemy = null;
	        int closestDistance = 0;
	        for(Unit enemyUnit:pgs.getUnits()) {
	            if (enemyUnit.getPlayer()>=0 && enemyUnit.getPlayer()!=p.getID() && enemyUnit.getType() != baseType) { 
	                int d = Math.abs(enemyUnit.getX() - unit.getX()) + Math.abs(enemyUnit.getY() - unit.getY());
	                if (closestEnemy==null || d<closestDistance) {
	                    closestEnemy = enemyUnit;
	                    closestDistance = d;
	                }
	            }
	        }
	        if (closestEnemy==null) {
	        	 for(Unit enemyBase:pgs.getUnits()) {
	 	            if (enemyBase.getPlayer()>=0 && enemyBase.getPlayer()!=p.getID() && enemyBase.getType() == baseType) {
	 	            	closestEnemy = enemyBase;
	 	            }
	        }
	        }
	        // attack enemy when close
	        if (closestDistance <= attackDistance || resourceWorkerAmount == 0 || readyForAttack) {
		        if (closestEnemy!=null) {
		            attack(unit,closestEnemy);
	        }
	        }
	        else {
	        	
	        	// Moves away from base
	        	int xSpot = 0;
	        	int ySpot = 0;
	        	for (Unit baseUnit:pgs.getUnits()) {
	        		if (baseUnit.getType()==baseType && 
	        				baseUnit.getPlayer() == p.getID()) {
	        			if ((closestEnemy.getX() - unit.getX()) != 0) xSpot = baseUnit.getX() + distanceFromBase * ((closestEnemy.getX() - unit.getX()) / Math.abs(closestEnemy.getX() - unit.getX()));
	        			if ((closestEnemy.getY() - unit.getY()) != 0) ySpot = baseUnit.getY() + distanceFromBase * ((closestEnemy.getY() - unit.getY()) / Math.abs(closestEnemy.getY() - unit.getY()));			
	        		}
	        	}
	        	// Random movement so they don't get stuck
	        		
	        	if (r.nextBoolean()) {
	        		if (r.nextBoolean()) { 
	        			xSpot += 1;
	        			if (r.nextBoolean()) xSpot += 1; 
	        			}
	        		else {
	        			xSpot -= 1;
	        			if (r.nextBoolean()) xSpot -= 1; 
	        			}
	        		}
	        	if (r.nextBoolean()) {
	        		if (r.nextBoolean()) {
	        			ySpot += 1;
	        			if (r.nextBoolean()) ySpot += 1;
	        		}
	        		else {
	        			ySpot -= 1;
	        			if (r.nextBoolean()) ySpot += 1;
	        		}
	        	}
	        	
	        	move(unit,xSpot, ySpot);
	        }
	}
    
	public void moveRandomDirection(Unit unit, Player p, PhysicalGameState pgs) {
		
    	int xSpot = 0;
    	int ySpot = 0;
    	
		xSpot = unit.getX();
		ySpot = unit.getY();
		
		if (r.nextBoolean()) {
    		if (r.nextBoolean()) xSpot += 2;
    		else xSpot -= 2;
    	}
		else {
    		if (r.nextBoolean()) ySpot += 2;
    		else ySpot -= 2;
    	}
		move(unit,xSpot, ySpot);
	}
	
	public void baseBehaviour(Unit u, Player p, PhysicalGameState pgs) {

    if (p.getResources()>=workerType.cost) train(u, workerType);
	}
	
	public void barracksBehaviour(Unit u, Player p, PhysicalGameState pgs) {
		//System.out.println(u.getX() + " " + u.getY());
        if (troopTrainTypeToggle) {
			if (p.getResources() >= rangedType.cost) {
	            train(u, rangedType);
	            //troopTrainTypeToggle = true;
	        }
        }
        else {
        	if (p.getResources() >= heavyType.cost) {
                train(u, heavyType);
                //troopTrainTypeToggle = true;
        	}
        }
    }
	
	public void workerHarvest (Unit unit, Player p, PhysicalGameState pgs) 	{
		Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getType().isResource) { 
                int d = Math.abs(u2.getX() - unit.getX()) + Math.abs(u2.getY() - unit.getY());
                if (closestResource==null || d<closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestDistance > 4) resourceWorkerAmount = 0;
        closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) { 
                int d = Math.abs(u2.getX() - unit.getX()) + Math.abs(u2.getY() - unit.getY());
                if (closestBase==null || d<closestDistance) {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestResource!=null && closestBase!=null) {
            AbstractAction aa = getAbstractAction(unit);
            if (aa instanceof Harvest) {
                Harvest h_aa = (Harvest)aa;
                if (h_aa.getTarget() != closestResource || h_aa.getBase() !=closestBase) harvest(unit, closestResource, closestBase);
            } else {
                harvest(unit, closestResource, closestBase);
            }
        }
	}
	
	public int checkNearResources(Player p, PhysicalGameState pgs) {
        int closeToBase = 0;
        for(Unit baseUnit:pgs.getUnits()) {
            if (baseUnit.getType()==baseType && 
    				baseUnit.getPlayer() == p.getID()) { 
		    	for(Unit resource : pgs.getUnits()) {
		    		if (resource.getType().isResource) {
		    			int d = Math.abs(baseUnit.getX() - resource.getX()) + Math.abs(baseUnit.getY() - resource.getY());
		    			if (d<resourceCheckDistance) {
		    				closeToBase += 1;
		    			}
		            }
	            }
    		}
		 }
		return closeToBase;
	}
	
	@Override
    public List<ParameterSpecification> getParameters()     {
        return new ArrayList<>();
    }
    
}
