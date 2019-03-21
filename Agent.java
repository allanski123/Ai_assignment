/*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2017
*/

//////////////////////////////////////////////////////////////////////////////////////////////
//
// Briefly describe how your program works, including any algorithms and data structures employed, and explain any design decisions you made along the way.
//   
// My program works as follows and these are the design decisions that i made:
// 1) Firstly, I had to figure out a way to deal with representing the map. To do this, I used a 2d array of characters and updated the corresponding
//    indexes as I explored the 5x5 view with my AI. I used this map as a reference to find specific objectives such as treasure, trees, axes, keys etc.  
// 2) Next, I had to implement A*star search with the Manhattan Heuristic (which is ideal since we have 4 directional movement). To do this, I needed a
//    class called "Node" to store the fcost, gcost and parent Node.
// 3) With A*star working, next I had to figure out a way of exploring the map. In my internal map I used a 159 x 159 2d array (to ensure I could store the potential
//    80 x 80 map provided) and initialized it with all '?' char values. I used A*star search between my current position to positions in the map which were next to a '?' char. 
//    By moving to that point, I would discover what was actually in the '?' position and add it to my internal map. Repeating this process would allow me to fully explore the map. 
// 4) Whilst i explored the map, I also looked out for important objectives such as treasure, keys, axes and trees. (this also used A*star algorithm). 
//    By doing so, I could make my AI more efficient because if it were to see treasure early on, it would simply pick it up and try to path back to the start location.
// 5) Overall I had to make many decisions and the task was enjoyable ! Definitely recommended this assignment for future semesters as I learned so much more about AI and 
//    how great it is when attempting complex tasks. 
//    

import java.util.*;
import java.awt.Point;
import java.io.*;
import java.net.*;
import java.awt.Point;


// node class used for a*search 
class Node {
    Point p;
    int gCost;
    int fCost;
    Node parent;
    
    public Node(Point p, int g, int f) {
    	this.p = p;
    	this.gCost = g;
    	this.fCost = f;
    	this.parent = null;
    }

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public Point getP() {
		return p;
	}

	public void setP(Point p) {
		this.p = p;
	}

	public int getgCost() {
		return gCost;
	}

	public void setgCost(int gCost) {
		this.gCost = gCost;
	}

	public int getfCost() {
		return fCost;
	}

	public void setfCost(int fCost) {
		this.fCost = fCost;
	}
}

public class Agent {
	
	// variables used to keep track of gamestate 
	private Point currPos;
	private int direction;
	private boolean firstTurn = true;
	private ArrayList<Character> allMoves;
	private ArrayList<Point> exploredPoints;
	private ArrayList<Node> pathToUnexplored;
	private char[][] map;
	
	private boolean hasRaft;
	private boolean hasAxe;
	private boolean hasKey;
	private boolean hasGold;
	private int numDynamite;
	private boolean onWater;
	
