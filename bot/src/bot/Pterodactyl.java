/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

//import ai.RandomBiasedAI;
import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.cRush.RangedAttack;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.units.*;
import BurgerBot.*;


/**
 *
 * @author Stomps
 */

class Node
{
	// C needs a lot of tweaking
    private float C = 0.05f;
    private Node m_Parent;// = null;
    private GameState m_GameState;
    private int m_CurrentTreeDepth;// = 0;
    
    private boolean m_HasUnexploredActions = true;
    private PlayerActionGenerator m_ActionGenerator = null;
    private List<Node> m_ChildrenList = new ArrayList<>();
    private float m_EvaluationBound = 0;
    private double m_Score = 0;
    private int m_VisitCount = 0;
    private Map<Node, PlayerAction> m_ActionMap = new HashMap<Node, PlayerAction> ();
    
/*------------------------------------------------------------------------*/    
    
    public double getScore() { return m_Score; }
    public void addScore(double score) { m_Score += score; }
    public Node getParent() { return m_Parent; }
    public GameState getGameState() { return m_GameState; }
    public int getVisitCount() { return m_VisitCount; }
    public void incrementVisitCount() { m_VisitCount++; }
    public List<Node> getChildrenList() { return m_ChildrenList; }
    public PlayerAction getActionFromChildNode(Node child) { return m_ActionMap.get(child); }
    
/*------------------------------------------------------------------------*/   
    
    // Constructor
    public Node(int maxPlayer, int minPlayer, Node parent, GameState gameState, float evaluationBound) throws Exception
    {
        m_Parent = parent;
        m_GameState = gameState;
        m_EvaluationBound = evaluationBound;
        
        // The node initialised with a null parent is the tree's root node with depth 0, otherwise it is the next depth layer down from the parent's depth
        if (m_Parent == null) m_CurrentTreeDepth = 0;
        else m_CurrentTreeDepth = m_Parent.m_CurrentTreeDepth + 1;
        
        // While there is no winner and the game is not over, cycle the gameState until a player can make a move
        while(m_GameState.winner() == -1 && 
              !m_GameState.gameover() &&
              !m_GameState.canExecuteAnyAction(maxPlayer) && 
              !m_GameState.canExecuteAnyAction(minPlayer)) m_GameState.cycle();
        
        // Check that the gameState and winner is still valid for playing on
        if (m_GameState.winner() == -1 || !m_GameState.gameover())
        {
        	// Initialise and randomise the PlayerActionGenerator for this node based on the player number
	        if (m_GameState.canExecuteAnyAction(maxPlayer))
	        {
	            m_ActionGenerator = new PlayerActionGenerator(gameState, maxPlayer);
	            m_ActionGenerator.randomizeOrder();
	        }
	        else if (m_GameState.canExecuteAnyAction(minPlayer))
	        {
	            m_ActionGenerator = new PlayerActionGenerator(gameState, minPlayer);
	            m_ActionGenerator.randomizeOrder();
	        }
        }
    }
    
