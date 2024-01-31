public class ConcretePlayer implements Player{
    private int _wins;
    private boolean _player;
    private int _fp;

    ConcretePlayer(int wins, boolean player){
        this._wins = wins;
        this._player = player;
        this._fp = 24;
    }
    ConcretePlayer(Player player){
        this._wins = player.getWins();
        this._player = player.isPlayerOne();
    }

    @Override
    public boolean isPlayerOne() {
        return this._player;
    }

    @Override
    public int getWins() {
        return this._wins;
    }

    public void pieceCaptured(){
        this._fp--;
    }

    public int getFP(){
        return this._fp;
    }

    public void won(){
        this._wins++;
    }
}
