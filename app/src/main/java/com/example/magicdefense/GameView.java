package com.example.magicdefense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class GameView extends View {
    private Paint paint = new Paint();
    private float x = 200;
    private float y = 300;
    private float radius = 40;



    public GameView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1er objet : cercle
        paint.setColor(Color.rgb(200,80,255));
        canvas.drawCircle(x,y,radius, paint);

        // 2eme objet : ligne de défense
        paint.setColor(Color.rgb(80,200,255));
        float defenseY = getHeight() * 0.85f;
        canvas.drawRect(0, defenseY, getWidth(), defenseY + 20, paint);
    }
}
