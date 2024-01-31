public class Pawn extends ConcretePiece{
    protected int _kills;

    public Pawn(ConcretePlayer player, Position pos, String name){
        super(player, pos, name);
        this._kills = 0;
    }

    @Override
    public String getType() {
        if(getOwner().isPlayerOne()){
            return "♙";
        }
        return "♟";
    }
    public void addKill(){
        this._kills++;
    }
}
