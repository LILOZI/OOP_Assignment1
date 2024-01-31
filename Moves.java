public class Moves {
    private ConcretePiece[] _eaten;
    private Position[] _whereEaten;
    private ConcretePiece _moved;
    private Position _from;
    private Position _to;


    public Moves(ConcretePiece piece, Position src, Position dst, ConcretePiece[] killed, Position[] where){
        this._moved = piece;
        this._from = new Position(src);
        this._to = new Position(dst);
        this._eaten = killed.clone();
        this._whereEaten = where.clone();
    }

    public Position get_to() {
        return _to;
    }

    public Position get_from() {
        return _from;
    }

    public Position[] get_whereEaten() {
        return _whereEaten;
    }

    public ConcretePiece get_moved() {
        return _moved;
    }

    public ConcretePiece[] get_eaten() {
        return _eaten;
    }
}