	// will return a char / move to do 
    public char get_action(char view[][]) {
    	
    	// on first turn of the whole game -> initialize variables and map state
        if(this.firstTurn) {        	
        	initialize(view);
        }
    	
        // updates the current position of the agent
        currPosition();
        
        // changes the view relative to its direction
        if(direction == 1) view = relativeEast(view);
        if(direction == 2) view = relativeSouth(view);
        if(direction == 3) view = relativeWest(view);
        
        // updates internal map if agent has made moves
        if(allMoves.size()-1 >= 0) {
            updateMap(view, allMoves.get(allMoves.size()-1));
        }
        
    	// find points/coordinates to desired objectives
        Point treasure = findMap('$');
        Point key = findMap('k');
        Point dynamite = findMap('d');
        Point axe = findMap('a');
        Point tree = findMap('T');
        Point door = findMap('-');
        Point initialPoint = new Point(80, 80);
        
        ArrayList<Node> currPath = new ArrayList<Node>();
        
        // if we have gold and there is a path to start - go back ! 
        if(hasGold == true && initialPoint != null && 
        		pathFinder(view, currPos, initialPoint) != null && pathOk(pathFinder(view, currPos, initialPoint))) {
            currPath = pathFinder(view, currPos, initialPoint);
        }
        // if see treasure and path is clear - go to it ! 
        else if(treasure != null && pathFinder(view, currPos, treasure) != null && 
        		pathOk(pathFinder(view, currPos, treasure))) {
        	currPath = pathFinder(view, currPos, treasure);
        }
        // if we see key and path is clear - go to it
        else if(key != null && pathFinder(view, currPos, key) != null && !hasKey &&
        		pathOk(pathFinder(view, currPos, key))) {
        	currPath = pathFinder(view, currPos, key);
        }
        // if we see dynamite and path is clear - go to it
        else if(dynamite != null && pathFinder(view, currPos, dynamite) != null &&
        		pathOk(pathFinder(view, currPos, dynamite))) {
        	currPath = pathFinder(view, currPos, dynamite);
        }
        // if we see axe and path is clear - go to it
        else if(axe != null && pathFinder(view, currPos, axe) != null && !hasAxe &&
        		pathOk(pathFinder(view, currPos, axe))) {
        	currPath = pathFinder(view, currPos, axe);
        }
        // if we see a door and we have the key - unlock it
        else if(door != null && pathFinder(view, currPos, door) != null && hasKey &&
        		pathOk(pathFinder(view, currPos, door))) {
        	currPath = pathFinder(view, currPos, door);
        }
        // if we see a tree and have an axe, cut it down ! 
        else if(tree != null && pathFinder(view, currPos, tree) != null && hasAxe &&
        		pathOk(pathFinder(view, currPos, tree))) {
        	currPath = pathFinder(view, currPos, tree);
        } 
        // if not on water - explore the unknown spaces in map
        else if(!onWater){
        	if(explore_world(view) != null && pathToUnexplored.size() == 0) {
        		pathToUnexplored = explore_world(view);
        	}
    		if(pathToUnexplored != null && pathToUnexplored.size() != 0) {
    			char c = pathToAction(view, pathToUnexplored);
    		    allMoves.add(c);
    		    
    		    if(!containsPoint(pathToUnexplored.get(pathToUnexplored.size() - 1).getP(), exploredPoints)) {
    		    	 exploredPoints.add(pathToUnexplored.get(pathToUnexplored.size() - 1).getP());	
    		    }
                pathToUnexplored.remove(pathToUnexplored.size() - 1);
                
    		    return c;
    		}
        }
        
        // if we have a path to an objective - return the char needed to get to that path
    	if(currPath != null && pathToAction(view, currPath) != 0) {
    		allMoves.add(pathToAction(view, currPath));
    		if(!containsPoint(currPath.get(currPath.size() - 1).getP(), exploredPoints)) {
    			exploredPoints.add(currPath.get(currPath.size() - 1).getP());
    		}
    		
    		// if stepping off water - set raft to false and tell agent we are no longer on water
    		boolean shouldExplore = true;
    		if(pathToAction(view, currPath) == 'f') {
	    		if(direction == 0 && map[(int)currPos.getX()][(int)currPos.getY() - 1] == ' '
	    				&& map[(int)currPos.getX()][(int)currPos.getY()] == '~') {
					onWater = false;
					hasRaft = false;
					shouldExplore = hasTree(view, new Point((int)currPos.getX(), (int)currPos.getY() - 1));
				}
				if(direction == 1 && map[(int)currPos.getX() + 1][(int)currPos.getY()] == ' '
						&& map[(int)currPos.getX()][(int)currPos.getY()] == '~') {
					onWater = false;
					hasRaft = false;
					shouldExplore = hasTree(view, new Point((int)currPos.getX() + 1, (int)currPos.getY()));
				}
				if(direction == 2 && map[(int)currPos.getX()][(int)currPos.getY() + 1] == ' '
						&& map[(int)currPos.getX()][(int)currPos.getY()] == '~') {
					onWater = false;
					hasRaft = false;
					shouldExplore = hasTree(view, new Point((int)currPos.getX(), (int)currPos.getY() + 1));
				}
				if(direction == 3 && map[(int)currPos.getX() - 1][(int)currPos.getY()] == ' '
						&& map[(int)currPos.getX()][(int)currPos.getY()] == '~') {
					onWater = false;
					hasRaft = false;
					shouldExplore = hasTree(view, new Point((int)currPos.getX() - 1, (int)currPos.getY()));
				}
    		}    		
    		if(shouldExplore) {
    			return pathToAction(view, currPath);
    		}
    	}
    	// otherwise we have nothing left to do, so try to get onto water ! 
		if(pathToWater(view) != 0 && !onWater) {
			if(map[(int)currPos.getX()][(int)currPos.getY()] == '~') {
				onWater = true;
			} 
			else {
			    allMoves.add(pathToWater(view));
    		    return pathToWater(view);
			}
		}
		
		// when on water - explore '?' unknown and expand map (whilst making sure we still on water)
		if(onWater) {
        	if(explore_water(view) != null && pathToUnexplored.size() == 0) {
        		pathToUnexplored = explore_water(view);
        	}
    		if(pathToUnexplored != null && pathToUnexplored.size() != 0) {
    			char c = pathToAction(view, pathToUnexplored);
    		    allMoves.add(c);
    		    if(!containsPoint(pathToUnexplored.get(pathToUnexplored.size() - 1).getP(), exploredPoints)) {
    		    	 exploredPoints.add(pathToUnexplored.get(pathToUnexplored.size() - 1).getP());	
    		    }
                pathToUnexplored.remove(pathToUnexplored.size() - 1);
    		    return c;
    		}
        }
    	
    	return 0;
    }
    
