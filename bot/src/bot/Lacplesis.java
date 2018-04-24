/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.*;
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
 * @author Roberts
 */
public class Lacplesis extends AbstractionLayerAI {

    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType lightType;
    
    int basePosX;
    int basePosY;
    boolean workerDefend = false;
    boolean inPos=false;
    int nDefenders= 0;
    

    // If we have any "light": send it to attack to the nearest enemy unit
    // If we have a base: train worker until we have 1 workers
    // If we have a barracks: train light
    // If we have a worker: do this if needed: build base, build barracks, harvest resources
    public Lacplesis(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }


    public Lacplesis(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
    	super.reset();
    }
    
    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
    }

    public AI clone() {
        return new Lacplesis(utt, pf);
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
//        System.out.println("LightRushAI for player " + player + " (cycle " + gs.getTime() + ")");

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
        List<Unit> defWorkers = new LinkedList<Unit>();
        int dWrkCount=0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                if(dWrkCount<1) {
                	defWorkers.add(u);
                	dWrkCount++;
                }
                else {
                	workers.add(u);
                }
            	
            }
        }
        workersBehavior(workers,defWorkers, p, pgs);


        return translateActions(player, gs);
    }

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        basePosX = u.getX();
        basePosY = u.getY();
        //System.out.println(basePos);
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }

        if (nworkers < 2 && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (p.getResources() >= rangedType.cost) {
            train(u, rangedType);
            
        }
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        int mybase = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        else if(u2.getPlayer()==p.getID() && u2.getType() == baseType)
            {
                mybase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }
        if (closestEnemy!=null && (closestDistance < pgs.getHeight()/2 || mybase < pgs.getHeight()/2)) {
            attack(u,closestEnemy);
        }
        else
        {
            attack(u, null);
        }
    }

    public void workersBehavior(List<Unit> workers,List<Unit> defWorkers, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;

        int resourcesUsed = 0;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        List<Unit> defendWorkers = new LinkedList<Unit>();
        if (nDefenders == 1) {
        	workerDefend = true;
        }
     
        if (workers.isEmpty()) {
            return;
        }
        else {
        	defendWorkers.addAll(defWorkers);
        	freeWorkers.addAll(workers);
//        	for (Unit wrk : workers) {
//        		if (workerDefend==false) {
//        			defendWorkers.add(wrk);
//        			nDefenders++;
//        		}
//        		else if(inPos==true && defendWorkers.isEmpty()!= true) {
//        			if(defendWorkers.get(0)!=wrk) {
//        				System.out.println("added");
//        				freeWorkers.add(wrk);
//        			}
//        		}
//        	}
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

        if (nbarracks == 0 && !freeWorkers.isEmpty()) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost+ 2 + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;
            }
        }
        
        // Defend with units
        
        for (Unit def: defendWorkers ) {
        	

        	if(inPos==false) {
        		move(def, basePosX+1, basePosY-1);
        		
        		if(def.getX() == basePosX+1 && def.getY() == basePosY-1) {
        			inPos=true;
        			System.out.println("In Position!");
        		}
        		
        	}
        	else {
        		Unit closestEnemy = null;
                int closestDistance = 0;
                int mybase = 0;
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                        int d = Math.abs(u2.getX() - def.getX()) + Math.abs(u2.getY() - def.getY());
                        if (closestEnemy == null || d < closestDistance) {
                            closestEnemy = u2;
                            closestDistance = d;
                        }
                    }
//                else if(u2.getPlayer()==p.getID() && u2.getType() == baseType)
//                    {
//                        mybase = Math.abs(u2.getX() - def.getX()) + Math.abs(u2.getY() - def.getY());
//                    }
                }
                if (closestEnemy!=null && closestDistance < 2) {
                    attack(def,closestEnemy);
                }
                else
                {
                    attack(def, null);
                }
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