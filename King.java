public class King extends ConcretePiece{
    public King(ConcretePlayer player, Position pos) {
        super(player, pos,"K7");
    }

    @Override
    public String getType() {
        return "♔";
    }
}
