package tests;
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import ai.core.AI;
import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;

import ai.abstraction.cRush.CRush_V2;
import ai.abstraction.cRush.CRush_V1;
import ai.portfolio.PortfolioAI;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.mcts.naivemcts.NaiveMCTS;
import bot.*;
import gui.PhysicalGameStatePanel;
import java.io.OutputStreamWriter;
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
public class GameVisualSimulationTest {
    public static void main(String args[]) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/NoWhereToRun9x8.xml", utt);
        PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/8x8/basesWorkers8x8Obstacle.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/8x8/basesWorkers8x8.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/10x10/basesWorkers10x10.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/12x12/basesWorkers12x12.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/16x16/basesWorkers16x16.xml", utt);
        //PhysicalGameState pgs = PhysicalGameState.load("../microrts/maps/24x24/basesWorkers24x24.xml", utt);
//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        //AI ai1 = new WorkerRush(utt, new BFSPathFinding());
        AI ai2 = new ShallowMind(utt);
        AI ai1 = new WorkerRush(utt);
        //AI ai2 = new RandomAI();

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
        
        System.out.println("Game Over");
    }    
}
