/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.List;
import ai.RandomBiasedAI;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;


/**
 *
 * @author Stomps
 */

// The AI class
public class TRex extends AI//WithComputationBudget implements InterruptibleAI
{
	// C needs a lot of tweaking
    float C = 0.05f;
	
	// Game evaluation function that returns a value based on units and resources available
//    EvaluationFunction ORIGINAL_EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
//    DinoEvaluation evaluationFunction;
    EvaluationFunction evaluationFunction = new SimpleSqrtEvaluationFunction3();
    
    // Simulations require an opponent to play out against, RandomBiasedAI is a slightly stronger opponent than RandomAI, Or maybe choose stronger?
    AI simulationEnemyAI = new RandomBiasedAI();
    
    Node treeRootNode;
    GameState initialGameState;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 100;
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH;
    
    // If doing a NSimulate evaluation then average random play outs over this many simulations
    int SIMULATION_PLAYOUTS;
    
    int totalNodeVisits;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // For finding the near side resources
    int halfMapDistance;
    
    int playerNumberDifference;
    
    // for epsilon greedy?
//    Random random = new Random();
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    UnitType baseType;
    UnitType workerType;
    
    PhysicalGameState physicalGameState;
    
    float evaluationBound = 1;
    long endTime;
    
    Analysis analysis;
    
    public TRex(UnitTypeTable utt) {
    	unitTypeTable = utt;
        baseType = utt.getUnitType("Base");
        workerType = utt.getUnitType("Worker");
//        evaluationFunction = new DinoEvaluation(unitTypeTable);
    }      
    
    
    public TRex() {
    }
    
    
    public void reset() {
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
    }
    
    
    public void resetSearch() {
        totalNodeVisits = 0;
        simulationEnemyAI = new RandomBiasedAI();
    }
    
    
    public AI clone() {
        return new TRex();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
        playerNumber = player;
        initialGameState = gameState;
    	totalNodeVisits = 0;
    	
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Epsilon greedy?
 //       if (random.nextFloat() < 0.07f) return new PlayerActionGenerator(gameState, player).getRandom();
        
        // Simulate against the best heuristic quick time algorithm possible / available
//        simulationEnemyAI = new Brontosaurus(unitTypeTable);
        
        
        MAX_TREE_DEPTH = 10;
        SIMULATION_PLAYOUTS = 10;
        
        // Time limit
        
        // For determining nearby resources
        physicalGameState = gameState.getPhysicalGameState();
        halfMapDistance = (physicalGameState.getWidth() + physicalGameState.getHeight()) / 2 + 1;
        
        // The initial analysis can be expensive
        analysis = new Analysis(playerNumber, gameState, halfMapDistance, baseType, workerType);
        analysis.analyseGameState();
        
        int gameStateTime = gameState.getTime();
        
        // Sets the weightings for the action analysis. Magnitude not relevant, just the comparative values to each other
        // Key for function arguments:
        // Harvest action weight | move To harvest position weight | attack action weight | produce weight | move towards enemy weight | distance can see Enemy
        if 		(gameStateTime < 100) 	analysis.setAnalysisWeightings(100.0f,	1.0f,	100.0f,	0.0f,	0.0f,	6);
        else if (gameStateTime < 500)	analysis.setAnalysisWeightings(80.0f,	5.0f,	80.0f,	5.0f,	100.0f,	8);
        else if (gameStateTime < 800)	analysis.setAnalysisWeightings(10.0f,	0.2f,	100.0f,	8.0f,	50.0f,	halfMapDistance);
        else if (gameStateTime < 1200)	analysis.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	20.0f,	halfMapDistance*2);
        else if (gameStateTime < 2000)	analysis.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	10.0f,	halfMapDistance*2);
        else 							analysis.setAnalysisWeightings(0.0f,	0.0f,	100.0f,	10.0f,	0.0f,	halfMapDistance*2); 
  