    public boolean hasTree(char[][] view, Point p) {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			if(map[i][j] == 'T') {
    				if(pathFinder(view, p, new Point(i, j)) != null) {
    					return true;
    				}
    			}
    		}
    	}
    	
    	return false;
    }
    
    // checks if a given path (from a*) is valid
    public boolean pathOk(ArrayList<Node> path) {
    	int count = 0;
    	for(Node n : path) {
    		if(map[(int)n.getP().getX()][(int)n.getP().getY()] == '~') {
    			count++;
    		}
    		if(map[(int)n.getP().getX()][(int)n.getP().getY()] == '*') {
    			return false;
    		}
    	}
    	
    	if(count > 1) {
    		return false;
    	}
    	
    	return true;
    }
    
    // returns a path to a given place on the map which will reveal a '?'
    public ArrayList<Node> explore_water(char[][] view) {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			Point toExplore = new Point(i, j);
    			char atPoint = map[i][j];
    			if(atPoint == '~' && hasUnexplored(toExplore)) {
	    			ArrayList<Node> possiblePath = pathFinder(view, currPos, toExplore);
	    			if(possiblePath != null && pathToAction(view, possiblePath) != 0) {
    				    return possiblePath;
	    			}
    			}
    		}
    	}
        
    	return null;
    }
    
    // returns a move that will take the agent to water
    public char pathToWater(char[][] view) {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			Point toExplore = new Point(i, j);
    			char atPoint = map[i][j];
    			if(atPoint == '~') {
    				ArrayList<Node> possiblePath = pathFinder(view, currPos, toExplore);
    				if(possiblePath != null && pathToAction(view, possiblePath) != 0) {
    					return pathToAction(view, possiblePath);
    				}
    			}
    		}
    	}
    	
        return 0;
    }

    // checks if a coordinate/point is in an arrayList
    public boolean containsPoint(Point input, ArrayList<Point> toCheck) {
    	for(Point p : toCheck) {
    		if((int)p.getX() == (int)input.getX() && (int)p.getY() == (int)input.getY()) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    // returns the path to reveal more of the world
    public ArrayList<Node> explore_world(char[][] view) {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			Point toExplore = new Point(i, j);
    			char atPoint = map[i][j];
    			if(atPoint == ' ' && hasUnexplored(toExplore)) {
	    			ArrayList<Node> possiblePath = pathFinder(view, currPos, toExplore);
	    			if(possiblePath != null && pathToAction(view, possiblePath) != 0) {
    				    return possiblePath;
	    			}
    			}
    		}
    	}
        
    	return null;
    }
    
    // When we have used a* search, this tells the agent the moves required
    // to get to the destination/goal node
    public char pathToAction(char[][] view, ArrayList<Node> path) {
    	
    	Node n = new Node(new Point(0,0), 0, 0);
    	if(path.size() >= 1) {
    		n = path.get(path.size() - 1);
    	}
    	
    	for(int i = 0; i < 5; i++) {
    		for(int j = 0; j < 5; j++) {
    	    	Point p = viewToMapCoords(view, j, i);
    	    	char toFind = map[(int)n.getP().getX()][(int)n.getP().getY()];
    	    	if(((int)p.getX() == (int)n.getP().getX())
    	    			&& ((int)p.getY() == (int)n.getP().getY())) {
	    			if(j == 1 && i == 2) {
	    				if(toFind == '-') {
			    			if(direction == 0) return 'u';
	    				}
	    				if(toFind == 'T') {
			    			if(direction == 0) return 'c';
	    				}
	    				if(toFind == '*') {
			    			if(direction == 0) return 'b';
	    				}
	    				if(toFind == '~') {
	    					if(direction == 0) return 'f';
	    				}

		    			if(direction == 0) return 'f';
	    	            if(direction == 1) return 'l';
	    	            if(direction == 2) return 'l';
	    	            if(direction == 3) return 'r';
	    			}
	    			else if(j == 2 && i == 3) {
	    				if(toFind == '-') {
			    			if(direction == 1) return 'u';
	    				}
	    				if(toFind == 'T') {
			    			if(direction == 1) return 'c';
	    				}
	    				if(toFind == '*') {
			    			if(direction == 1) return 'b';
	    				}
	    				if(toFind == '~') {
	    					if(direction == 1) return 'f';
	    				}
	    	            
	    				if(direction == 0) return 'r';
	    				if(direction == 1) return 'f';
	    	            if(direction == 2) return 'l';
	    	            if(direction == 3) return 'l';
	    			}
	    			else if(j == 3 && i == 2) {
	    				if(toFind == '-') {
			    			if(direction == 2) return 'u';
	    				}
	    				if(toFind == 'T') {
			    			if(direction == 2) return 'c';
	    				}
	    				if(toFind == '*') {
			    			if(direction == 2) return 'b';
	    				}
	    				if(toFind == '~') {
	    					if(direction == 2) return 'f';
	    				}
	    				
	    				if(direction == 0) return 'l';
	    	            if(direction == 1) return 'r';
	    	            if(direction == 2) return 'f';
	    	            if(direction == 3) return 'l';
	    			}
	    			else if(j == 2 && i == 1) {
	    				if(toFind == '-') {
			    			if(direction == 3) return 'u';
	    				}
	    				if(toFind == 'T') {
			    			if(direction == 3) return 'c';
	    				}
	    				if(toFind == '*') {
			    			if(direction == 3) return 'b';
	    				}
	    				if(toFind == '~') {
	    					if(direction == 3) return 'f';
	    				}
	    				
	    				if(direction == 0) return 'l';
	    	            if(direction == 1) return 'l';
	    	            if(direction == 2) return 'r';
	    	            if(direction == 3) return 'f';
	    			}
    	    	}
    		}
    	}
    	
    	return 0;
    }
    
    // gets the shortest path after a* is used
    public ArrayList<Node> shortestPath(Node end) {
    	ArrayList<Node> sPath = new ArrayList<Node>();
    	sPath.add(end);
    	while(end.getParent() != null) {
    		end = end.getParent();
    		sPath.add(end);
    	}
    	sPath.remove(sPath.size() - 1);
    	
    	return sPath;
    }
    
    // A* algorithm 
    public ArrayList<Node> pathFinder(char[][] view, Point start, Point end) {
    	Comparator<Node> comparator = new Comparator<Node>() {
    	    public int compare(Node n1, Node n2) {
    	    	return Integer.compare(n1.getfCost(), n2.getfCost());
    	    }
    	};
    	
    	Node first = new Node(start, 0, manDist(start, end));
    	PriorityQueue<Node> neighbours = new PriorityQueue<Node>(11, comparator);
    	ArrayList<Node> visited = new ArrayList<Node>();
    	
    	neighbours.add(first);
    	while(!neighbours.isEmpty()) {
    		Node curr = neighbours.poll();
    		visited.add(curr);
    		if(curr.getP().getX() == end.getX() &&
    				curr.getP().getY() == end.getY()) {
    			return shortestPath(curr);
    		}
    		
    		ArrayList<Node> currNeighbours = getNeighbours(curr, end);
    		if(currNeighbours == null) return null;
    		for(Node n : currNeighbours) {
    	        if(!hasVisited(n, visited)) {
    	        	n.setfCost(n.getgCost() + manDist(n.getP(), end));
    	        	
    	        	if(inQueue(n, neighbours) == null) {
    	        		neighbours.add(n);
    	        	}
    	        	else {
    	        		Node existingNeighbour = inQueue(n, neighbours);
    	        		if(n.getgCost() < existingNeighbour.getgCost()) {
    	        			existingNeighbour.setgCost(n.getgCost());
    	        			existingNeighbour.setParent(n.getParent());
    	        		}
    	        	}
    	        }
    		}	
    	}
    	
    	return null;
    }
    
    // checks if a point is in a priority queue
    public Node inQueue(Node n, PriorityQueue<Node> pq) {
    	if(pq.isEmpty()) return null; 
    	
    	for(Node curr : pq) {
    		if(curr.getP().getX() == n.getP().getX() &&
    				curr.getP().getY() == n.getP().getY()) {
    			return curr;
    		}
    	}
    	
    	return null;
    }
    
    // checks if we have visited a point
    public boolean hasVisited(Node n, ArrayList<Node> list) {
    	if(list.isEmpty()) return false;
    	for(Node curr : list) {
    		if(curr.getP().getX() == n.getP().getX() &&
    				curr.getP().getY() == n.getP().getY()) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    // checks if a point has a '?' nearby so we know to go over and reveal more of map
    public boolean hasUnexplored(Point curr) {
    	Point p1 = new Point((int)curr.getX(), (int)curr.getY() - 1); 
    	Point p2 = new Point((int)curr.getX() + 1, (int)curr.getY());
    	Point p3 = new Point((int)curr.getX() - 1, (int)curr.getY());
    	Point p4 = new Point((int)curr.getX(), (int)curr.getY() + 1);
    	
    	if(map[(int)p1.getX()][(int)p1.getY()] == '?') return true;
    	if(map[(int)p2.getX()][(int)p2.getY()] == '?') return true;
    	if(map[(int)p3.getX()][(int)p3.getY()] == '?') return true;
    	if(map[(int)p4.getX()][(int)p4.getY()] == '?') return true;
    	
    	return false;
    }
    
    // returns a list of neighbours to a current node
    public ArrayList<Node> getNeighbours(Node n, Point goal) {
    	ArrayList<Node> list = new ArrayList<Node>();
    	char toFind = map[(int)goal.getX()][(int)goal.getY()];
    	Point curr = n.getP();
    	
    	// if we haven't seen goal on map yet
    	if(toFind == '?') {
    		return null;
    	}
    	
    	// relative north
    	Point p1 = new Point((int)curr.getX(), (int)curr.getY() - 1);    	
    	if(((map[(int)p1.getX()][(int)p1.getY()] == ' ' || ((int)p1.getX() == 80 && (int)p1.getY() == 80)
    			|| map[(int)p1.getX()][(int)p1.getY()] == toFind) && !onWater) || map[(int)p1.getX()][(int)p1.getY()] == '~'
    			&& onWater) {

        	Node n1 = new Node(p1, n.getgCost() + 1, 0);
        	n1.setParent(n);	
        	list.add(n1);
    	}
    	else if((map[(int)p1.getX()][(int)p1.getY()] == ' ' || ((int)p1.getX() == 80 && (int)p1.getY() == 80)
    			|| map[(int)p1.getX()][(int)p1.getY()] == toFind)) {
        	Node n1 = new Node(p1, n.getgCost() + 1, 0);
        	n1.setParent(n);	
        	list.add(n1);
    	}
    	
    	// relative east
    	Point p2 = new Point((int)curr.getX() + 1, (int)curr.getY());
    	if(((map[(int)p2.getX()][(int)p2.getY()] == ' ' || ((int)p2.getX() == 80 && (int)p2.getY() == 80)
    			|| map[(int)p2.getX()][(int)p2.getY()] == toFind) && !onWater) || map[(int)p2.getX()][(int)p2.getY()] == '~'
    			&& onWater) {
        	Node n2 = new Node(p2, n.getgCost() + 1, 0);
        	n2.setParent(n);	
        	list.add(n2);
    	}
    	else if((map[(int)p2.getX()][(int)p2.getY()] == ' ' || ((int)p2.getX() == 80 && (int)p2.getY() == 80)
    			|| map[(int)p2.getX()][(int)p2.getY()] == toFind)) {
        	Node n2 = new Node(p1, n.getgCost() + 1, 0);
        	n2.setParent(n);	
        	list.add(n2);
    	}
    	
    	// relative south
    	Point p3 = new Point((int)curr.getX() - 1, (int)curr.getY());
    	if(((map[(int)p3.getX()][(int)p3.getY()] == ' ' || ((int)p3.getX() == 80 && (int)p3.getY() == 80)
    			|| map[(int)p3.getX()][(int)p3.getY()] == toFind) && !onWater) || map[(int)p3.getX()][(int)p3.getY()] == '~'
    			&& onWater) {
        	Node n3 = new Node(p3, n.getgCost() + 1, 0);
        	n3.setParent(n);	
        	list.add(n3);
    	}
    	else if((map[(int)p3.getX()][(int)p3.getY()] == ' ' || ((int)p3.getX() == 80 && (int)p3.getY() == 80)
    			|| map[(int)p3.getX()][(int)p3.getY()] == toFind)) {
        	Node n3 = new Node(p3, n.getgCost() + 1, 0);
        	n3.setParent(n);	
        	list.add(n3);
    	}
    	
    	// relative west
    	Point p4 = new Point((int)curr.getX(), (int)curr.getY() + 1);
    	if(((map[(int)p4.getX()][(int)p4.getY()] == ' ' || ((int)p4.getX() == 80 && (int)p4.getY() == 80) 
    			|| map[(int)p4.getX()][(int)p4.getY()] == toFind) && !onWater) || map[(int)p4.getX()][(int)p4.getY()] == '~'
    			&& onWater) {
        	Node n4 = new Node(p4, n.getgCost() + 1, 0);
        	n4.setParent(n);	
        	list.add(n4);
    	}
    	else if((map[(int)p4.getX()][(int)p4.getY()] == ' ' || ((int)p4.getX() == 80 && (int)p4.getY() == 80)
    			|| map[(int)p4.getX()][(int)p4.getY()] == toFind)) {
        	Node n4 = new Node(p4, n.getgCost() + 1, 0);
        	n4.setParent(n);	
        	list.add(n4);
    	}
    
    	return list;
    }
    
    // heuristic for a* algorithm
    public int manDist(Point start, Point end) {
    	int x = Math.abs((int)start.getX() - (int)end.getX());
    	int y = Math.abs((int)start.getY() - (int)end.getY());
    	
    	return x + y;
    }
    
    // flips the view to maintain relative direction
    public char[][] relativeEast(char view[][]) {
    	
    	char[][] newView = new char[5][5];
    	char temp = ' ';
    	for(int i = 0; i < 5; i++) {
    		for(int j = 4; j >= 0; j--) {
    			if(i == 2 && j == 2) {
    				newView[2][2] = '^';
    			}
    			else {
    				temp = view[j][i];
    				newView[i][4-j] = temp;
    			}
    		}
    	}
    	
    	return newView;
    }
    
    // flips the view to maintain relative direction
    public char[][] relativeSouth(char view[][]) {
    	
    	for(int i = 0; i < 2; i++) {
        	view = relativeEast(view);
    	}
    	
    	return view;
    }
    
    // flips the view to maintain relative direction
    public char[][] relativeWest(char view[][]) {
    	
    	for(int i = 0; i < 3; i++) {
    		view = relativeEast(view);
    	}
    	
    	return view;
    }
    
    // updates the internal map (the 159 x 159 map) 
    public void updateMap(char view[][], char move) {
    	
		Point p5 = null;
		if(direction == 0) p5 = viewToMapCoords(view, 1, 2);
		if(direction == 1) p5 = viewToMapCoords(view, 2, 3);
    	if(direction == 2) p5 = viewToMapCoords(view, 3, 2);
    	if(direction == 3) p5 = viewToMapCoords(view, 2, 1);
    	
    	if(move == 'f') {
    		int xPos = 0;
    		int yPos = 0;
    		int xInc = 0;
    		int yInc = 0;
    		if(direction == 0) {
    			yInc = 1;
    		}
    		if(direction == 1) {
    			yPos = 4;
    			xInc = 1;
    		}
    		if(direction == 2) {
    			xPos = 4;
    			yInc = 1;
    		}
    		if(direction == 3) {
    			xInc = 1;
    		}
    		
    		
    		for(int i = 0; i < 5; i++) {
    			Point temp;
    			if(i == 0) {
    				temp = viewToMapCoords(view, xPos, yPos);
        			map[(int)temp.getX()][(int)temp.getY()] = view[xPos][yPos];
    			}
    			else {
    				temp = viewToMapCoords(view, xPos + xInc, yPos + yInc);
        			map[(int)temp.getX()][(int)temp.getY()] = view[xPos + xInc][yPos + yInc];
        			xPos += xInc;
        			yPos += yInc;
    			}
    		}
    		print_view(view);
    		
    		p5 = viewToMapCoords(view, 2, 2);
			int x = (int)p5.getX();
			int y = (int)p5.getY();
			if(map[x][y] == 'k' || map[x][y] == '$' || 
					map[x][y] == 'a' || map[x][y] == 'd') {
				
				if(map[x][y] == 'k') hasKey = true;
				if(map[x][y] == '$') hasGold = true;
				if(map[x][y] == 'a') hasAxe = true;
				if(map[x][y] == 'd') numDynamite++; 
				
				map[x][y] = ' ';
			}
    	}
    	else if(move == 'b' || move == 'c' || move == 'u') {
    		map[(int)p5.getX()][(int)p5.getY()] = ' ';
    		if(move == 'b') {
    			numDynamite--;
    		}
    		if(move == 'c') {
    			hasRaft = true;
    		}
    	}
    }
    
    // prints the internal map representation
    public void printMap() {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			if((j > 60 && j < 100) && i > 60 && i < 100) {
        			if(i == currPos.getX() && j == currPos.getY()) {
        				System.out.print(" ");
        			}
        			else {
    			        System.out.print(map[i][j]);
        			}
    			}
    		}
    		if(i > 60 && i < 100) {
    		    System.out.println();
    		}
    	}
    }
    
    // initialize the game state at beginning of game
    public void initialize(char view[][]) {
    	this.currPos = new Point(80, 80);
    	this.direction = 0;
    	this.firstTurn = false;
    	this.onWater = false;
    	allMoves = new ArrayList<Character>();
    	exploredPoints = new ArrayList<Point>();
    	pathToUnexplored = new ArrayList<Node>();
    	map = new char[159][159];
    	for(int i = 0; i < 159; i++) {
    		Arrays.fill(map[i], '?');
    	}
    	this.hasRaft = false;
    	this.hasAxe = false;
    	this.numDynamite = 0;
    	this.hasKey = false;
    
    	for(int i = 0; i < 5; i++) {
    		for(int j = 0; j < 5; j++) {
	    		Point p = viewToMapCoords(view, i, j);
	    		map[(int)p.getX()][(int)p.getY()] = view[i][j];
    		}
    	}    	
    	
    	printMap();
    }
    
    // takes in x,y coords of view and converts to a point on the map
    public Point viewToMapCoords(char[][] view, int i, int j) {
		int x = (int)currPos.getX();
		int y = (int)currPos.getY();
		
	    int right_left = i - 2;
	    int up_down = j - 2; 
				
		Point found = new Point(0, 0);
		found.setLocation(x - up_down, y - right_left);

    	return found;
    }
    
    // takes in a character to find and returns its coordinates
    public Point findCoordinates(char[][] view, char toFind) {
    	for(int i = 0; i < 5; i++) {
    		for(int j = 0; j < 5; j++) {
    			if(view[i][j] == toFind) {
    				int x = (int)currPos.getX();
    				int y = (int)currPos.getY();
    				
				    int right_left = i - 2;
	    		    int up_down = j - 2; 
    	    				
    				Point found = new Point(0, 0);
    				found.setLocation(x - up_down, y - right_left);
    				
    			    return found;
    			}
    		}
    	}
    	
    	return null;
    }
    
    // finds a char in the map representation
    public Point findMap(char toFind) {
    	for(int i = 0; i < 159; i++) {
    		for(int j = 0; j < 159; j++) {
    			if(map[i][j] == toFind) {
    				
    				return (new Point(i, j));
    			}
    		}
    	}
    	
    	return null;
    }
    
    // gets current location relative to start and sets direction to current facing direction
    public void currPosition() {
    	direction = 0;
    	Point relative = new Point(80, 80);
    	for(char c : allMoves) {
    		int x = (int)relative.getX();
    		int y = (int)relative.getY();
    		
    		if(c == 'r') {
    			direction = (direction + 1) % 4;
    		}
    		else if(c == 'l') {
    			direction = (direction + 3) % 4;
    		}
    		else if(c == 'f') {
    			if(direction == 0) relative.setLocation(x, y + 1);
    			if(direction == 1) relative.setLocation(x - 1, y);
    			if(direction == 2) relative.setLocation(x, y - 1);
    			if(direction == 3) relative.setLocation(x + 1, y);
    		}
    		
    	}
    	
    	this.currPos = relative; 
    }
    
    // checks if a move is valid
    public boolean isValidMove(char view[][], char move) {
    	char next_Obj = view[1][2];
    	if(direction == 1) next_Obj = view[2][3];
    	if(direction == 2) next_Obj = view[3][2];
    	if(direction == 3) next_Obj = view[2][1];
    	
    	if(move == 'l' || move == 'r') return true;
    	if(move == 'f') {
    		if(next_Obj == ' ' || next_Obj == 'd' || next_Obj == 'k' || next_Obj == 'a' || next_Obj == '$') {
    			return true;
    		}
    		else if(next_Obj == '~' && hasRaft) {
    			return true;
    		}
    	}
    	if(move == 'c') {
    	    if(next_Obj == 'T' && hasAxe) {
    	    	return true;
    	    }
    	}
    	if(move == 'b') {
    	    if((next_Obj == 'T' || next_Obj == '*') && numDynamite > 0) {
    	        return true;	
    	    }
    	}
    	if(move == 'u') {
    		if(next_Obj == '-' && hasKey) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    // finds a list of objectives in the view
    public ArrayList<Character> find_objectives(char view[][]) {
    	ArrayList<Character> objList = new ArrayList<Character>();
    	
    	for(int i = 0; i < 5; i++) {
    		for(int j = 0; j < 5; j++) {
    			char ch = view[i][j];
    			switch(ch) {
    			    case 'd':
    			    case 'k': 
    			    case 'a':
    			    case '$':
    			    case 'T':
    			    	objList.add(ch);
    			    	break;
    			    default: 
    			    	break;		    
    			}
    		}
    	}
    	
    	return objList;
    }

    // prints the view
    void print_view( char view[][] ) {
      
        int i,j;

        System.out.println("\n+-----+");
        for( i=0; i < 5; i++ ) {
            System.out.print("|");
            for( j=0; j < 5; j++ ) {
                if(( i == 2 )&&( j == 2 )) {
                   System.out.print('^');
                }
                else {
                   System.out.print( view[i][j] );
                }
            }
            System.out.println("|");
        }
        System.out.println("+-----+");
    }

    public static void main( String[] args ) {
        InputStream in  = null;
        OutputStream out= null;
        Socket socket   = null;
        Agent  agent    = new Agent();
        char   view[][] = new char[5][5];
        char   action   = 'F';
        int port;
        int ch;
        int i,j;

        if( args.length < 2 ) {
            System.out.println("Usage: java Agent -p <port>\n");
            System.exit(-1);
        }

        port = Integer.parseInt( args[1] );

        try { // open socket to Game Engine
            socket = new Socket( "localhost", port );
            in  = socket.getInputStream();
            out = socket.getOutputStream();
        }
        catch( IOException e ) {
            System.out.println("Could not bind to port: "+port);
            System.exit(-1);
        }

        try { // scan 5-by-5 wintow around current location
            while( true ) {
                for( i=0; i < 5; i++ ) {
                    for( j=0; j < 5; j++ ) {
                        if( !(( i == 2 )&&( j == 2 ))) {
	                        ch = in.read();
	                        if( ch == -1 ) {
	                            System.exit(-1);
	                        }
	                        view[i][j] = (char) ch;
                        }
                    }
                }    
                //agent.print_view( view ); // COMMENT THIS OUT BEFORE SUBMISSION
                action = agent.get_action( view );
                out.write( action );
            }    
        }
        catch( IOException e ) {
            System.out.println("Lost connection to port: "+ port );
            System.exit(-1);
        }
        finally {
            try {
                socket.close();
            }
            catch( IOException e ) {}
        }
    }
}
