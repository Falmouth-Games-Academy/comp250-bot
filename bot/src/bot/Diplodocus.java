/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.RandomBiasedAI;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.InterruptibleAI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
//import ai.mcts.uct.UCT;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
class NodeDip {
    static Random r = new Random();
    public static float C = 0.05f;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
//    public static float C = 1;   // this is the constant that regulates exploration vs exploitation, it must be tuned for each domain
    
    public int type;    // 0 : max, 1 : min, -1: Game-over
    private NodeDip parent = null;
    public GameState gs;
    int depth = 0;  // the depth in the tree
    
    boolean hasMoreActions = true;
    PlayerActionGenerator moveGenerator = null;
    public List<PlayerAction> actions = null;
    public List<NodeDip> children = null;
    float evaluation_bound = 0;
    private float accum_evaluation = 0;
    public int visit_count = 0;
    
    
    public NodeDip(int maxplayer, GameState a_gs, NodeDip a_parent, float bound) throws Exception
    {
        setParent(a_parent);
        gs = a_gs;
        if (getParent()==null) depth = 0;
        else depth = getParent().depth+1;        
        
        evaluation_bound = bound;

        while(gs.winner()==-1 && 
              !gs.gameover() &&
              !gs.canExecuteAnyAction(maxplayer)) gs.cycle();   
        
        if (gs.winner()!=-1 || gs.gameover())
        {
            type = -1;
        }
        else if (gs.canExecuteAnyAction(maxplayer))
        {
            type = 0;
//            actions = gs.getPlayerActions(maxplayer);
            moveGenerator = new PlayerActionGenerator(a_gs, maxplayer);
            moveGenerator.randomizeOrder();
            actions = new ArrayList<>();
            children = new ArrayList<>();
        }
        else
        {
            type = -1;
            System.err.println("RTMCTSNode: This should not have happened...");
        }     
    }
    
    public NodeDip UCTSelectLeaf(int maxplayer, long cutOffTime, int max_depth) throws Exception
    {
        
        // Cut the tree policy at a predefined depth
        if (depth>=max_depth) return this;        
        
        // if non visited children, visit:
        
        
        if (hasMoreActions)
        {
        	// If no more leafs because moveGenerator = null!
            if (moveGenerator == null) return this;
            
            PlayerAction nextAction = moveGenerator.getNextAction(cutOffTime);
            
            if (nextAction != null)
            {
                actions.add(nextAction);
                GameState gs2 = gs.cloneIssue(nextAction);                
                NodeDip node = new NodeDip(maxplayer, gs2.clone(), this, evaluation_bound);
                children.add(node);
                return node;                
            }
            else
            {
                hasMoreActions = false;
            }
        }
        
        // Bandit policy:
        double best_score = 0;
        NodeDip best = null;
        for (NodeDip child : children)
        {
            double tempScore = childValue(child);
            if (best == null || tempScore > best_score)
            {
                best = child;
                best_score = tempScore;
            }
        } 
        
        // No more leafs because this node has no children!
        if (best == null) return this;
        
        return best.UCTSelectLeaf(maxplayer, cutOffTime, max_depth);
    }    
    
        
    public double childValue(NodeDip child)
    {
        double exploitation = ((double)child.getAccum_evaluation()) / child.visit_count;
        double exploration = Math.sqrt(Math.log((double)visit_count)/child.visit_count);
        
        if (type==0)
        {
            // max node:
            exploitation = (evaluation_bound + exploitation)/(2*evaluation_bound);
        }
        else
        {
            exploitation = (evaluation_bound - exploitation)/(2*evaluation_bound);
        }
//            System.out.println(exploitation + " + " + exploration);

        double tmp = C*exploitation + exploration;
        return tmp;
    }
    
    
    

	public float getAccum_evaluation() {
		return accum_evaluation;
	}

	public void setAccum_evaluation(double d) {
		this.accum_evaluation = (float) d;
	}

	public NodeDip getParent() {
		return parent;
	}

	public void setParent(NodeDip parent) {
		this.parent = parent;
	}
}


public class Diplodocus extends AIWithComputationBudget implements InterruptibleAI
{
    public static int DEBUG = 0;
    EvaluationFunction ef = null;
       
    Random r = new Random();
    AI randomAI = new RandomBiasedAI();
    long max_actions_so_far = 0;
    
    GameState gs_to_start_from = null;
    public NodeDip tree = null;
    
    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;
    
    long total_runs_this_move = 0;
        
    int MAXSIMULATIONTIME = 1024;
    int MAX_TREE_DEPTH = 10;
    
    int playerForThisComputation;
    
    
    public Diplodocus(UnitTypeTable utt)
    {
        this(100,-1,100,10,
             new RandomBiasedAI(),
             new SimpleSqrtEvaluationFunction3());
    }      
    
    
    public Diplodocus(int available_time, int max_playouts, int lookahead, int max_depth, AI policy, EvaluationFunction a_ef)
    {
        super(available_time, max_playouts);
        MAXSIMULATIONTIME = lookahead;
        randomAI = policy;
        MAX_TREE_DEPTH = max_depth;
        ef = a_ef;
    }
    
    
    public String statisticsString() {
        return "Average runs per cycle: " + ((double)total_runs)/total_cycles_executed +
               ", Average runs per action: " + ((double)total_runs)/total_actions_issued;

    }
    
