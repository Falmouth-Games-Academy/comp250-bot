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
 * @author santi :)
 */
public class ScotlandNumberOne extends AbstractionLayerAI { 
	private Random rng; 
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType heavyType;
	
    public ScotlandNumberOne(UnitTypeTable utt) {
    super(new AStarPathFinding());
    rng = new Random();
    }
    
    @Override
    public void reset() {
    }

    
    @Override
    public AI clone() {
        return new ScotlandNumberOne(null);
    }
   
    
    @Override
    public PlayerAction getAction(int player, GameState gs) {
        for (Unit unit : gs.getUnits())
        {
        	if (unit.getPlayer() == player)
        	{
        		if (unit.getType().canAttack && gs.getActionAssignment(unit) == null)
        		{
        			System.out.println("Thinking");
        			
        			Unit enemyUnit = null;
        			for (Unit u : gs.getUnits())
        			{
        				if (u.getPlayer() != player && u.getType().canMove)
        				{
        					enemyUnit = u;
        				}
        			}
        			if (enemyUnit != null)
        			{
        				attack(unit, enemyUnit);
        			}
        			else
        			{
        				int x = rng.nextInt(gs.getPhysicalGameState().getWidth());
        				int y = rng.nextInt(gs.getPhysicalGameState().getHeight());
        				move (unit, x, y);
        			}
        		}
        	}
        }
    	
        return translateActions(player, gs);
    	/*
    	try {
            if (!gs.canExecuteAnyAction(player)) return new PlayerAction();
            PlayerActionGenerator pag = new PlayerActionGenerator(gs, player);
            return pag.getRandom();
        }catch(Exception e) {
            // The only way the player action generator returns an exception is if there are no units that
            // can execute actions, in this case, just return an empty action:
            // However, this should never happen, since we are checking for this at the beginning
            return new PlayerAction();
        }*/
    }
    
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
