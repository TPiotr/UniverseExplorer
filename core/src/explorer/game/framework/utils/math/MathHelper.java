package explorer.game.framework.utils.math;

/**
 * Created by RYZEN on 04.12.2017.
 */

public class MathHelper {

    /**
     * Function for checking if two rectangles (first x1,y1,w1,h1 second is x2,y2,w2,h2) are overlapping
     * Workaround for normal Rectangle class because this do not need Rectangle instances
     * @param x1 x pos of first rectangle
     * @param y1 y pos of first rectangle
     * @param w1 width of first rectangle
     * @param h1 height of first rectangle
     * @param x2 x pos of second rectangle
     * @param y2 y pos of second rectangle
     * @param w2 width of second rectangle
     * @param h2 height of second rectangle
     * @return
     */
    public static boolean overlaps2Rectangles(float x1, float y1, float w1, float h1,
                                   float x2, float y2, float w2, float h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

}