    // Returns a new Node linked to a new unexplored player action in the m_ActionMap as the PlayerAction's key
    public Node selectNewAction(int maxPlayer, int minPlayer, long endTime, int maxTreeDepth) throws Exception
    {
        // Do a depth check. This AI will explore up to a predefined depth as the end of the game is often too far away
        if (m_CurrentTreeDepth >= maxTreeDepth) return this;        
        
        // If this node has unexplored actions, else look at best child child determined by UCB
    	if (m_HasUnexploredActions)
        {
    		// If no more actions
            if (m_ActionGenerator == null) return this;
            
            // Move to the next (randomised order on initialisation) action available
    		PlayerAction nextAction = m_ActionGenerator.getNextAction(endTime);
            
    		// Check if last action that is available has been reached (next will be null)
    		if (nextAction != null)
            {
    			// Clone the gameState from after the command
    			GameState simulatedGameState = m_GameState.cloneIssue(nextAction);                
                
    			// Constructor takes for new child takes 'this' as parent argument
        		Node newChildNode = new Node(maxPlayer, minPlayer, this, simulatedGameState.clone(), m_EvaluationBound);
                
        		// Store action in map with newNode as key to retrieve if necessary were this node chosen as final move
    			m_ActionMap.put(newChildNode, nextAction);
        		
    			// Add to children list. This is later cycled through to find the best child of a node
                m_ChildrenList.add(newChildNode);
                
                return newChildNode;                
            }
            else
            {
            	// Stop future iterations from trying to explore new actions from this node
                m_HasUnexploredActions = false;
            }
        }
        
        // Temporary variables
        Node tempBestNode = null;
        double tempBestScore = 0;
        
        // Find the child with the best UCB score
        for (Node childNode : m_ChildrenList)
        {
            double childNodeScore = UCBScore(childNode);
            if (tempBestNode == null || childNodeScore > tempBestScore)
            {
                tempBestNode = childNode;
                tempBestScore = childNodeScore;
            }
        } 
        
        // Sanity check, or if no children
        if (tempBestNode == null) return this;
        
        // Explore that child for new unexplored PlayerActions
        return tempBestNode.selectNewAction(maxPlayer, minPlayer, endTime, maxTreeDepth);
    }    
      
    public double UCBScore(Node child)
    {
    	// Tweak the constant. Dynamic? How...
    	//C = 0.707f;
    	
    	return child.getScore()/child.getVisitCount() + C * Math.sqrt(2 * Math.log((double)child.getParent().getVisitCount())/child.getVisitCount());
    }
}


// The AI class
public class Pterodactyl extends AI//WithComputationBudget implements InterruptibleAI
{
	// Game evaluation function that returns a value based on units and resources available
    EvaluationFunction EVALUATION_FUNCTION = new SimpleSqrtEvaluationFunction3();
    
    // Simulations require an opponent to play out against, RandomBiasedAI is a slightly stronger opponent than RandomAI, Or maybe choose stronger?
    AI simuationEnemyAI;// = new RandomBiasedAI();
    
    // 
    GameState initialGameState = null;
    Node tree = null;
    
    // The time allowance that is given to the main loop before breaking and finding the best found child
    int MAXSIMULATIONTIME = 100;//1024; // 100?
    
    // The look ahead depth allowance of nodes in the tree
    int MAX_TREE_DEPTH; //10;
    
    // The 0 or 1 identifier number of this player
    int playerNumber;
    
