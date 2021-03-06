package g2;

import java.util.*;

import sim.Game;
import sim.GameHistory;
import sim.SimPrinter;

public class Player extends sim.Player {

/**
* Player constructor
*
* @param teamID      team ID
* @param rounds      number of rounds
* @param seed        random seed
* @param simPrinter  simulation printer
*
*/
public Player(Integer teamID, Integer rounds, Integer seed, SimPrinter simPrinter) {
	super(teamID, rounds, seed, simPrinter);
}

/**
* Reallocate player goals
*
* @param round             current round
* @param gameHistory       cumulative game history from all previous rounds
* @param playerGames       state of player games before reallocation
* @param opponentGamesMap  state of opponent games before reallocation (map of opponent team IDs to their games)
* @return                  state of player games after reallocation
*
*/
public List<Game> reallocate(Integer round, GameHistory gameHistory, List<Game> playerGames, Map<Integer, List<Game>> opponentGamesMap) {


	int goalsToReallocate = 0;
	int excessGoals = 0;
	int mustWinMargin = 2;
	int drawtoLoss = 7;
	int mustLossMargin = 7;

	double ran = Math.random();

	Set<Game> reallocatedPlayerGames = new HashSet<>();

	List<Game> wonGames = getWinningGames(playerGames);
	List<Game> drawnGames = getDrawnGames(playerGames);
	List<Game> lostGames = getLosingGames(playerGames);
	List<Game> lostOrDrawnGames = new ArrayList<>(lostGames);
	List<Game> drawnGamesWithReallocationCapacity = new ArrayList<>(drawnGames);
	List<Integer> targetTeams = new ArrayList<>();


	Map<Integer, Double> rankedMap = new HashMap<Integer, Double>();
	Map<Integer, String> rankedMapS = new HashMap<Integer, String>();
	Map<Integer, Integer> goalsTaken = new HashMap<Integer, Integer>();

	/*if(round == 5){
		simPrinter.println("HIT");
		for (Game game : playerGames){
			simPrinter.println(game.getNumPlayerGoals());
		}
		return playerGames;
	}*/
	lostOrDrawnGames.addAll(drawnGames);

	// Most of the time we want a margin of 2, but a few other times 3
	if (ran>0.1)
		mustWinMargin = 2;
	else
		mustWinMargin = 3;

	/*
 	*  Looks at all opponents' previous two games. If scores were not reallocated, good chance it 
	*  might not happen again, so add them to a target list and target those games. 
	*
	*/

	if(round > 2){
		Map<Integer,List<Game>> lastRoundGames = gameHistory.getAllGamesMap().get(round - 1);
		Map<Integer,List<Game>> twoRoundsAgoGames = gameHistory.getAllGamesMap().get(round - 2);
		List<Integer> lastRoundScores = new ArrayList<>();
		List<Integer> twoRoundsAgoScores = new ArrayList<>();

		for(Map.Entry<Integer,List<Game>> entry : lastRoundGames.entrySet()){
			int teamID = entry.getKey();
			if (teamID != this.teamID){
				simPrinter.println("TESTBEGIN");
				for(Game game : entry.getValue()){
					//simPrinter.println("Zero: " + game.getScoreAsString() +" "+ game.getNumPlayerGoals());
					lastRoundScores.add(game.getNumPlayerGoals());
				}
				for(Game game : twoRoundsAgoGames.get(teamID)){
					//simPrinter.println("One: " + game.getScoreAsString()+" "+game.getNumPlayerGoals());
					twoRoundsAgoScores.add(game.getNumPlayerGoals());
				}
				if (lastRoundScores.equals(twoRoundsAgoScores)){
					simPrinter.println("MATCH: " + teamID);
					targetTeams.add(teamID);
				}
				lastRoundScores.clear();
				twoRoundsAgoScores.clear();
			}
		}
		simPrinter.println(targetTeams);
	}

	
	

	// If draw has 7 or 8 points, most of the time we'll convert to a loss for the points
	for(Game drawGame : drawnGames){
		if(drawGame.getNumPlayerGoals() >= drawtoLoss){
			if (ran>0.01){
				int numGoals = drawGame.getHalfNumPlayerGoals();
				drawGame.setNumPlayerGoals(drawGame.getNumPlayerGoals() - numGoals);
				goalsToReallocate += numGoals;
			}
			drawnGamesWithReallocationCapacity.remove(drawGame);
		} 
		// If draw has < 7 points, save the excess goals just in case (??)
		else {
			int numGoals = drawGame.getHalfNumPlayerGoals();
			drawGame.setNumPlayerGoals(drawGame.getNumPlayerGoals() - numGoals);
			excessGoals += numGoals;
			//keep the game in draws??
		}
	}
	
	// If already winning by MWM, forget about it.
	for(Game winGame: wonGames){
		int margin = winGame.getNumPlayerGoals() - winGame.getNumOpponentGoals();
		if(margin == mustWinMargin){
			goalsTaken.put(winGame.getID(), 0);
		}
		// if winning be more than MWM, take max goals that preserves MWM
		else if(margin > mustWinMargin){
			int halfNumPlayerGoals = winGame.getHalfNumPlayerGoals();
			int numGoals = (int) Math.min(halfNumPlayerGoals, margin - mustWinMargin);

			winGame.setNumPlayerGoals(winGame.getNumPlayerGoals() - numGoals);
			goalsToReallocate += numGoals;
			goalsTaken.put(winGame.getID(), numGoals);
		}
		// If winning by less than MWM, take max goals
		else {
			int numGoals = winGame.getHalfNumPlayerGoals();

			winGame.setNumPlayerGoals(winGame.getNumPlayerGoals() - numGoals);
			goalsToReallocate += numGoals;
			// ??????
			if (margin > 1){
				goalsTaken.put(winGame.getID(), numGoals);
			}
			//  ??????
			else{
				goalsTaken.put(winGame.getID(), 0);
			}
		}
	}

	//look at all losses
	for (Game loss : lostGames) {
		int goalsScored = loss.getNumPlayerGoals();
		int margin = loss.getNumOpponentGoals() - goalsScored;
		
		if (margin >= mustLossMargin)
			continue; 
		if (margin <=2){
			if(goalsToReallocate >1 ){
				if(goalsScored == 7){
					loss.setNumPlayerGoals(goalsScored + 1);
					goalsToReallocate -= 1;
				}
				else{
					loss.setNumPlayerGoals(goalsScored + 2);
					goalsToReallocate -= 2;
				}
			}
			else{
				loss.setNumPlayerGoals(goalsScored + goalsToReallocate);
				goalsToReallocate = 0;
			}
		}
		else if (loss.getNumOpponentGoals() - goalsScored <=4){
			if(goalsToReallocate > 2){
				loss.setNumPlayerGoals(goalsScored + 3);
				goalsToReallocate -= 3;
			}
			else {
				loss.setNumPlayerGoals(goalsScored + goalsToReallocate);
				goalsToReallocate = 0;
			}
		}
		else{ 
			if(goalsToReallocate > 3){
				loss.setNumPlayerGoals(goalsScored + 4);
				goalsToReallocate -= 4;
			}
			else {
				loss.setNumPlayerGoals(goalsScored + goalsToReallocate);
				goalsToReallocate = 0;
			}
		}
	}

	if(round > 1) {
		List<Double> averageRank = new ArrayList<Double>(gameHistory.getAllAverageRankingsMap().get(round-1).values());
		for(int i = 0; i < 9; i++) {
			int opoID = i;
			if(i >= teamID-1) {opoID = opoID + 1;}
			Double opoRank = averageRank.get(opoID);
			Double ourRank = averageRank.get(teamID-1);

			rankedMap.put(gameHistory.getAllGamesMap().get(round - 1).get(teamID).get(i).getID(),(Math.abs(ourRank-opoRank)));
		}
	}

	
	Comparator<Game> rangeComparatorLoss = (Game g1, Game g2) ->
	{return (g1.getNumOpponentGoals()-g1.getNumPlayerGoals()) - (g2.getNumOpponentGoals()-g2.getNumPlayerGoals());};
	Comparator<Game> rangeComparatorWon = (Game g1, Game g2) ->
	{return (g1.getNumPlayerGoals()-g1.getNumOpponentGoals()) - (g2.getNumPlayerGoals()-g2.getNumOpponentGoals());};
	Comparator<Game> rangeComparatorRank = (Game g1, Game g2) ->
	{return (int) Math.round((rankedMap.get(g1.getID()) - rankedMap.get(g2.getID()))*1000);};
	
	Comparator<Game> rangeComparatorScore = (Game g1, Game g2) ->
	{return (g1.getNumPlayerGoals() - g2.getNumPlayerGoals());};

	if(excessGoals > 0){
		try{
			if (drawnGamesWithReallocationCapacity.size() > 1){
				Collections.sort(drawnGamesWithReallocationCapacity, rangeComparatorScore);
				simPrinter.println(drawnGamesWithReallocationCapacity);
			}
		}
		catch(Exception e){
			simPrinter.println("EXCEPTION DRAWS: " + e);
		}

		excessGoals += goalsToReallocate;
		goalsToReallocate = 0;

		for(Game draw : drawnGamesWithReallocationCapacity){
			int goalsToWin = draw.getNumOpponentGoals() - draw.getNumPlayerGoals() + 2;
			if(excessGoals > goalsToWin){
				draw.setNumPlayerGoals(draw.getNumPlayerGoals() + goalsToWin);
				excessGoals -= goalsToWin;
			}
			else {
				draw.setNumPlayerGoals(draw.getNumPlayerGoals() + excessGoals);
				excessGoals = 0;
			}
		}

	}

	excessGoals += goalsToReallocate;
	goalsToReallocate = 0;

	if(excessGoals > 0) {
		try{
			if (wonGames.size() > 1){
				Collections.sort(wonGames, rangeComparatorLoss);
			}  
		}
		catch(Exception e){
			simPrinter.println("EXCEPTION WINS: " + e);
		}
		for(Game win : wonGames){
			int goalsToWin = goalsTaken.get(win.getID());
			if(excessGoals > goalsToWin){
				win.setNumPlayerGoals(win.getNumPlayerGoals() + goalsToWin);
				excessGoals -= goalsToWin;
			}
			else {
				win.setNumPlayerGoals(win.getNumPlayerGoals() + excessGoals);
				excessGoals = 0;
			}
		}

	}

	if(excessGoals != 0) {
		try{
			if (lostOrDrawnGames.size() > 1){
				Collections.sort(lostOrDrawnGames, rangeComparatorRank);
			}  
		}
		catch(Exception e){
			simPrinter.println(e);
		}
		for(Game loss : lostOrDrawnGames){
			int goalsToWin =loss.getNumOpponentGoals() - loss.getNumPlayerGoals();
			if(goalsToWin > 0){
				if(excessGoals > goalsToWin){
					loss.setNumPlayerGoals(loss.getNumPlayerGoals() + goalsToWin);
					excessGoals -= goalsToWin;
				}
				else {
					loss.setNumPlayerGoals(loss.getNumPlayerGoals() + excessGoals);
					excessGoals = 0;
				}
			}
		}
	}


	reallocatedPlayerGames.addAll(drawnGamesWithReallocationCapacity);
	reallocatedPlayerGames.addAll(wonGames);
	reallocatedPlayerGames.addAll(drawnGames);
	reallocatedPlayerGames.addAll(lostGames);

	List<Game> finalList = new ArrayList<>(reallocatedPlayerGames);
	List<Game> targetList = new ArrayList<>();
	int margin, opMargin, possibleGoals, neededGoals;
	margin = opMargin = possibleGoals = neededGoals = 0;

	//Put target games that aren't wins with margin == 1 in targetList
	for (Game game : finalList){
		for (Integer targetID : targetTeams){
			if (game.getID().equals(targetID)){
				simPrinter.println("targetID: " + targetID + " gameID: " + game.getID() + " score: " + game.getScoreAsString());
				margin = game.getNumPlayerGoals() - game.getNumOpponentGoals();
				if (margin == 1)
					continue;
				else if (margin > 1)
					possibleGoals += margin - 1;
				else if (margin == 0){
					if (game.maxPlayerGoalsReached())
						continue;
					neededGoals += 1;
				}
				else if (margin < 0){
					if (game.maxOpponentGoalsReached())
						neededGoals += (margin * -1); 
					else
						neededGoals += (margin * -1) + 1;
				}
				targetList.add(game);
			}
		}
	}
	for (Game game : targetList)
		finalList.remove(game);
	for (Game game : finalList)
		simPrinter.println("FINAL LIST: " + game.getID() + " " + game.getScoreAsString());
	for (Game game : targetList)
		simPrinter.println("TARGET LIST: " + game.getID() + " " + game.getScoreAsString());
	simPrinter.println("POSSIBLE: " + possibleGoals + " NEEDED: " + neededGoals + " EXCESS: " + excessGoals);
	
	if (possibleGoals >= neededGoals && possibleGoals != 0){
		for (Game game : targetList){
			margin = game.getNumPlayerGoals() - game.getNumOpponentGoals();
			if (margin > 1){
				excessGoals += margin - 1;
				game.setNumPlayerGoals(game.getNumPlayerGoals() - margin + 1);
			}
		}
		for (Game game : targetList){
			margin = game.getNumPlayerGoals() - game.getNumOpponentGoals();
			if (margin < 1){
				excessGoals -= margin + 1;
				game.setNumPlayerGoals(game.getNumPlayerGoals() + margin + 1);
			}
		}
		/**
		* if possible goals > neededGoals, reallocate to non-target games.
		* 	1. try to convert draws first to wins with margin 1
		*	2. Increase any remaining win margins of 1 to margin 2
		*	3. try to convert losses to wins
		*/
		while (excessGoals > 0){
			for (int i = 0; i < 10; i++){
				for (Game game : finalList){
					if (excessGoals == 0)
						break;
					if (game.maxPlayerGoalsReached())
						continue;
					opMargin = game.getNumOpponentGoals() - game.getNumPlayerGoals();

					//steps #1 and #2
					if (opMargin == 0 || i == 1){
						if (i == 0 || opMargin == -1){
							excessGoals -= 1;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + 1);
						continue;
						}
					}
					//step #3
					if (opMargin == i - 1){
						if (excessGoals >= i){
							excessGoals -= i;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + i);
						}
						else{
							excessGoals = 0;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + excessGoals);
						}
					}
				}
			}
		}
	}

	for (Game game : finalList)
		simPrinter.println("FINAL LIST: " + game.getID() + " " + game.getScoreAsString());
	for (Game game : targetList)
		simPrinter.println("TARGET LIST: " + game.getID() + " " + game.getScoreAsString());
	simPrinter.println("POSSIBLE: " + possibleGoals + " NEEDED: " + neededGoals + " EXCESS: " + excessGoals);
	/**
	*	TO DO:if targetList doesn't have enough points to "fix itself", we'll
	*	consider reallocating points from our finalList games in the following order:
		0.  if excess goals remain, first try to use them for a win in targetList.
	* 	1.	For all draws in target list, realloc one point from any game in finalList 
	*		that has margin >2, then if none, take from win with margin >1. if none, forget it. 
	*	2.  survey losses in targeList up to margin i; for each loss with margin i, find win 
	*		in finalList with win margin > i + 2, and realloc 2 points to loss in targetList.
	*		if no such win exists, look for win with margin i + 1 and realloc 2 points to 
	*		loss in targetList.
	*
	*/		
	if (neededGoals > possibleGoals){
		//farm for possible goals first
		if (possibleGoals > 0){
			for (Game game : targetList){
				margin = game.getNumPlayerGoals() - game.getNumOpponentGoals();
				if (margin > 1){
					excessGoals += margin - 1;
					game.setNumPlayerGoals(game.getNumPlayerGoals() - margin + 1);
				}
			}
			neededGoals -= excessGoals;
			simPrinter.println("TARGET FARMED = EXCESS: " + excessGoals);
		}
		

		while (excessGoals > 0 && neededGoals > 0){
			simPrinter.println("DISTRIBUTING EXCESS: " + excessGoals);
			for (int j = 0; j < 10; j++){
				for (Game game : targetList){
					if (excessGoals == 0)
						break;
					if (game.maxPlayerGoalsReached())
						continue;
					opMargin = game.getNumOpponentGoals() - game.getNumPlayerGoals();

					//draws first
					if (opMargin == 0){
						excessGoals -= 1;
						game.setNumPlayerGoals(game.getNumPlayerGoals() + 1);
						neededGoals -= 1;
						continue;
					}
					//the rest after 
					if (opMargin == j && j != 0){
						if (excessGoals >= j + 1){
							excessGoals -= j + 1;
							neededGoals -= j + 1;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + j + 1);
						}
						else{
							game.setNumPlayerGoals(game.getNumPlayerGoals() + excessGoals);
							neededGoals -= excessGoals;
							excessGoals = 0;
						}
					}
				}
			}
		}
			

		simPrinter.println("FINAL FARM = NEEDED: " + neededGoals + " EXCESS: " + excessGoals);
		
		for (int i = 2; i > 0; i--){
			for (Game finalGame : finalList){
				margin =finalGame.getNumPlayerGoals() - finalGame.getNumOpponentGoals();
				if (margin > i && neededGoals > 0){
					excessGoals += 1;
					neededGoals -= 1;
					finalGame.setNumPlayerGoals(finalGame.getNumPlayerGoals() - 1);
				}
			}
			simPrinter.println("FARM MARGIN " + i + " = " + " NEEDED: " + neededGoals + " EXCESS: " + excessGoals);
			while (excessGoals > 0 && neededGoals > 0){
				simPrinter.println("DISTRIBUTING EXCESS: " + excessGoals);
				for (int j = 0; j < 9; j++){
					for (Game targetGame : targetList){
						if (excessGoals == 0)
							break;
						if (targetGame.maxPlayerGoalsReached())
							continue;
						opMargin = targetGame.getNumOpponentGoals() - targetGame.getNumPlayerGoals();

						//draws first
						if (opMargin == 0){
							excessGoals -= 1;
							targetGame.setNumPlayerGoals(targetGame.getNumPlayerGoals() + 1);
							neededGoals -= 1;
							continue;
						}
						//the rest after 
						if (opMargin == j){
							if (excessGoals >= j + 1){
								excessGoals -= j + 1;
								neededGoals -= j + 1;
								if (targetGame.maxOpponentGoalsReached())
									targetGame.setNumPlayerGoals(targetGame.getNumPlayerGoals() + 1);
								else
									targetGame.setNumPlayerGoals(targetGame.getNumPlayerGoals() + j + 1);
							}
							else{
								targetGame.setNumPlayerGoals(targetGame.getNumPlayerGoals() + excessGoals);
								neededGoals -= excessGoals;
								excessGoals = 0;
							}
						}
					}
				}
			}
			simPrinter.println("FARM DIST " + i + " = " + " NEEDED: " + neededGoals + " EXCESS: " + excessGoals);
		}
		while (excessGoals > 0){
			simPrinter.println("DISTRIBUTING EXCESS TO FINAL: " + excessGoals);
			for (int j = 0; j < 9; j++){
				for (Game game : finalList){
					if (excessGoals == 0)
						break;
					if (game.maxPlayerGoalsReached())
						continue;
					opMargin = game.getNumOpponentGoals() - game.getNumPlayerGoals();

					//draws first
					if (opMargin == 0){
						excessGoals -= 1;
						game.setNumPlayerGoals(game.getNumPlayerGoals() + 1);
						continue;
					}
					//try to take losses to wins first
					else if (opMargin == j){
						if (excessGoals >= j + 1){
							excessGoals -= j + 1;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + j + 1);
						}
					}
					//increase win margins next
					else if (opMargin == -j){
						if (excessGoals > 0){
							excessGoals -= 1;
							game.setNumPlayerGoals(game.getNumPlayerGoals() + j + 1);
						}
					}
				}
			}
		}
	}
	for (Game game : finalList)
		simPrinter.println("FINAL LIST: " + game.getID() + " " + game.getScoreAsString());
	for (Game game : targetList)
		simPrinter.println("TARGET LIST: " + game.getID() + " " + game.getScoreAsString());
	simPrinter.println("POSSIBLE: " + possibleGoals + " NEEDED: " + neededGoals + " EXCESS: " + excessGoals);

	for(Game game : targetList)
		finalList.add(game);
 
	if(checkConstraintsSatisfied(playerGames, finalList)) {
		simPrinter.println("CONSTRAINS G2G: " + round);
		return finalList;
	}
	simPrinter.println(checkConstraintsSatisfied2(playerGames, finalList));
	for (Game game : playerGames){
		simPrinter.println("Game " + game.getID() + ": " +game.getScoreAsString());
		for (Game fgame : finalList)
			if(fgame.getID() == game.getID())
				simPrinter.println("Change: " + fgame.getScoreAsString() + "\n");
	}
	simPrinter.println("returning unchanged");
	return playerGames;
}

