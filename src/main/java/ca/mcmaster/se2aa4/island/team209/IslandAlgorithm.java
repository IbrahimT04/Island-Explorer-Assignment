package ca.mcmaster.se2aa4.island.team209;


import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;

import java.util.ArrayDeque;
import java.util.Queue;


public class IslandAlgorithm implements ExploreAlgorithm {
    String nearestCreek;
    int distance_from_last_scan, distance_to_edge,distance_to_land;
    Queue<String> decisions = new ArrayDeque<>();
    ExploringDrone drone;
    JSONObject data;
    State state;

    private enum State{
        findWidth,findLand,moveToIsland,scanIsland, stop
    }
    public IslandAlgorithm(String s){
        JSONObject info = new JSONObject(new JSONTokener(new StringReader(s)));
        String given_direction = info.getString("heading");
        Direction direction = switch (given_direction) {
            case "W" -> Direction.W;
            case "N" -> Direction.N;
            case "S" -> Direction.S;
            default -> Direction.E;
        };
        distance_from_last_scan = 0;
        drone = new ExploringDrone(0,0,info.getInt("budget"), direction,0);
        data = new JSONObject();
        state = State.findWidth;
    }
    @Override
    public String decision() {
        if (decisions.isEmpty()){//
            switch(state){
                case findWidth: useRadar(drone.getDirection());
                break;
                case findLand:{
                    if (distance_to_edge > 1){
                        goForward();
                        if (drone.getLastScan() == Direction.right(drone.getDirection())) {
                            useRadar(Direction.left(drone.myDir)); //alternate right and left.
                        }
                        else useRadar(Direction.right(drone.getDirection()));
                        distance_to_edge--;
                    }
                    else {
                        if (distance_from_last_scan==0 &&
                                (drone.getLastScan() == Direction.right(drone.getDirection()))){ // if near a wall and cant turn in direction
                            goDirection(Direction.left(drone.getDirection())); //go other way
                        }
                        else{
                            goDirection(Direction.right(drone.getDirection()));
                        }
                    }

                }
                break;
                case moveToIsland:{
                    if (distance_to_land != 0){
                        goDirection(drone.getLastScan());
                        distance_to_land--;
                    }
                    else {
                        scan();
                        state = State.scanIsland;
                    }
                }
                break;
                case scanIsland:{
                    scan();
                    goForward();
                    scan();
                    goForward();
                    scan();
                    goForward();
                    scan();
                    goForward();
                    decisions.add("");

                }
                break;
                default: return"";
            }
        }
        return decisions.remove();
    }

    @Override
    public void takeInfo(String s) {
        JSONObject mixed_info = new JSONObject(new JSONTokener(new StringReader(s)));
        drone.battery-=mixed_info.getInt("cost");
        data = mixed_info.getJSONObject("extras");
        if (data.has("found")){
            distance_from_last_scan =data.getInt("range");
            if (data.getString("found").equals("OUT_OF_RANGE")&&state == State.findWidth )  {
                distance_to_edge = distance_from_last_scan;
                state = State.findLand;
            }
            else if (data.getString("found").equals("GROUND")){
                state = State.moveToIsland;
                distance_to_land = data.getInt("range");
            }
        }

    }

    @Override
    public String finalReport() {
        return nearestCreek;
    }
    private void goDirection(Direction direction){
        if (direction == drone.getDirection())  {
            goForward();
        }
        else{
            decisions.add("{ \"action\": \"heading\", \"parameters\": { \"direction\": \""+ Direction.toString( direction ) +"\" } }");
            drone.turnRight();
        }

    }
    private void goRight(){
        decisions.add("{ \"action\": \"heading\", \"parameters\": { \"direction\": \""+ Direction.toString( Direction.right(drone.getDirection()) ) +"\" } }");
        drone.turnRight();
    }
    private void goLeft(){
        decisions.add("{ \"action\": \"heading\", \"parameters\": { \"direction\": \""+ Direction.toString( Direction.left(drone.getDirection()) ) +"\" } }");
        drone.turnLeft();
    }
    private void goForward(){
        decisions.add("{ \"action\": \"fly\" }");
        drone.goForward();
    }
    private void useRadar(Direction d){
        decisions.add("{ \"action\": \"echo\", \"parameters\": { \"direction\": \"" + Direction.toString(d) + "\" } }");
        drone.setLastScan(d);
    }
    private void scan(){
        decisions.add("{ \"action\": \"scan\" }");
    }
}
