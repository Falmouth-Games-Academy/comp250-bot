package tests;
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import ai.core.AI;
import ai.RandomBiasedAI;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.mcts.MCTSNode;
import ai.mcts.naivemcts.NaiveMCTS;
import ai.mcts.naivemcts.NaiveMCTSNode;
import bot.*;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class GameVisualSimulationTest
{
	// maps found at C:\Users\Rich\Desktop\Uni\comp250\MICRORTS\_MyMicroRTS\comp250-bot\microrts\maps
	public static List<String> mapList = new ArrayList<String>(Arrays.asList(
			"../microrts/maps/8x8/bases8x8.xml",
			"../microrts/maps/8x8/basesWorkers8x8.xml",
			"../microrts/maps/8x8/basesWorkers8x8A.xml",
			"../microrts/maps/8x8/basesWorkers8x8B.xml",
			"../microrts/maps/8x8/basesWorkers8x8C.xml",
			"../microrts/maps/8x8/basesWorkers8x8D.xml",
			"../microrts/maps/8x8/basesWorkers8x8E.xml",
			"../microrts/maps/8x8/basesWorkers8x8F.xml",
/*			"../microrts/maps/8x8/basesWorkers8x8G.xml",
			"../microrts/maps/8x8/basesWorkers8x8H.xml",
			"../microrts/maps/8x8/basesWorkers8x8I.xml",
			"../microrts/maps/8x8/basesWorkers8x8J.xml",
			"../microrts/maps/8x8/basesWorkers8x8K.xml",
			"../microrts/maps/8x8/basesWorkers8x8L.xml",
			"../microrts/maps/8x8/basesWorkers8x8Obstacle.xml",
			"../microrts/maps/8x8/basesWorkersBarracks8x8.xml",
			"../microrts/maps/8x8/FourBasesWorkers8x8.xml",
			"../microrts/maps/8x8/melee8x8light4.xml",
			"../microrts/maps/8x8/melee8x8Mixed4.xml",
			"../microrts/maps/8x8/melee8x8Mixed6.xml",
			"../microrts/maps/8x8/OneBaseWorker8x8.xml",
			"../microrts/maps/8x8/ThreeBasesWorkers8x8.xml",
			"../microrts/maps/8x8/TwoBasesWorkers8x8.xml",
			
			"../microrts/maps/10x10/basesWorkers10x10.xml",
			
			"../microrts/maps/12x12/basesWorkers12x12.xml",
			"../microrts/maps/12x12/complexBasesWorkers12x12.xml",
			"../microrts/maps/12x12/FourBasesWorkers12x12.xml",
			"../microrts/maps/12x12/melee12x12Mixed12.xml",
			"../microrts/maps/12x12/OneBaseWorker12x12.xml",
			"../microrts/maps/12x12/SixBasesWorkers12x12.xml",
			"../microrts/maps/12x12/ThreeBasesWorkers12x12.xml",
			"../microrts/maps/12x12/TwoBasesWorkers12x12.xml",
			
			"../microrts/maps/16x16/basesWorkers16x16.xml",
			"../microrts/maps/16x16/basesWorkers16x16A.xml",
			"../microrts/maps/16x16/basesWorkers16x16B.xml",
			"../microrts/maps/16x16/basesWorkers16x16C.xml",
			"../microrts/maps/16x16/basesWorkers16x16D.xml",
			"../microrts/maps/16x16/basesWorkers16x16E.xml",
			"../microrts/maps/16x16/basesWorkers16x16F.xml",
			"../microrts/maps/16x16/basesWorkers16x16G.xml",
			"../microrts/maps/16x16/basesWorkers16x16H.xml",
			"../microrts/maps/16x16/basesWorkers16x16I.xml",
			"../microrts/maps/16x16/basesWorkers16x16J.xml",
			"../microrts/maps/16x16/basesWorkers16x16K.xml",
			"../microrts/maps/16x16/basesWorkers16x16L.xml",
			"../microrts/maps/16x16/EightBasesWorkers16x16.xml",
			"../microrts/maps/16x16/melee16x16Mixed8.xml",
			"../microrts/maps/16x16/melee16x16Mixed12.xml",
			"../microrts/maps/16x16/TwoBasesBarracks16x16.xml",
			
			"../microrts/maps/24x24/basesWorkers24x24.xml",
			"../microrts/maps/24x24/basesWorkers24x24A.xml",
			"../microrts/maps/24x24/basesWorkers24x24B.xml",
			"../microrts/maps/24x24/basesWorkers24x24C.xml",
			"../microrts/maps/24x24/basesWorkers24x24D.xml",
			"../microrts/maps/24x24/basesWorkers24x24E.xml",
			"../microrts/maps/24x24/basesWorkers24x24F.xml",
			"../microrts/maps/24x24/basesWorkers24x24G.xml",
			"../microrts/maps/24x24/basesWorkers24x24H.xml",
			"../microrts/maps/24x24/basesWorkers24x24I.xml",
			"../microrts/maps/24x24/basesWorkers24x24J.xml",
			"../microrts/maps/24x24/basesWorkers24x24K.xml",
*/		//	"../microrts/maps/24x24/basesWorkers24x24L.xml",
/*			
			"../microrts/maps/BroodWar/(2)Benzene.scxA.xml",
			"../microrts/maps/BroodWar/(2)Destination.scxA.xml",
			"../microrts/maps/BroodWar/(2)HeartbreakRidge.scxA.xml",
			"../microrts/maps/BroodWar/(3)Aztec.scxA.xml",
			"../microrts/maps/BroodWar/(3)Aztec.scxB.xml",
			"../microrts/maps/BroodWar/(3)Aztec.scxC.xml",
			"../microrts/maps/BroodWar/(3)TauCross.scxA.xml",
			"../microrts/maps/BroodWar/(3)TauCross.scxB.xml",
			"../microrts/maps/BroodWar/(3)TauCross.scxC.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxA.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxB.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxC.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxD.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxE.xml",
			"../microrts/maps/BroodWar/(4)Andromeda.scxF.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmA.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmB.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmC.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmD.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmE.xml",
			"../microrts/maps/BroodWar/(4)BloodBath.scmF.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxA.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxB.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxC.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxD.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxE.xml",
			"../microrts/maps/BroodWar/(4)CircuitBreaker.scxF.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmA.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmB.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmC.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmD.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmE.xml",
			"../microrts/maps/BroodWar/(4)EmpireoftheSun.scmF.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxA.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxB.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxC.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxD.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxE.xml",
			"../microrts/maps/BroodWar/(4)Fortress.scxF.xml",
			"../microrts/maps/BroodWar/(4)Python.scxA.xml",
			"../microrts/maps/BroodWar/(4)Python.scxB.xml",
			"../microrts/maps/BroodWar/(4)Python.scxC.xml",
			"../microrts/maps/BroodWar/(4)Python.scxD.xml",
			"../microrts/maps/BroodWar/(4)Python.scxE.xml",
			"../microrts/maps/BroodWar/(4)Python.scxF.xml",
*/			
//			"../microrts/maps/BWDistantResources32x32.xml",
//			"../microrts/maps/DoubleGame24x24.xml",
//			"../microrts/maps/EightBasesWorkers16x12.xml",
//			"../microrts/maps/melee4x4light2.xml",
//			"../microrts/maps/melee4x4Mixed2.xml",
			"../microrts/maps/melee14x12Mixed18.xml",
			"../microrts/maps/NoWhereToRun9x8.xml"
			));
	
    public static void main(String args[]) throws Exception {
    	
    	for (int i = 0; i < mapList.size(); i++)//1; i++)//
    	{
    	
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load(mapList.get(i)/*"../microrts/maps/8x8/basesWorkers8x8H.xml"*/, utt);//NoWhereToRun9x8.xml", utt);//16x16/basesWorkers16x16.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        AI ai1 = new Pterodactyl(utt);
        AI ai2 = new Brontosaurus(utt);//RandomBiasedAI();//

        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        /*
        Triceratops myAi = (Triceratops)ai1;
        for (MCTSNode node : myAi.GetTree().children)
        {
        	System.out.println(node.visit_count);// node.accum_evaluation);
        }
        System.out.println("Tree size: " + myAi.GetTree().children.size());
        System.out.println("Game Over");
        */
    }    
    }
}