    public void printStats() {
        if (total_cycles_executed>0 && total_actions_issued>0) {
            System.out.println("Average runs per cycle: " + ((double)total_runs)/total_cycles_executed);
            System.out.println("Average runs per action: " + ((double)total_runs)/total_actions_issued);
        }
    }
    
    
    public void reset() {
        gs_to_start_from = null;
        tree = null;
        total_runs_this_move = 0;
    }
    
    
    public AI clone() {
        return new Diplodocus(TIME_BUDGET, ITERATIONS_BUDGET, MAXSIMULATIONTIME, MAX_TREE_DEPTH, randomAI, ef);
    }  
    
    
    public PlayerAction getAction(int player, GameState gs) throws Exception
    {
        if (!gs.canExecuteAnyAction(player)) return new PlayerAction();
        
        startNewComputation(player,gs.clone());
        computeDuringOneGameFrame();
        return getBestActionSoFar();
              
    }
    
    
    public void startNewComputation(int a_player, GameState gs) throws Exception
    {
        float evaluation_bound = ef.upperBound(gs);
        playerForThisComputation = a_player;
        tree = new NodeDip(playerForThisComputation, gs, null, evaluation_bound);
        gs_to_start_from = gs;
        total_runs_this_move = 0;
    }    
    
    
    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        tree = null;
        gs_to_start_from = null;
        total_runs_this_move = 0;
    }
    

    public void computeDuringOneGameFrame() throws Exception
    {
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        int nPlayouts = 0;
        long cutOffTime = start + TIME_BUDGET;
        if (TIME_BUDGET<=0) cutOffTime = 0;

        while(true)
        {
            if (cutOffTime > 0 && System.currentTimeMillis() > cutOffTime) break;
            if (ITERATIONS_BUDGET>0 && nPlayouts>ITERATIONS_BUDGET) break;
            monteCarloRun(playerForThisComputation, cutOffTime);
            nPlayouts++;
        }
        
        total_cycles_executed++;
    }
    

    public double monteCarloRun(int player, long cutOffTime) throws Exception
    {
        NodeDip leaf = tree.UCTSelectLeaf(player, cutOffTime, MAX_TREE_DEPTH);

        if (leaf!=null)
        {
            GameState gs2 = leaf.gs.clone();
            simulate(gs2, gs2.getTime() + MAXSIMULATIONTIME);

            int time = gs2.getTime() - gs_to_start_from.getTime();
            double evaluation = ef.evaluate(player, 1-player, gs2)*Math.pow(0.99,time/10.0);

            while (leaf != null)
            {
                leaf.setAccum_evaluation(leaf.getAccum_evaluation() + evaluation);
                leaf.visit_count++;
                leaf = leaf.getParent();
            }
            total_runs++;
            total_runs_this_move++;
            return evaluation;
        }
        else
        {
            // no actions to choose from :)
            return 0;
        }
    }
    
    
    public PlayerAction getBestActionSoFar()
    {
        total_actions_issued++;
        
        if (tree.children == null)
        {
            return new PlayerAction();
        }
                
        int mostVisitedIdx = -1;
        NodeDip mostVisited = null;
        for (int i = 0; i < tree.children.size(); i++)
        {
            NodeDip child = tree.children.get(i);
            if (mostVisited == null || child.visit_count>mostVisited.visit_count || (child.visit_count==mostVisited.visit_count && child.getAccum_evaluation() > mostVisited.getAccum_evaluation()))
            {
                mostVisited = child;
                mostVisitedIdx = i;
            }
        }
        
        if (mostVisitedIdx==-1) return new PlayerAction();
        
        return tree.actions.get(mostVisitedIdx);
    }
    
    
    // gets the best action, evaluates it for 'N' times using a simulation, and returns the average obtained value:
    public float getBestActionEvaluation(GameState gs, int player, int N) throws Exception
    {
        PlayerAction pa = getBestActionSoFar();
        
        if (pa == null) return 0;

        float accum = 0;
        for (int i = 0; i < N; i++)
        {
            GameState gs2 = gs.cloneIssue(pa);
            GameState gs3 = gs2.clone();
            simulate(gs3,gs3.getTime() + MAXSIMULATIONTIME);
            int time = gs3.getTime() - gs2.getTime();
            // Discount factor:
            accum += (float)(ef.evaluate(player, 1-player, gs3)*Math.pow(0.99,time/10.0));
        }
            
        return accum/N;
    }    
    
    
    
    public void simulate(GameState gs, int time) throws Exception
    {
        boolean gameover = false;

        do
        {
            if (gs.isComplete())
            {
                gameover = gs.cycle();
            }
            else
            {
                gs.issue(randomAI.getAction(0, gs));
                gs.issue(randomAI.getAction(1, gs));
            }
        } while(!gameover && gs.getTime() < time);   
    }
    
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + MAXSIMULATIONTIME + ", " + MAX_TREE_DEPTH + ", " + randomAI + ", " + ef + ")";
    }
    
    
    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("MaxTreeDepth",int.class,10));
        
        parameters.add(new ParameterSpecification("DefaultPolicy",AI.class, randomAI));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));

        return parameters;
    }      
    
    
    public int getPlayoutLookahead() {
        return MAXSIMULATIONTIME;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        MAXSIMULATIONTIME = a_pola;
    }


    public int getMaxTreeDepth() {
        return MAX_TREE_DEPTH;
    }
    
    
    public void setMaxTreeDepth(int a_mtd) {
        MAX_TREE_DEPTH = a_mtd;
    }
    
    
    public AI getDefaultPolicy() {
        return randomAI;
    }
    
    
    public void setDefaultPolicy(AI a_dp) {
        randomAI = a_dp;
    }
    
    
    public EvaluationFunction getEvaluationFunction() {
        return ef;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        ef = a_ef;
    }
}

