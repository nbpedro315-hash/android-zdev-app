package com.example.snakepanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

/**
 * Minimal snake game implementation.
 * Adapted from the original snake-real/Panel.java (simplified for Android).
 */
public class Panel extends View {

    private static final int GRID_SIZE = 20;
    private static final int UPDATE_DELAY = 150; // ms

    private final Paint paint = new Paint();
    private final Handler handler = new Handler();
    private final Random random = new Random();

    private ArrayList<Point> snake = new ArrayList<>();
    private Point food;
    private int dirX = 1, dirY = 0; // start moving right
    private boolean gameOver = false;

    public Panel(Context context) {
        super(context);
        init();
    }

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // start snake in the middle
        snake.clear();
        snake.add(new Point(5, 5));
        spawnFood();
        handler.postDelayed(updateRunnable, UPDATE_DELAY);
    }

    private void spawnFood() {
        int max = GRID_SIZE - 1;
        food = new Point(random.nextInt(max), random.nextInt(max));
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!gameOver) {
                step();
                invalidate();
                handler.postDelayed(this, UPDATE_DELAY);
            }
        }
    };

    private void step() {
        // compute new head
        Point head = snake.get(0);
        int newX = head.x + dirX;
        int newY = head.y + dirY;

        // wall collision
        if (newX < 0 || newY < 0 || newX >= GRID_SIZE || newY >= GRID_SIZE) {
            gameOver = true;
            return;
        }

        Point newHead = new Point(newX, newY);

        // self collision
        if (snake.contains(newHead)) {
            gameOver = true;
            return;
        }

        // move snake
        snake.add(0, newHead);
        if (newHead.equals(food)) {
            spawnFood(); // grow
        } else {
            snake.remove(snake.size() - 1); // keep length
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth() / GRID_SIZE;
        int h = getHeight() / GRID_SIZE;

        // background
        canvas.drawColor(Color.BLACK);

        // draw food
        paint.setColor(Color.RED);
        canvas.drawRect(food.x * w, food.y * h,
                (food.x + 1) * w, (food.y + 1) * h, paint);

        // draw snake
        paint.setColor(Color.GREEN);
        for (Point p : snake) {
            canvas.drawRect(p.x * w, p.y * h,
                    (p.x + 1) * w, (p.y + 1) * h, paint);
        }

        // game over overlay
        if (gameOver) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(64);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            // restart on tap
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameOver = false;
                dirX = 1; dirY = 0;
                init();
                invalidate();
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            Point head = snake.get(0);
            int cellW = getWidth() / GRID_SIZE;
            int cellH = getHeight() / GRID_SIZE;
            int touchX = (int) (x / cellW);
            int touchY = (int) (y / cellH);

            int dx = touchX - head.x;
            int dy = touchY - head.y;

            // simple direction change: prioritize larger delta
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0 && dirX != -1) { dirX = 1; dirY = 0; }
                else if (dx < 0 && dirX != 1) { dirX = -1; dirY = 0; }
            } else {
                if (dy > 0 && dirY != -1) { dirX = 0; dirY = 1; }
                else if (dy < 0 && dirY != 1) { dirX = 0; dirY = -1; }
            }
        }
        return true;
    }

    // Simple point class (avoids android.graphics.Point for brevity)
    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }
}