    // Used if needed in the initialising of an opponent AI
    UnitTypeTable unitTypeTable;
    
    
    public Pterodactyl(UnitTypeTable utt) {
    	unitTypeTable = utt;
    }      
    
    
    public Pterodactyl() {
    }
    
    
    public void reset() {
        initialGameState = null;
        tree = null;
    }
    
    
    public void resetSearch() {
        tree = null;
        initialGameState = null;
    }
    
    
    public AI clone() {
        return new Pterodactyl();
    }  
    
    
    public PlayerAction getAction(int player, GameState gameState) throws Exception
    {
    	// 
        if (!gameState.canExecuteAnyAction(player)) return new PlayerAction();
        
        // Simulate against the best heuristic quick time algorithm possible / available
        simuationEnemyAI = new BurgerBot(unitTypeTable);//Diplodocus3(unitTypeTable);
        
        // Used to estimate the look ahead max tree depth heuristic
        PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
        
        // Cartesian derived heuristic for a lookahead amount
        MAX_TREE_DEPTH = physicalGameState.getWidth() + physicalGameState.getHeight();
        
        // This just returns 1 as far as I can tell
        float evaluation_bound = EVALUATION_FUNCTION.upperBound(gameState);
        
        playerNumber = player;
        initialGameState = gameState;
        
        // Initialise the tree as a new Node with parent = null
        tree = new Node(playerNumber, 1-playerNumber, null, gameState.clone(), evaluation_bound);
        
        // Time stuff can be done better
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 100;
        
        // Main loop
        while(true)
        {
        	// Breaks out when the time exceeds
            if (System.currentTimeMillis() > endTime) break;
            {
            	// Tries to get a new unexplored action from the tree
                Node newNode = tree.selectNewAction(playerNumber, 1-playerNumber, endTime, MAX_TREE_DEPTH);
                
                // If no new actions then null is returned
                if (newNode != null)
                {
                	// Clone the gameState for use in the simulation
                    GameState gameStateClone = newNode.getGameState().clone();
                    
                    // Simulate a play out of that gameState
                    simulate(gameStateClone, gameStateClone.getTime() + MAXSIMULATIONTIME);
                    
                    // Not too sure here, the evaluation tends towards zero as the time increases
                    int time = gameStateClone.getTime() - initialGameState.getTime();
                    double evaluation = EVALUATION_FUNCTION.evaluate(playerNumber, 1-playerNumber, gameStateClone) * Math.pow(0.99,time/10.0);

                    // Back propagation, cycle though each node's parents until the tree root is reached
                    while(newNode != null)
                    {
                        newNode.addScore(evaluation);
                        newNode.incrementVisitCount();
                        newNode = newNode.getParent();
                    }
                }
            }
        }
        
        // Sanity check
        if (tree.getChildrenList() == null) return new PlayerAction();
        
        // Temporary variable
        Node tempMostVisited = null;
        
        for (Node child : tree.getChildrenList())
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
        if (tempMostVisited == null) return new PlayerAction();
        
        // m_ActionMap getter
        return tree.getActionFromChildNode(tempMostVisited);
    }
      
    
    
/*    
 * ---------------------------------
 * This could be a better simulation environment? Averaging N play outs.
 * ---------------------------------
 * 
 * 
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float getBestActionEvaluation(GameState gs, int player, int N) throws Exception {
        PlayerAction pa = getBestActionSoFar();
        
        if (pa==null) return 0;

        float accum = 0;
        for(int i = 0;i<N;i++) {
            GameState gs2 = gs.cloneIssue(pa);
            GameState gs3 = gs2.clone();
            simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);
            int time = gs3.getTime() - gs2.getTime();
            // Discount factor:
            accum += (float)(EVALUATION_FUNCTION.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0));
        }
            
        return accum/N;
    }    
*/    
    
    
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
                gameState.issue(simuationEnemyAI.getAction(0, gameState));
                gameState.issue(simuationEnemyAI.getAction(1, gameState));
            }
        }while(!gameover && gameState.getTime() < time);   
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}