public static String checkConstraintsSatisfied2(List<Game> originalPlayerGames, List<Game> reallocatedPlayerGames) {
		
		Map<Integer, Game> originalPlayerGamesMap = new HashMap<>();
		for(Game originalPlayerGame : originalPlayerGames)
			originalPlayerGamesMap.put(originalPlayerGame.getID(), originalPlayerGame);
		Map<Integer, Game> reallocatedPlayerGamesMap = new HashMap<>();
		for(Game reallocatedPlayerGame : reallocatedPlayerGames)
			reallocatedPlayerGamesMap.put(reallocatedPlayerGame.getID(), reallocatedPlayerGame);
		
		int totalNumOriginalPlayerGoals = 0, totalNumReallocatedPlayerGoals = 0;
		for(Game originalPlayerGame : originalPlayerGames) {			
			if(!reallocatedPlayerGamesMap.containsKey(originalPlayerGame.getID()))
				continue;
			Game reallocatedPlayerGame = reallocatedPlayerGamesMap.get(originalPlayerGame.getID());
			boolean isOriginalWinningGame = hasWonGame(originalPlayerGame);
			boolean isOriginalLosingGame = hasLostGame(originalPlayerGame);
			boolean isOriginalDrawnGame = hasDrawnGame(originalPlayerGame);
			
			// Constraint 1
			if(reallocatedPlayerGame.getNumPlayerGoals() < 0 || reallocatedPlayerGame.getNumPlayerGoals() > Game.getMaxGoalThreshold())
				return "CONSTRAINT 1";

			// Constraint 2
			if(!originalPlayerGame.getNumOpponentGoals().equals(reallocatedPlayerGame.getNumOpponentGoals()))
				return "CONSTRAINT 2";
		
			// Constraint 3
			boolean numPlayerGoalsIncreased = reallocatedPlayerGame.getNumPlayerGoals() > originalPlayerGame.getNumPlayerGoals();
			if(isOriginalWinningGame && numPlayerGoalsIncreased)
				return "CONSTRAINT 3";

			// Constraint 4
			int halfNumPlayerGoals = originalPlayerGame.getHalfNumPlayerGoals();
			boolean numReallocatedPlayerGoalsLessThanHalf = 
					reallocatedPlayerGame.getNumPlayerGoals() < (originalPlayerGame.getNumPlayerGoals() - halfNumPlayerGoals);
			if((isOriginalWinningGame || isOriginalDrawnGame) && numReallocatedPlayerGoalsLessThanHalf)
				return "CONSTRAINT 4";
			
			totalNumOriginalPlayerGoals += originalPlayerGame.getNumPlayerGoals();
			totalNumReallocatedPlayerGoals += reallocatedPlayerGame.getNumPlayerGoals();
			
			// Constraint 5
			boolean numPlayerGoalsDecreased = reallocatedPlayerGame.getNumPlayerGoals() < originalPlayerGame.getNumPlayerGoals();
			if(isOriginalLosingGame && numPlayerGoalsDecreased)
				return "CONSTRAINT 5";
			
		}
		
		// Constraint 6
		if(totalNumOriginalPlayerGoals != totalNumReallocatedPlayerGoals)
			return "CONSTRAINT 6";
			
		return "CONSTRAINTS PASSED";
	}
private List<Game> getWinningGames(List<Game> playerGames) {
	List<Game> winningGames = new ArrayList<>();
	for(Game game : playerGames) {
		int numPlayerGoals = game.getNumPlayerGoals();
		int numOpponentGoals = game.getNumOpponentGoals();
		if(numPlayerGoals > numOpponentGoals)
			winningGames.add(game.cloneGame());
	}
	return winningGames;
}

private List<Game> getDrawnGames(List<Game> playerGames) {
	List<Game> drawnGames = new ArrayList<>();
	for(Game game : playerGames) {
		int numPlayerGoals = game.getNumPlayerGoals();
		int numOpponentGoals = game.getNumOpponentGoals();
		if(numPlayerGoals == numOpponentGoals)
			drawnGames.add(game.cloneGame());
	}
	return drawnGames;
}

private List<Game> getLosingGames(List<Game> playerGames) {
	List<Game> losingGames = new ArrayList<>();
	for(Game game : playerGames) {
		int numPlayerGoals = game.getNumPlayerGoals();
		int numOpponentGoals = game.getNumOpponentGoals();
		if(numPlayerGoals < numOpponentGoals)
			losingGames.add(game.cloneGame());
	}
	return losingGames;
} }