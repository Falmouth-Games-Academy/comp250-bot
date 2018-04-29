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
    int whoTrains=0;
    int basePosX;
    int basePosY;
    boolean workerDefend = true;
    boolean inPos=false;
    int workersSize= 5;
    
    //List all my Units
    List<Unit> workers = new LinkedList<Unit>();
    List<Unit> ranged = new LinkedList<Unit>();
    List<Unit> heavy = new LinkedList<Unit>();
    List<Unit> light = new LinkedList<Unit>();
    Unit base;
    Unit barracks;
    
    //List all Enemy Units
    
    List<Unit> e_workers = new LinkedList<Unit>();
    List<Unit> e_ranged = new LinkedList<Unit>();
    List<Unit> e_heavy = new LinkedList<Unit>();
    List<Unit> e_light = new LinkedList<Unit>();
    Unit e_base;
    Unit e_barracks;
    
    

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
        clear();
        

        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	barracks=u;
            	barracksBehavior(barracks, p, pgs);
            }
            else if (u.getType() == barracksType
                    && u.getPlayer() != player) {
            	e_barracks=u;
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	if(u.getType()==rangedType) {
            		ranged.add(u);
            	}
            	else if(u.getType()==heavyType) {
            		heavy.add(u);
            	}
            	else if(u.getType()==lightType) {
            		light.add(u);
            	}
            	meleeUnitBehavior(u, p, pgs);
            }
            else if(u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() != player) {
            	if(u.getType()==rangedType) {
            		e_ranged.add(u);
            	}
            	else if(u.getType()==heavyType) {
            		e_heavy.add(u);
            	}
            	else if(u.getType()==lightType) {
            		e_light.add(u);
            	}
            }
        }

        // behavior of workers:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest
                    && u.getPlayer() == player) {
                workers.add(u);
            }
            else if(u.getType().canHarvest
                    && u.getPlayer() != player) {
            	e_workers.add(u);
            }
        }
        if (e_workers.size()>workers.size()) {
        	workersSize++;
        }
        //Do behaviors after Units have been listed
     // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
            	base=u;
            	baseBehavior(u, p, pgs);
                
            }
            else if (u.getType() == baseType
                    && u.getPlayer() != player) {
            	e_base=u;
            }
            
        }
        workersBehavior(workers, p, pgs);


        return translateActions(player, gs);
    }
  
    public void clear() {
    	//Clear the list every tick
    	workers.clear();
    	ranged.clear();
        heavy.clear();
        light.clear();
        base=null;
        barracks=null;
        
        e_workers.clear();
    	e_ranged.clear();
        e_heavy.clear();
        e_light.clear();
        e_base=null;
        e_barracks=null;
    }
   

    public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        basePosX = u.getX();
        basePosY = u.getY();
        //System.out.println(basePosY);
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }

        if (nworkers < workersSize && p.getResources() >= workerType.cost) {
            train(u, workerType);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (p.getResources() >= rangedType.cost) {
        	System.out.println(whoTrains);
        	if(whoTrains==0) {
        		train(u, rangedType);
        		whoTrains++;
        	}
        	else if(whoTrains==1) {
        		train(u,lightType);
        		whoTrains++;
        	}
        	else if(whoTrains==2){
        		train(u,heavyType);
        		whoTrains=0;
        	}
            
        }
    }

    public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs) {
    	Unit closestEnemy = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy!=null) {
            attack(u,closestEnemy);
        }
    }

    public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
        int nbases = 0;
        int nbarracks = 0;
        int resourcesUsed = 0;
     
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
        if (nbases == 0 && !workers.isEmpty()) {
            // build a base:
            if (p.getResources() >= baseType.cost + resourcesUsed) {
                Unit u = workers.remove(0);

                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed += baseType.cost;

                
            }
        }

        if (nbarracks == 0 && !workers.isEmpty()) {
            // build a barracks:
            if (p.getResources() >= barracksType.cost+ 3 + resourcesUsed) {
                Unit u = workers.remove(1);
                int brPosX = basePosX-1;
                int brPosY = basePosY+2;
                //If player to build barracks in opposite corner
                if(p.getID()==1) {
                	brPosX = basePosX+2;
                    brPosY = basePosY;
                }
                buildIfNotAlreadyBuilding(u,barracksType,brPosX,brPosY,reservedPositions,p,pgs);
                resourcesUsed += barracksType.cost;

            }
        }
        
        // Defend with units
        
        	
        

        // harvest with first two workers
        int harvesterCount=0;
        
        for (Unit u : workers) {
        	if(harvesterCount<2) {
        		harvesterCount++;
        		
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
        	else{
        		meleeUnitBehavior(u, p, pgs);
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