/**
*
* @author Cristiano D'Angelo
* 
* Currently best available enemy to simulate play outs against
* 
*/
/*
class EnemyDiplodocus extends AbstractionLayerAI
{

   Random r = new Random();
   protected UnitTypeTable utt;
   UnitType workerType;
   UnitType baseType;
   UnitType barracksType;
   UnitType rangedType;
   UnitType heavyType;
   UnitType lightType;

   public EnemyDiplodocus(UnitTypeTable a_utt) {
       this(a_utt, new FloodFillPathFinding());
   }

   public EnemyDiplodocus(UnitTypeTable a_utt, PathFinding a_pf) {
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
       lightType = utt.getUnitType("Light");
   }

   public AI clone() {
       return new Diplodocus3(utt, pf);
   }

   boolean buildingRacks = false;
   int resourcesUsed = 0;
   
   
   public PlayerAction getAction(int player, GameState gs) {
       PhysicalGameState pgs = gs.getPhysicalGameState();
       Player p = gs.getPlayer(player);
       boolean isRush = false;
       
       
       if ((pgs.getWidth() * pgs.getHeight()) <= 144){
           isRush = true;
       }

       List<Unit> workers = new LinkedList<Unit>();
       for (Unit u : pgs.getUnits()) {
           if (u.getType().canHarvest
                   && u.getPlayer() == player) {
               workers.add(u);
           }
       }
       if(isRush){
           rushWorkersBehavior(workers, p, pgs, gs);
       } else {
           workersBehavior(workers, p, pgs, gs);
       }

       // Behaviour of bases:
       for (Unit u : pgs.getUnits()) {
           if (u.getType() == baseType
                   && u.getPlayer() == player
                   && gs.getActionAssignment(u) == null) {
               
               if(isRush){
                   rushBaseBehavior(u, p, pgs);
               }else {
                   baseBehavior(u, p, pgs);
               }
           }
       }

       // Behaviour of barracks:
       for (Unit u : pgs.getUnits()) {
           if (u.getType() == barracksType
                   && u.getPlayer() == player
                   && gs.getActionAssignment(u) == null) {
               barracksBehavior(u, p, pgs);
           }
       }

       // Behaviour of melee units:
       for (Unit u : pgs.getUnits()) {
           if (u.getType().canAttack && !u.getType().canHarvest
                   && u.getPlayer() == player
                   && gs.getActionAssignment(u) == null) {
               if (u.getType() == rangedType) {
                   rangedUnitBehavior(u, p, gs);
               } else {
                   meleeUnitBehavior(u, p, gs);
               }
           }
       }

       return translateActions(player, gs);
   }

   public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {

       int nbases = 0;
       int nbarracks = 0;
       int nworkers = 0;
       int resources = p.getResources();

       for (Unit u2 : pgs.getUnits()) {
           if (u2.getType() == workerType
                   && u2.getPlayer() == p.getID()) {
               nworkers++;
           }
           if (u2.getType() == barracksType
                   && u2.getPlayer() == p.getID()) {
               nbarracks++;
           }
           if (u2.getType() == baseType
                   && u2.getPlayer() == p.getID()) {
               nbases++;
           }
       }
       if (nworkers < (nbases + 1) && p.getResources() >= workerType.cost) {
           train(u, workerType);
       }

       //Buffers the resources that are being used for barracks
       if (resourcesUsed != barracksType.cost * nbarracks) {
           resources = resources - barracksType.cost;
       }

       if (buildingRacks && (resources >= workerType.cost + rangedType.cost)) {
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
       for (Unit u2 : pgs.getUnits()) {
           if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
               int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
               if (closestEnemy == null || d < closestDistance) {
                   closestEnemy = u2;
                   closestDistance = d;
               }
           }
       }
       if (closestEnemy != null) {
//           System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
           attack(u, closestEnemy);
       }
   }

   public void rangedUnitBehavior(Unit u, Player p, GameState gs) {
       PhysicalGameState pgs = gs.getPhysicalGameState();
       Unit closestEnemy = null;
       Unit closestRacks = null;
       int closestDistance = 0;
       for (Unit u2 : pgs.getUnits()) {
           if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
               int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
               if (closestEnemy == null || d < closestDistance) {
                   closestEnemy = u2;
                   closestDistance = d;
               }
           }
           if (u2.getType() == barracksType && u2.getPlayer() == p.getID()) {
               int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
               if (closestRacks == null || d < closestDistance) {
                   closestRacks = u2;
                   closestDistance = d;
               }
           }
       }
       if (closestEnemy != null) {
//           System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
           rangedAttack(u, closestEnemy, closestRacks);

       }
   }

   public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, GameState gs) {
       int nbases = 0;
       int nbarracks = 0;
       int nworkers = 0;
       resourcesUsed = 0;
       
       List<Unit> freeWorkers = new LinkedList<Unit>();
       List<Unit> battleWorkers = new LinkedList<Unit>();

       for (Unit u2 : pgs.getUnits()) {
           if (u2.getType() == baseType
                   && u2.getPlayer() == p.getID()) {
               nbases++;
           }
           if (u2.getType() == barracksType
                   && u2.getPlayer() == p.getID()) {
               nbarracks++;
           }
           if (u2.getType() == workerType
                   && u2.getPlayer() == p.getID()) {
               nworkers++;
           }
       }

       if (workers.size() > (nbases + 1)) {
           for (int n = 0; n < (nbases + 1); n++) {
               freeWorkers.add(workers.get(0));
               workers.remove(0);
           }
           battleWorkers.addAll(workers);
       } else {
           freeWorkers.addAll(workers);
       }

       if (workers.isEmpty()) {
           return;
       }

       List<Integer> reservedPositions = new LinkedList<Integer>();
       if (nbases == 0 && !freeWorkers.isEmpty()) {
           // build a base:
           if (p.getResources() >= baseType.cost) {
               Unit u = freeWorkers.remove(0);
               buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
               //resourcesUsed += baseType.cost;
           }
       }
       if ((nbarracks == 0) && (!freeWorkers.isEmpty()) && nworkers > 1
               && p.getResources() >= barracksType.cost) {
           
           int resources = p.getResources();
           Unit u = freeWorkers.remove(0);   
           buildIfNotAlreadyBuilding(u,barracksType,u.getX(),u.getY(),reservedPositions,p,pgs);
           resourcesUsed += barracksType.cost;
           buildingRacks = true;
               
               //The problem with this right now is that we can only track when a build command is sent
               //Not when it actually starts building the building.
       } else {
           resourcesUsed =  barracksType.cost * nbarracks;
       }
       
       if (nbarracks > 1) {
           buildingRacks = true;
       }

       for (Unit u : battleWorkers) {
           meleeUnitBehavior(u, p, gs);
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
               if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
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
                   Harvest h_aa = (Harvest) aa;
                   if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                       harvest(u, closestResource, closestBase);
                   }
               } else {
                   harvest(u, closestResource, closestBase);
               }
           }
       }
   }
   
   
   public void rushBaseBehavior(Unit u,Player p, PhysicalGameState pgs) {
       if (p.getResources()>=workerType.cost) train(u, workerType);
   }
   
   public void rushWorkersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs, GameState gs) {
       int nbases = 0;
       int nworkers = 0;
       resourcesUsed = 0;
       
       List<Unit> freeWorkers = new LinkedList<Unit>();
       List<Unit> battleWorkers = new LinkedList<Unit>();

       for (Unit u2 : pgs.getUnits()) {
           if (u2.getType() == baseType
                   && u2.getPlayer() == p.getID()) {
               nbases++;
           }
           if (u2.getType() == workerType
                   && u2.getPlayer() == p.getID()) {
               nworkers++;
           }
       }
       if (p.getResources() == 0){
           battleWorkers.addAll(workers);
       } 
       else if (workers.size() > (nbases)) {
           for (int n = 0; n < (nbases); n++) {
               freeWorkers.add(workers.get(0));
               workers.remove(0);
           }
           battleWorkers.addAll(workers);
       } else {
           freeWorkers.addAll(workers);
       }

       if (workers.isEmpty()) {
           return;
       }

       List<Integer> reservedPositions = new LinkedList<Integer>();
       if (nbases == 0 && !freeWorkers.isEmpty()) {
           // build a base:
           if (p.getResources() >= baseType.cost) {
               Unit u = freeWorkers.remove(0);
               buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
               //resourcesUsed += baseType.cost;
           }
       }
       
       for (Unit u : battleWorkers) {
           meleeUnitBehavior(u, p, gs);
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
               if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
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
                   Harvest h_aa = (Harvest) aa;
                   if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                       harvest(u, closestResource, closestBase);
                   }
               } else {
                   harvest(u, closestResource, closestBase);
               }
           }
       }
   }
   
   
   public void rangedAttack(Unit u, Unit target, Unit racks) {
       actions.put(u, new RangedAttack(u, target, racks, pf));
   }
   
   

   @Override
   public List<ParameterSpecification> getParameters() {
       List<ParameterSpecification> parameters = new ArrayList<>();

       parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new FloodFillPathFinding()));

       return parameters;
   }   
}
*/
