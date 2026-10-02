package com.example.magicdefense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class GameView extends View {
    private Paint paint = new Paint();
    private float x = 200;
    private float y = 300;
    private float radius = 40;
    private float speedX = 5;
    private float speedY = 4;
    private Random random = new Random();
    private float touchTolerance;



    public GameView(Context context) {
        super(context);
        paint.setAntiAlias(true);
        // marge pour l'inprécision du doigt
        touchTolerance = 16 * getResources().getDisplayMetrics().density;

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

    private void update() {
        x += speedX;

        if (x - radius < 0) {
            x = radius;
            speedX = -speedX;
        } else if (x + radius > getWidth()) {
            x = getWidth() - radius;
            speedX = -speedX;
        }
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate();
            postDelayed(this, 16);
        }
    };

    public void startGame() {
        removeCallbacks(gameLoop);
        post(gameLoop);
    }

    public void stopGame() {
        removeCallbacks(gameLoop);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            float dx = event.getX() - x;
            float dy = event.getY() - y;
            float distance = (float) Math.sqrt(dx* dx + dy * dy );

            if (distance <= radius + touchTolerance) {
                respawnEnemy();
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void respawnEnemy() {
        x = radius + random.nextFloat() * (getWidth() - 2 * radius );
        y = radius + random.nextFloat() * (getHeight() * 0.4f);
    }
}
