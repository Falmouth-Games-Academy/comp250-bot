/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;

import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.GreedyPathFinding;
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
 * @author Steve :) 
 */
public class AverageBot extends AbstractionLayerAI {
	//All variables needed
	Random r = new Random();
	
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType lightType;
    
    List<Unit> workerUnitList = new LinkedList<Unit>();
    List<Unit> rangedUnitList = new LinkedList<Unit>();
    List<Unit> heavyUnitList = new LinkedList<Unit>();
    List<Unit> UnitList = new LinkedList<Unit>();
    List<Unit> enemyUnitList = new LinkedList<Unit>();
    
    Unit base = null;
    Unit barracks = null;
    Unit enemyBase = null;
    Unit enemyBarracks = null;
    
	
    
    public AverageBot(UnitTypeTable a_utt) {
    	this(a_utt, new AStarPathFinding());
    }
    
    public AverageBot(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
    	super.reset();
    }
    @Override
    //Assigning units to variables
    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
        lightType = utt.getUnitType("Light");
    }

    
    @Override
    public AI clone() {
        return new AverageBot(null);
    }
   
    // Main Function of AI, called at each cycle
    @Override
    public PlayerAction getAction(int player, GameState gs) {
    	
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
//        System.out.println("HeavyRushAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType
                    && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
                baseBehavior(u, p, pgs);
            }
        }
    	
        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest
                    && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
                meleeUnitBehavior(u, p, gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest && u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers, p, pgs);
        
        return translateActions(player, gs);  
    }
    
    
    private void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
		// TODO Auto-generated method stub
		
	}

	private void meleeUnitBehavior(Unit u, Player p, GameState gs) {
		// TODO Auto-generated method stub
		
	}

	private void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
		// TODO Auto-generated method stub
		
	}

	private void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
		// TODO Auto-generated method stub
		
	}

	@Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
