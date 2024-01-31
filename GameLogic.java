import java.sql.SQLOutput;
import java.util.*;
import java.util.Iterator;
import java.util.function.DoubleToIntFunction;

public class GameLogic implements PlayableLogic {
    private final int _boardSize = 11;
    private ConcretePiece[][] _board = new ConcretePiece[_boardSize][_boardSize];
    private ConcretePlayer _one;
    private ConcretePlayer _two;
    private boolean _turn;
    private Position _kingPos;
    private Stack<Moves> _moves = new Stack<Moves>();
    private ConcretePiece[] _pieces = new ConcretePiece[37];
    private Map<Position,HashMap<ConcretePiece,Integer>> _map = new HashMap<>();
    private ConcretePlayer _won;
//
    public GameLogic() {
        // Create the two players, set the turn and arrange the pieces on the board
        this._one = new ConcretePlayer(0, true);
        this._two = new ConcretePlayer(0, false);
        this._turn = false;
        arrangeBoard();

    }

    @Override
    public boolean move(Position a, Position b) {
        // Checks if this piece has the right to move
        if (!isTurn(a)) {
            return false;
        }
        // If one of the positions isn't valid just return false
        if (!(inBound(a) && inBound(b))) {
            return false;
        }
        // Make sure the move is not diagonal and that there are no pieces in the way
        if (a.get_y() != b.get_y()) {
            // If both are true then the step is diagonal and that illegal
            if (a.get_x() != b.get_x()) {
                return false;
            } else {
                // If the move is vertical, make sure there are no pieces on the way
                int min = Math.min(a.get_y(), b.get_y()) + 1;
                int max = Math.max(a.get_y(), b.get_y());
                while (min < max) {
                    if (getPieceAtPosition(new Position(a.get_x(), min)) != null) {
                        return false;
                    }
                    min++;
                }
            }
        } else {
            if (a.get_x() != b.get_x()) {
                // If the move is horizontal, make sure there are no pieces on the way
                int min = Math.min(a.get_x(), b.get_x()) + 1;
                int max = Math.max(a.get_x(), b.get_x());
                while (min < max) {
                    if (getPieceAtPosition(new Position(min, a.get_y())) != null) {
                        return false;
                    }
                    min++;
                }
            }
        }

        // If the dst position is a corner and the piece isn't a king illegal
        if (!checkCorner(a, b)) {
            return false;
        }
        // That's not a move
        if (a.equals(b)) {
            return false;
        }
        // Can't take other piece's place.
        if (this.getPieceAtPosition(b) != null) {
            return false;
        }


        // Update the king position if he moved
        if (getPieceAtPosition(a) instanceof King) {
            this._kingPos = new Position(b);
        }

        // Update the 2D array representing the board
        this._board[b.get_y()][b.get_x()] = this.getPieceAtPosition(a);
        this._board[a.get_y()][a.get_x()] = null;
        // Update all the fields of the piece
        getPieceAtPosition(b).addStep(b);
        if(this._map.containsKey(b)){
            if(this._map.get(b).containsKey(getPieceAtPosition(b))){
                this._map.get(b).put(getPieceAtPosition(b),this._map.get(b).get(getPieceAtPosition(b))+1);
            }
            else{
                this._map.get(b).put(getPieceAtPosition(b),1);
            }
        }
        else{
            this._map.put(b,new HashMap<>(Map.of(getPieceAtPosition(b),1)));
        }

        Position[] whereEaten = new Position[3];
        ConcretePiece[] eaten = new ConcretePiece[3];
        int c = 0;

        // Create an array with the other team pieces surrounding current piece
        Position[] neigh = findDanNeigh(b);
        for (int i = 0; i < 4; i++) {
            if (neigh[i] != null) {
                // If the neighbor exists check if you just captured him
                if (capturePawn(neigh[i])) {
                    whereEaten[c] = neigh[i];
                    eaten[c] = getPieceAtPosition(neigh[i]);
                    c++;
                    ((Pawn) getPieceAtPosition(b)).addKill();
                    // Remove captured piece from the board
                    this._board[neigh[i].get_y()][neigh[i].get_x()] = null;
                }
            }
        }
        Moves mov = new Moves(getPieceAtPosition(b),a,b,eaten,whereEaten);
        this._moves.push(mov);
        if(isGameFinished()){
            printLog(this._won);
        }

        // Change turns
        this._turn = !_turn;
        return true;
    }

