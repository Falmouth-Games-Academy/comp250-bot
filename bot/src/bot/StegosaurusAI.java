/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.core.AI;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.List;
import rts.*;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
public class StegosaurusAI extends AI {    
    public StegosaurusAI(UnitTypeTable utt) {
    }
    

    public StegosaurusAI() {
    }
    
    
    @Override
    public void reset() {
    }

    
    @Override
    public AI clone() {
        return new StegosaurusAI();
    }
   
    
    @Override
    public PlayerAction getAction(int player, GameState gs) {
        try {
            if (!gs.canExecuteAnyAction(player)) return new PlayerAction();
            PlayerActionGenerator pag = new PlayerActionGenerator(gs, player);
            return pag.getRandom();
        }catch(Exception e) {
            // The only way the player action generator returns an exception is if there are no units that    
            // can execute actions, in this case, just return an empty action:
            // However, this should never happen, since we are checking for this at the beginning
            return new PlayerAction();
        }
    }
    
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
