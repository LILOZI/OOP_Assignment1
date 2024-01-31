import java.util.Objects;

public class Position {
    private final int _x;
    private final int _y;


    Position(int x, int y){
        this._x = x;
        this._y = y;
    }

    Position(Position pos){
        this._x = pos.get_x();
        this._y = pos.get_y();
    }


    public String toString(){
        return String.format("(%d, %d)",this._x,this._y);
    }

    public boolean equals(int x, int y){
        return this._x == x && this._y == y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return _x == position._x && _y == position._y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_x, _y);
    }

    public int get_x() {
        return this._x;
    }

    public int get_y() {
        return this._y;
    }
}
