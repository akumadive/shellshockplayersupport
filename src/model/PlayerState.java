package model;

public class PlayerState {

    public enum PlayerType {
        SELF,
        ENEMY
    }

    private int x;
    private int y;
    private PlayerType type;

    public PlayerState(int x, int y) {
        this(x, y, null);
    }

    public PlayerState(int x, int y, PlayerType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PlayerType getType() {
        return type;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }
}