/*
        playerNumberDifference = tree.getPlayerUnitDifference();
        
        if		(tree.getEnemyListSize() <= 2 && gameState.getTime() > 1000)	tree.setAnalysisWeightings(0.0f,	0.0f,	100.0f,	10.0f,	0.0f,	halfMapDistance*2);
        else if (playerNumberDifference < 2) 	tree.setAnalysisWeightings(100.0f,	10.0f,	40.0f,	0.0f,	0.0f,	6);
        else if (playerNumberDifference < 3)	tree.setAnalysisWeightings(50.0f,	1.0f,	100.0f,	5.0f,	100.0f,	8);
        else if (playerNumberDifference < 4)	tree.setAnalysisWeightings(10.0f,	0.2f,	100.0f,	8.0f,	50.0f,	halfMapDistance);
        else if (playerNumberDifference < 5)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	20.0f,	halfMapDistance*2);
        else if (playerNumberDifference < 6)	tree.setAnalysisWeightings(5.0f,	0.0f,	100.0f,	10.0f,	10.0f,	halfMapDistance*2);
        else if (gameState.getTime() > 4000)	tree.setAnalysisWeightings(0.0f,	0.0f,	100.0f,	10.0f,	0.0f,	halfMapDistance*2);
*/       
        
//        endTime = System.currentTimeMillis() + MAXSIMULATIONTIME;
        
        // Initialise the tree as a new Node with parent = null
        treeRootNode = new Node(playerNumber, 1-playerNumber, null, gameState.clone(), analysis, endTime);
        
        endTime = System.currentTimeMillis() + MAXSIMULATIONTIME;
        
        
        // Main loop
        while (true)
        {
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) break;
            
        	// Tries to get a new unexplored action from the tree
            Node newNode = treeRootNode.selectNewAction(playerNumber, 1-playerNumber, endTime, MAX_TREE_DEPTH);

        	// Creating Nodes is expensive so check again!
            if (System.currentTimeMillis() > endTime) break;
            //else System.out.println("Success!");
            
            // If no new actions then null is returned
            if (newNode != null)
            {
            	// Clone the gameState for use in the simulation
                GameState gameStateClone = newNode.getGameState().clone();
                
                // Simulate a play out of that gameState
                simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
                
                // Not too sure here, the evaluation tends towards zero as the time increases
                int time = gameStateClone.getTime() - initialGameState.getTime();
                double evaluation = evaluationFunction.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);//evaluationFunction//

                // Back propagation, cycle though each node's parents until the tree root is reached
                while(newNode != null)
                {
                    newNode.addScore(evaluation);
                    newNode.incrementVisitCount();
                    newNode = newNode.getParent();
                }
            }
        }
        
        // Sanity check
        if (treeRootNode.getChildrenList() == null)
        {
        	System.out.println("Nope");
        	return simulationEnemyAI.getAction(player, gameState);// new PlayerAction();
       	}
        
        // Temporary variable
        Node tempMostVisited = null;
        
        for (Node child : treeRootNode.getChildrenList())
        {
        	// if no other value has been assigned then assign child
            if (tempMostVisited == null ||
            		// or if child is better than temp variable then replace
            		child.getVisitCount() > tempMostVisited.getVisitCount() ||
            		// or if visits are the same but child's score is better then replace
            		(child.getVisitCount() == tempMostVisited.getVisitCount() && child.getScore() > tempMostVisited.getScore()))
            {
            	// Update temporary variable
                tempMostVisited = child;
            }
        }
        
        // Sanity check
        if (tempMostVisited == null)
        {
        	System.out.println("Noooooope");
        	return simulationEnemyAI.getAction(player, gameState);// new PlayerAction();
       	}
        
        // m_ActionMap getter
        return treeRootNode.getActionFromChildNode(tempMostVisited);
    }
      
    

/*    
 * ---------------------------------
 * This could be a better simulation environment? Averaging N play outs.
 * ---------------------------------
 *     
*/
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float NSimulate(GameState gameStateClone, int player, int N) throws Exception
    {
        float accum = 0;
        for(int i = 0; i < N; i++)
        {
            GameState thisNGS = gameStateClone.clone();
            simulate(thisNGS,thisNGS.getTime() + MAXSIMULATIONTIME);
            int time = thisNGS.getTime() - gameStateClone.getTime();
            // Discount factor:
//            accum += (float)(ORIGINAL_EVALUATION_FUNCTION.evaluate(player, 1-player, thisNGS)*Math.pow(0.99,time/10.0));
            accum += (float)(evaluationFunction.evaluate(player, 1-player, thisNGS)*Math.pow(0.99,time/10.0));
        }
            
        return accum/N;
    }    
    
    
    public void simulate(GameState gameState, int time) throws Exception
    {
        boolean gameover = false;

        do
        {
            if (gameState.isComplete())
            {
                gameover = gameState.cycle();
            }
            else
            {
                gameState.issue(simulationEnemyAI.getAction(0, gameState));
                gameState.issue(simulationEnemyAI.getAction(1, gameState));
            }
        }while(!gameover && gameState.getTime() < time);   
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}