    @Override
    public ConcretePiece getPieceAtPosition(Position position) {
        if (!inBound(position)) {
            return null;
        }
        return this._board[position.get_y()][position.get_x()];
    }

    @Override
    public ConcretePlayer getFirstPlayer() {
        return this._one;
    }

    @Override
    public ConcretePlayer getSecondPlayer() {
        return this._two;
    }

    @Override
    public boolean isGameFinished() {
        // Player one got his king to the corner and won, or captured all player two pieces
        int[] corners_x = {0, 10};
        int[] corners_y = {0, 10};
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                // Only the king can reach the corner, so if there is a piece at one of the corners that's the king
                if (getPieceAtPosition(new Position(corners_x[i], corners_y[j])) != null) {
                    getFirstPlayer().won();
                    this._won = this._one;
                    return true;
                }
            }
        }
        // If the second player only has 2 pieces left he can't capture the king anymore so first player won
        if(getSecondPlayer().getFP() == 2){
            getFirstPlayer().won();
            this._won = this._one;
        }

        // Player two captured the king and won
        if (captureKing(this._kingPos)) {
            getSecondPlayer().won();
            this._won = this._two;
            return true;
        }
        return false;
    }

    @Override
    public boolean isSecondPlayerTurn() {
        // The _turn variable is set to true when it is the first player turn
        return !this._turn;
    }

    @Override
    public void reset() {
        // Set the turn, erase the board and place all the pieces on it
        this._turn = false;
        this._board = new ConcretePiece[this._boardSize][this._boardSize];
        this._moves.empty();
        arrangeBoard();
    }

    @Override
    public void undoLastMove() {
        if (this._moves.isEmpty()) {
            return;
        }
        int kills = 0;
        Moves last = this._moves.pop();
        Position[] pos = last.get_whereEaten();
        ConcretePiece[] pieces = last.get_eaten();
        set_onBoard(last.get_from(),last.get_moved());
        set_onBoard(last.get_to(),null);
        if (last.get_moved().getType() == "♚") {
            this._kingPos = last.get_from();
        }
        for (int i = 0; i < pos.length; i++) {
            if (pos[i] == null) {
                break;
            }
            kills++;
            if (pieces[i].getType() == "♚") {
                this._kingPos = pos[i];
            }
            set_onBoard(pos[i],pieces[i]);
        }
        Position src = last.get_from();
        Position dst = last.get_to();
        ConcretePiece piece = last.get_moved();
        if(getPieceAtPosition(src) instanceof Pawn){
            ((Pawn) getPieceAtPosition(src))._kills -= kills;
        }
        int diff = Math.abs(src.get_x() - dst.get_x() + src.get_y() - dst.get_y());
        getPieceAtPosition(src)._squares -= diff;

        this._map.get(dst).put(piece, this._map.get(dst).get(piece) -1);
        if(this._map.get(dst).get(piece) == 0){
            this._map.get(dst).remove(piece);
            if(this._map.get(dst).isEmpty()){
                this._map.remove(dst);
                piece._steps.remove(dst);
            }
        }
        this._turn = !this._turn;
    }

    private void set_onBoard(Position pos, ConcretePiece piece){
        this._board[pos.get_y()][pos.get_x()] = piece;
    }

    @Override
    public int getBoardSize() {
        return this._boardSize;
    }

    private void arrangeBoard() {
        // Create player one pieces
        // King
        this._board[5][5] = new King(this._one, new Position(5, 5));
        this._kingPos = new Position(5, 5);
        this._pieces[6] = this._board[5][5];
        this._map.put(_kingPos,new HashMap<>(Map.of(this._pieces[6],1)));
        // Pawns
        this._board[3][5] = new Pawn(this._one, new Position(5, 3), "D1");
        this._pieces[0] = this._board[3][5];
        this._map.put(new Position(5,3),new HashMap<>(Map.of(this._pieces[0],1)));
        this._board[4][4] = new Pawn(this._one, new Position(4, 4), "D2");
        this._pieces[1] = this._board[4][4];
        this._map.put(new Position(4,4),new HashMap<>(Map.of(this._pieces[1],1)));
        this._board[4][5] = new Pawn(this._one, new Position(5, 4), "D3");
        this._pieces[2] = this._board[4][5];
        this._map.put(new Position(5,4),new HashMap<>(Map.of(this._pieces[2],1)));
        this._board[4][6] = new Pawn(this._one, new Position(6, 4), "D4");
        this._pieces[3] = this._board[4][6];
        this._map.put(new Position(6,4),new HashMap<>(Map.of(this._pieces[3],1)));
        this._board[5][3] = new Pawn(this._one, new Position(3, 5), "D5");
        this._pieces[4] = this._board[5][3];
        this._map.put(new Position(3,5),new HashMap<>(Map.of(this._pieces[4],1)));
        this._board[5][4] = new Pawn(this._one, new Position(4, 5), "D6");
        this._pieces[5] = this._board[5][4];
        this._map.put(new Position(4,5),new HashMap<>(Map.of(this._pieces[5],1)));
        this._board[5][6] = new Pawn(this._one, new Position(6, 5), "D8");
        this._pieces[7] = this._board[5][6];
        this._map.put(new Position(6,5),new HashMap<>(Map.of(this._pieces[7],1)));
        this._board[5][7] = new Pawn(this._one, new Position(7, 5), "D9");
        this._pieces[8] = this._board[5][7];
        this._map.put(new Position(7,5),new HashMap<>(Map.of(this._pieces[8],1)));
        this._board[6][4] = new Pawn(this._one, new Position(4, 6), "D10");
        this._pieces[9] = this._board[6][4];
        this._map.put(new Position(4,6),new HashMap<>(Map.of(this._pieces[9],1)));
        this._board[6][5] = new Pawn(this._one, new Position(5, 6), "D11");
        this._pieces[10] = this._board[6][5];
        this._map.put(new Position(5,6),new HashMap<>(Map.of(this._pieces[10],1)));
        this._board[6][6] = new Pawn(this._one, new Position(6, 6), "D12");
        this._pieces[11] = this._board[6][6];
        this._map.put(new Position(6,6),new HashMap<>(Map.of(this._pieces[11],1)));
        this._board[7][5] = new Pawn(this._one, new Position(5, 7), "D13");
        this._pieces[12] = this._board[7][5];
        this._map.put(new Position(5,7),new HashMap<>(Map.of(this._pieces[12],1)));

        // Create player two pieces
        this._board[0][3] = new Pawn(this._two, new Position(3,0),"A1");
        this._pieces[13] = this._board[0][3];
        this._map.put(new Position(3,0),new HashMap<>(Map.of(this._pieces[13],1)));
        this._board[0][4] = new Pawn(this._two, new Position(4,0),"A2");
        this._pieces[14] = this._board[0][4];
        this._map.put(new Position(4,0),new HashMap<>(Map.of(this._pieces[14],1)));
        this._board[0][5] = new Pawn(this._two, new Position(5,0),"A3");
        this._pieces[15] = this._board[0][5];
        this._map.put(new Position(5,0),new HashMap<>(Map.of(this._pieces[15],1)));
        this._board[0][6] = new Pawn(this._two, new Position(6,0),"A4");
        this._pieces[16] = this._board[0][6];
        this._map.put(new Position(6,0),new HashMap<>(Map.of(this._pieces[16],1)));
        this._board[0][7] = new Pawn(this._two, new Position(7,0),"A5");
        this._pieces[17] = this._board[0][7];
        this._map.put(new Position(7,0),new HashMap<>(Map.of(this._pieces[17],1)));
        this._board[1][5] = new Pawn(this._two, new Position(5, 1), "A6");
        this._pieces[18] = this._board[1][5];
        this._map.put(new Position(5,1),new HashMap<>(Map.of(this._pieces[18],1)));


        this._board[3][0] = new Pawn(this._two, new Position(0,3),"A7");
        this._pieces[19] = this._board[3][0];
        this._map.put(new Position(0,3),new HashMap<>(Map.of(this._pieces[19],1)));
        this._board[4][0] = new Pawn(this._two, new Position(0,4),"A9");
        this._pieces[21] = this._board[4][0];
        this._map.put(new Position(0,4),new HashMap<>(Map.of(this._pieces[21],1)));
        this._board[5][0] = new Pawn(this._two, new Position(0,5),"A11");
        this._pieces[23] = this._board[5][0];
        this._map.put(new Position(0,5),new HashMap<>(Map.of(this._pieces[23],1)));
        this._board[5][1] = new Pawn(this._two, new Position(1,5),"A12");
        this._pieces[24] = this._board[5][1];
        this._map.put(new Position(1,5),new HashMap<>(Map.of(this._pieces[24],1)));
        this._board[6][0] = new Pawn(this._two, new Position(0,6),"A15");
        this._pieces[27] = this._board[6][0];
        this._map.put(new Position(0,6),new HashMap<>(Map.of(this._pieces[27],1)));
        this._board[7][0] = new Pawn(this._two, new Position(0,7),"A17");
        this._pieces[29] = this._board[7][0];
        this._map.put(new Position(0,7),new HashMap<>(Map.of(this._pieces[29],1)));


        this._board[3][10] = new Pawn(this._two, new Position(10,3),"A8");
        this._pieces[20] = this._board[3][10];
        this._map.put(new Position(10,3),new HashMap<>(Map.of(this._pieces[20],1)));
        this._board[4][10] = new Pawn(this._two, new Position(10,4),"A10");
        this._pieces[22] = this._board[4][10];
        this._map.put(new Position(10,4),new HashMap<>(Map.of(this._pieces[22],1)));
        this._board[5][9] = new Pawn(this._two, new Position(9,5),"A13");
        this._pieces[25] = this._board[5][9];
        this._map.put(new Position(9,5),new HashMap<>(Map.of(this._pieces[25],1)));
        this._board[5][10] = new Pawn(this._two, new Position(10,5),"A14");
        this._pieces[26] = this._board[5][10];
        this._map.put(new Position(10,5),new HashMap<>(Map.of(this._pieces[26],1)));
        this._board[6][10] = new Pawn(this._two, new Position(10,6),"A16");
        this._pieces[28] = this._board[6][10];
        this._map.put(new Position(10,6),new HashMap<>(Map.of(this._pieces[28],1)));
        this._board[7][10] = new Pawn(this._two, new Position(10,7),"A18");
        this._pieces[30] = this._board[7][10];
        this._map.put(new Position(10,7),new HashMap<>(Map.of(this._pieces[30],1)));


        this._board[9][5] = new Pawn(this._two, new Position(5,9),"A19");
        this._pieces[31] = this._board[9][5];
        this._map.put(new Position(5,9),new HashMap<>(Map.of(this._pieces[31],1)));
        this._board[10][3] = new Pawn(this._two, new Position(3,10),"A20");
        this._pieces[32] = this._board[10][3];
        this._map.put(new Position(3,10),new HashMap<>(Map.of(this._pieces[32],1)));
        this._board[10][4] = new Pawn(this._two, new Position(4,10),"A21");
        this._pieces[33] = this._board[10][4];
        this._map.put(new Position(4,10),new HashMap<>(Map.of(this._pieces[33],1)));
        this._board[10][5] = new Pawn(this._two, new Position(5,10),"A22");
        this._pieces[34] = this._board[10][5];
        this._map.put(new Position(5,10),new HashMap<>(Map.of(this._pieces[34],1)));
        this._board[10][6] = new Pawn(this._two, new Position(6,10),"A23");
        this._pieces[35] = this._board[10][6];
        this._map.put(new Position(6,10),new HashMap<>(Map.of(this._pieces[35],1)));
        this._board[10][7] = new Pawn(this._two, new Position(7,10),"A24");
        this._pieces[36] = this._board[10][7];
        this._map.put(new Position(7,10),new HashMap<>(Map.of(this._pieces[36],1)));
    }

    // Checks if the position inside the playing field
    private boolean inBound(Position pos) {
        if (pos == null) {
            return false;
        }
        // If x or y are higher than 11 they are outside the board
        if (pos.get_x() >= 11 || pos.get_y() >= 11 || pos.get_x() < 0 || pos.get_y() < 0) {
            return false;
        }
        return true;
    }

    private boolean checkCorner(Position a, Position b) {
        // If the position is one of the corners than it isn't accessible for pawns, only for the king
        if (b.equals(0, 0) || b.equals(0, 10) || b.equals(10, 0) ||
                b.equals(10, 10)) {
            if (getPieceAtPosition(a) != null) {
                if (getPieceAtPosition(a) instanceof Pawn) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    private boolean isTurn(Position pos) {
        // Get the piece's player
        Player temp = new ConcretePlayer(getPieceAtPosition(pos).getOwner());
        // isPlayerOne return true for player one, _turn is true if it's player one turn
        if (temp.isPlayerOne()) {
            if (this._turn) {
                return true;
            }
        }
        // The piece belongs to player two
        else {
            // Not turn is like isSecondPlayerTurn
            if (!this._turn) {
                return true;
            }
        }
        return false;
    }

    // Return an array with the surrounding pawns that can capture you
    private Position[] findDanNeigh(Position pos) {
        Position[] ans = new Position[4];
        // Neighbors with the same x or y value, not diagonal neighbors
        int[] xUpdate = {0, 0, -1, 1};
        int[] yUpdate = {1, -1, 0, 0};
        for (int i = 0; i < 4; i++) {
            Position temp = new Position(pos.get_x() + xUpdate[i], pos.get_y() + yUpdate[i]);
            if (getPieceAtPosition(temp) != null) {
                // If there is an existing piece, and it belongs to the opposite player
                if (getPieceAtPosition(temp).getOwner().isPlayerOne() ^ getPieceAtPosition(pos).getOwner().isPlayerOne()) {
                    // If the piece is a pawn remember it, a king can't capture so we don't remember him
                    if (getPieceAtPosition(temp) instanceof Pawn) {
                        ans[i] = temp;
                    }
                }
            }
        }
        return ans;
    }

    // Check if a piece has "it's back to the wall"
    private boolean isEdgeX(Position pos) {
        if (pos.get_x() == 0 || pos.get_x() == 10) {
            return true;
        }
        if(pos.equals(1,0) || pos.equals(9,0)){
            return true;
        }
        return false;
    }

    // Checks if a piece has "it's back to the wall"
    private boolean isEdgeY(Position pos) {
        if (pos.get_y() == 0 || pos.get_y() == 10) {
            return true;
        }
        if(pos.equals(10,1) || pos.equals(9,10)){
            return true;
        }
        return false;
    }

    private boolean capturePawn(Position pos) {
        // Find the surrounding pawns
        Position[] neigh = findDanNeigh(pos);
        if (isEdgeX(pos)) {
            // If the pawn is next to a wall need only one pawn to capture it
            if (neigh[2] != null) {
                if(getPieceAtPosition(pos) instanceof Pawn) {
                    // Update kills and deaths and return true
                    getPieceAtPosition(pos).getOwner().pieceCaptured();
                    return true;
                }
            }
            // Same
            if (neigh[3] != null) {
                if(getPieceAtPosition(pos) instanceof Pawn) {
                    getPieceAtPosition(pos).getOwner().pieceCaptured();
                    return true;
                }
            }
        }
        // Same but for "celling" and "floor"
        if (isEdgeY(pos)) {
            if (neigh[0] != null) {
                if (getPieceAtPosition(pos) instanceof Pawn) {
                    getPieceAtPosition(pos).getOwner().pieceCaptured();
                    return true;
                }
            }
            if (neigh[1] != null) {
                if (getPieceAtPosition(pos) instanceof Pawn) {
                    getPieceAtPosition(pos).getOwner().pieceCaptured();
                    return true;
                }
            }
        }
        // If there is no wall then check if the pawn is surrounded horizontally or vertically
        if (neigh[0] != null && neigh[1] != null) {
            if(getPieceAtPosition(pos) instanceof Pawn) {
                getPieceAtPosition(pos).getOwner().pieceCaptured();
                return true;
            }
        }
        if (neigh[2] != null && neigh[3] != null) {
            if(getPieceAtPosition(pos) instanceof Pawn){
                getPieceAtPosition(pos).getOwner().pieceCaptured();
                return true;
                }
            }
        return false;
    }

    private boolean captureKing(Position pos) {
        Position[] neigh = findDanNeigh(pos);
        // If the king is blocked by a wall only three pawn are need to capture him
        boolean edge = isEdgeX(pos) || isEdgeY(pos);
        int sur = 0;
        for (int i = 0; i < 4; i++) {
            if (neigh[i] != null) {
                sur++;
            }
        }
        if (sur == 3 && edge) {
            return true;
        }
        if (sur == 4) {
            return true;
        }
        return false;
    }

    private void printLog(ConcretePlayer won){
        // SOrt by steps
        ConcretePiece[] one = Arrays.copyOfRange(this._pieces,0,13);
        ConcretePiece[] two = Arrays.copyOfRange(this._pieces,13,37);
        Arrays.sort(one, new sortBySteps());
        Arrays.sort(two,new sortBySteps());
        Iterator<ConcretePiece> it1 = Arrays.stream(one).iterator();
        Iterator<ConcretePiece> it2 = Arrays.stream(two).iterator();
        if(won.isPlayerOne()){
            while(it1.hasNext()){
                ConcretePiece temp = it1.next();
                if(temp._steps.size() > 1){
                    System.out.println(temp);
                }
            }
            while(it2.hasNext()){
                ConcretePiece temp = it2.next();
                if(temp._steps.size() > 1){
                    System.out.println(temp);
                }
            }
        }
        else{
            while(it2.hasNext()){
                ConcretePiece temp = it2.next();
                if(temp._steps.size() > 1){
                    System.out.println(temp);
                }
            }
            while(it1.hasNext()){
                ConcretePiece temp = it1.next();
                if(temp._steps.size() > 1){
                    System.out.println(temp);
                }
            }
        }
        // Sort by kills
        // put the winner's pieces first and then our comparator with Java's stable sort
        System.out.println("***************************************************************************");
        if(!won.isPlayerOne()){
            Collections.reverse(Arrays.asList(this._pieces));
        }
        Arrays.sort(this._pieces, new sortByKills().reversed());
        Iterator<ConcretePiece> it3 = Arrays.stream(this._pieces).iterator();
        while(it3.hasNext()){
            ConcretePiece temp = it3.next();
            if(temp instanceof Pawn){
                if(((Pawn) temp)._kills != 0){
                    System.out.printf("%s: %d kills%n",temp._name,((Pawn) temp)._kills);
                }
            }
        }
        // Sort by squares
        System.out.println("***************************************************************************");
        Arrays.sort(this._pieces, new sortBySquares().reversed());
        it3 = Arrays.stream(this._pieces).iterator();
        while(it3.hasNext()){
            ConcretePiece temp = it3.next();
            if(temp._squares != 0){
                System.out.printf(("%s: %d squares%n"),temp._name,temp._squares);
            }
        }
        // Sort by stepped
        System.out.println("***************************************************************************");
        ArrayList<Position> posArr = new ArrayList<>();
        for(Map.Entry<Position,HashMap<ConcretePiece,Integer>> entry : this._map.entrySet()){
            if(entry.getValue().size() > 1){
                posArr.add(entry.getKey());
            }
        }
        posArr.sort((Position o1, Position o2) -> {
                int ans = Integer.compare(this._map.get(o2).values().size(),this._map.get(o1).values().size());
                if(ans == 0){
                    ans = Integer.compare(o1.get_x(),o2.get_x());
                    if(ans == 0){
                        ans = Integer.compare(o1.get_y(),o2.get_y());
                    }
                }
                return ans;
        });
//        Collections.reverse(posArr);
        for(Position position : posArr){
            System.out.println(String.format("%s%s pieces",position,this._map.get(position).size()));
        }
        System.out.println("***************************************************************************");
    }
}
