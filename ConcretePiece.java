import java.util.ArrayList;
import java.util.Comparator;
public abstract class ConcretePiece implements Piece {
    private final ConcretePlayer _player;
    protected final String _name;
    protected int _squares;
    protected ArrayList<Position> _steps = new ArrayList<>();

    public ConcretePiece(ConcretePlayer player, Position pos, String name){
        this._player = new ConcretePlayer(player);
        this._name = name;
        this._squares = 0;
        this._steps.add(pos);
    }

    // Returns this piece Player
    @Override
    public ConcretePlayer getOwner() {
        return this._player;
    }

    public abstract String getType();

    public void addStep(Position pos){
        // Firstly calculate the amount of squares about to move
        if(this._steps.get(this._steps.size()-1).get_x() == pos.get_x()){
            this._squares += Math.abs(this._steps.get(this._steps.size()-1).get_y() - pos.get_y());
        }
        else{
            this._squares += Math.abs(this._steps.get(this._steps.size()-1).get_x() - pos.get_x());
        }
        // Then update current position
        this._steps.add(pos);

    }

    // Create a string representing this piece
    public String toString(){
        return String.format("%s: %s",this._name,this._steps);
    }
}
class sortBySteps implements Comparator<ConcretePiece> {

    // This comparator won't return 0 because the 2nd condition will give definite answer -
    // all the pieces are from the same team so there aren't two pieces with the same number
    @Override
    public int compare(ConcretePiece o1, ConcretePiece o2) {
        // Whoever did more steps is bigger
        if(o1._steps.size() > o2._steps.size()){
            return 1;
        }
        // If they did the same amount of steps decide by their number
        if(o1._steps.size() == o2._steps.size()){
            // The number is the char or 2 chars starting from string[1] till the end
            if( Integer.parseInt(o1._name.substring(1)) > Integer.parseInt(o2._name.substring(1))){
                return 1;
            }
            else{
                return -1;
            }
        }
        else{
            return -1;
        }
    }
}

// These two are the same as the first one, only different by the parameter
class sortByKills implements Comparator<ConcretePiece>{
    @Override
    public int compare(ConcretePiece o1, ConcretePiece o2) {
        if(o1 instanceof Pawn && o2 instanceof Pawn){
            if(((Pawn) o1)._kills > ((Pawn) o2)._kills){
                return 1;
            }
            if(((Pawn) o1)._kills == ((Pawn) o2)._kills){
                if(Integer.parseInt(o1._name.substring(1)) < Integer.parseInt(o2._name.substring(1))){
                    return 1;
                }
                if(Integer.parseInt(o1._name.substring(1)) > Integer.parseInt(o2._name.substring(1))) {
                    return -1;
                }
                return 0;
            }
            return -1;
        }
        if(o1 instanceof King){
            if(((Pawn) o2)._kills > 0){
                return -1;
            }
            else{
                if(Integer.parseInt(o1._name.substring(1)) < 7){
                    return 1;
                }
                if(Integer.parseInt(o1._name.substring(1)) > 7){
                    return -1;
                }
            }
        }
        return 0;
    }
}
class sortBySquares implements Comparator<ConcretePiece>{

    @Override
    public int compare(ConcretePiece o1, ConcretePiece o2){
         if(o1._squares > o2._squares){
             return 1;
         }
         if(o1._squares == o2._squares){
             if(Integer.parseInt(o1._name.substring(1)) < Integer.parseInt(o2._name.substring(1))){
                return 1;
             }
             if(Integer.parseInt(o1._name.substring(1)) > Integer.parseInt(o2._name.substring(1))){
                 return -1;
             }
             return 0;
         }
         return -1;
    }
}