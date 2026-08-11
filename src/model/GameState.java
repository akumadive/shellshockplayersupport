package model;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private PlayerState ownPlayer;
    private List<PlayerState> enemies;

    private int wind;
    private int angle;
    private int power;

    public GameState() {
        enemies = new ArrayList<>();
    }

    public PlayerState getOwnPlayer() {
        return ownPlayer;
    }

    public void setOwnPlayer(PlayerState ownPlayer) {
        this.ownPlayer = ownPlayer;
    }

    public List<PlayerState> getEnemies() {
        return enemies;
    }

    public void addEnemy(PlayerState enemy) {
        enemies.add(enemy);
    }

    public int getWind() {
        return wind;
    }

    public void setWind(int wind) {
        this.wind = wind;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }
}