package br.unifor.metahlib.heuristics.hillclimbing;

import java.util.Random;

import deprecated.Function;
import deprecated.Metaheuristic;


/**
 * An implementation of the hill climbing optimization method 
 * 
 * @author marcelo lotif
 *
 */
public class HillClimbing extends Metaheuristic {

	/**
	 * Executes the default hill climbing
	 */
	public static final int DEFAULT = 0;
	/**
	 * Executes the iterated hill climbing with the default execution
	 */
	public static final int ITERATED_DEFAULT = 1;
	/**
	 * Executes the stochastic hill climbing
	 */
	public static final int STOCHASTIC = 2;
	/**
	 * Executes the iterated hill climbing with the stochastic execution
	 */
	public static final int ITERATED_STOCHASTIC = 3;
	
	/**
	 * The max number of iterations
	 */
	private int maxIterations;
	/**
	 * The max number of iterations for the iterated hill climbing
	 */
	private int maxIterations2;

	/**
	 * The type of the execution
	 */
	private int type;
	/**
	 * Parameter of the stochastic hill climbing
	 */
	private double T;
	
	public HillClimbing(Function function, int type, int maxIterations,int maxIterations2, double t) {
		super(function);
		this.maxIterations = maxIterations;
		this.maxIterations2 = maxIterations2;
		this.type = type;
		T = t;
	}
	
	/**
	 * Executes the desired hill climbing.
	 * 
	 * @return the best solution found
	 */
	public double[] execute() {
		lastBestFoundOn = 0;
		switch(type){
			case DEFAULT: return executeDefault(); 
			case ITERATED_DEFAULT: return executeIterated(); 
			case STOCHASTIC: return executeStochastic(); 
			case ITERATED_STOCHASTIC: return executeIterated(); 
		}
		return null;
	}

	/**
	 * Executes the default hill climbing
	 * 
	 * @return the best solution found
	 */
	private double[] executeDefault(){
		double[] x;
		if(initialSolution == null){
			x = function.getRandomSolution();
		} else {
			x = initialSolution;
		}
		
		double eval = function.eval(x);
		//if(type == DEFAULT) { }
		
		for(int i = 0; i < maxIterations; i++){
			double[] _x = function.perturb(x);
			
			eval = function.eval(x);
			double _eval = function.eval(_x);
			
			if(_eval < eval){
				x = _x;
				if(type == DEFAULT){ lastBestFoundOn = i; }
			}
			//if(type == DEFAULT) { }
		}
		
		return x;
	}
	
	/**
	 * Executes the iterated hill climbing
	 * 
	 * @return the best solution found
	 */
	private double[] executeIterated(){
		double[] bestx;
		if(initialSolution == null){
			bestx = function.getRandomSolution();
		} else {
			bestx = initialSolution;
		}
		
		double bestEval = function.eval(bestx);
		for(int i = 0; i < maxIterations2; i++){
	
			double[] thisx; 
			if(type == ITERATED_DEFAULT) {
				thisx = executeDefault();
			} else {
				thisx = executeStochastic();
			}
			
			double thisEval = function.eval(thisx);
			bestEval = function.eval(bestx);
			
			if(thisEval < bestEval){
				bestx = thisx;
				lastBestFoundOn = i + 2;
			}
		}
		
		return bestx;
	}
	
	/**
	 * Executes the stochastic hill climbing
	 * 
	 * @return the best solution found
	 */
	private double[] executeStochastic(){
		Random r = new Random();
		
		double[] x;
		if(initialSolution == null){
			x = function.getRandomSolution();
		} else {
			x = initialSolution;
		}
		
		double eval = function.eval(x);
		//if(type == STOCHASTIC) { }
		
		for(int i = 0; i < maxIterations; i++){
			double[] _x = function.perturb(x);
			
			eval = function.eval(x);
			double _eval = function.eval(_x);
			
			double rand = r.nextDouble();
//			double minus = Math.abs(_eval) - Math.abs(eval);
//			double minusT = minus/T;
//			double exp = Math.exp(minusT);
//			double plusOne = (1.0 + exp); 
//			double p = 1.0/plusOne;
			
			double exp = (1./(1.+Math.exp((Math.abs(eval)-Math.abs(_eval))/T)));
			
			if(rand < exp){
				x = _x;
				eval = _eval;
				if(type == STOCHASTIC) { lastBestFoundOn = i; }
			}
			//if(type == STOCHASTIC) { }
		}
		
		return x;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public int getMaxIterations2() {
		return maxIterations2;
	}

	public void setMaxIterations2(int maxIterations2) {
		this.maxIterations2 = maxIterations2;
	}
	
	public double getT() {
		return T;
	}

	public void setT(double t) {
		T = t;
	}
	
}