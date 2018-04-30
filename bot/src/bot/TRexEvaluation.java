/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;


/**
 *
 * @author Stomps
 */

/*
 * A set of weights and values that when applied to a gameState will result in a grading for that gameState based on the player values passed in.
 * 
 * @param maxPlayer The player for which the evaluation function will return a higher score if the gameState is favourable to that player.
 * @param minPlayer The player for which the evaluation function will return a lower score if the gameState is favourable to that player.
 */

public class TRexEvaluation extends EvaluationFunction
{   
	// Value of the player's resource in base
    public static float RESOURCE_VALUE = 40;
    // Value of each unit's carried resource
    public static float RESOURCE_IN_WORKER = 20;
    // Value modifier for owning a unit
    public static float UNIT_BONUS_MULTIPLIER = 50;
    
    static float HARVEST_VALUE = 150;
    static float PRODUCE_VALUE = 100;
    static float BASE_VALUE = 30;
    static float BARRACKS_VALUE = 60;
    static float RANGED_VALUE = 200;
    static float LIGHT_VALUE = 200;
    
    UnitType m_WorkerType;
    UnitType m_BaseType;
    UnitType m_BarracksType;
    UnitType m_RangedType;
    UnitType m_LightType;
    
    public TRexEvaluation(UnitTypeTable unitTypeTable)
    {
    	// Set the Unit type variables from the unitTypeTable
    	m_WorkerType = unitTypeTable.getUnitType("Worker");
        m_BaseType = unitTypeTable.getUnitType("Base");
        m_BarracksType = unitTypeTable.getUnitType("Barracks");
        m_RangedType = unitTypeTable.getUnitType("Ranged");
        m_LightType = unitTypeTable.getUnitType("Light");
    }
    
    public float evaluate(int maxplayer, int minplayer, GameState gs)
    {
        float score1 = baseScore(maxplayer, gs);
        float score2 = baseScore(minplayer, gs);
        
        // Check for dividing by zero
        if (score1 + score2 == 0) return 0.5f;
        return (2 * score1 / (score1 + score2)) - 1;
    }
    
    public float baseScore(int player, GameState gameState)
    {
        // Initialise score float with player resource value
        float score = gameState.getPlayer(player).getResources() * RESOURCE_VALUE;
        
        boolean playerHasUnits = false;
        for(Unit unit : gameState.getUnits())
        {
            if (unit.getPlayer() == player) 
            {
                playerHasUnits = true;
                
                // Accumulate score based on number of units
                score += unit.getResources() * RESOURCE_IN_WORKER;
                score += UNIT_BONUS_MULTIPLIER * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
              
                // accumulate score based on the unit type and the value given to it
                if 		(unit.getType() == m_BaseType) 		score += BASE_VALUE 	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
	            else if (unit.getType() == m_BarracksType)	score += BARRACKS_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
                else if (unit.getType() == m_RangedType)	score += RANGED_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
	            else if (unit.getType() == m_LightType)		score += LIGHT_VALUE	* unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
            }
            // Give priority to weakening or destroying the enemy base
            else if (unit.getType() == m_BaseType)			score -= BASE_VALUE * 99 * unit.getCost() * Math.sqrt(unit.getHitPoints() / unit.getMaxHitPoints());
        }
        if (!playerHasUnits) return 0;
        return score;
    }    
    
    // Not used but needed to extend EvaluationFunction
    public float upperBound(GameState gs)
    {
        return 1.0f;
    }
}
