package ee.taltech.examplegame.server.TMXLoaders;

import com.badlogic.gdx.maps.MapLayer;


public class HijackedMapImageLayer extends MapLayer {

    private float x;
    private float y;

    public HijackedMapImageLayer (float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX () {
        return x;
    }

    public void setX (float x) {
        this.x = x;
    }

    public float getY () {
        return y;
    }

    public void setY (float y) {
        this.y = y;
    }

}
