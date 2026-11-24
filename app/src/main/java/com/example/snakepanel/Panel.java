package com.example.snakepanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple Snake implementation using a SurfaceView.
 */
public class Panel extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread thread;
    private boolean running = false;
    private final SurfaceHolder holder;
    private final Paint paint = new Paint();

    // Game board
    private static final int NUM_COLUMNS = 20;
    private static final int NUM_ROWS = 30;
    private static final int CELL_SIZE = 40; // px (will be scaled by canvas)

    // Snake state
    private final List<Point> snake = new ArrayList<>();
    private Direction currentDir = Direction.RIGHT;

    // Food
    private Point food;

    // Touch handling
    private float startX, startY;

    private final Random random = new Random();

    public Panel(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        initGame();
    }

    private void initGame() {
        snake.clear();
        // start with 3 segments in the middle of the board
        int startX = NUM_COLUMNS / 2;
        int startY = NUM_ROWS / 2;
        snake.add(new Point(startX, startY));
        snake.add(new Point(startX - 1, startY));
        snake.add(new Point(startX - 2, startY));
        placeFood();
    }

    private void placeFood() {
        int x, y;
        do {
            x = random.nextInt(NUM_COLUMNS);
            y = random.nextInt(NUM_ROWS);
        } while (containsSnake(x, y));
        food = new Point(x, y);
    }

    private boolean containsSnake(int x, int y) {
        for (Point p : snake) {
            if (p.x == x && p.y == y) return true;
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void run() {
        long prevTime = System.currentTimeMillis();
        while (running) {
            long now = System.currentTimeMillis();
            if (now - prevTime >= 150) { // game tick every 150 ms
                update();
                draw();
                prevTime = now;
            }
        }
    }

    private void update() {
        // compute new head position
        Point head = snake.get(0);
        int newX = head.x;
        int newY = head.y;

        switch (currentDir) {
            case UP:    newY--; break;
            case DOWN:  newY++; break;
            case LEFT:  newX--; break;
            case RIGHT: newX++; break;
        }

        // wrap around edges
        if (newX < 0) newX = NUM_COLUMNS - 1;
        if (newX >= NUM_COLUMNS) newX = 0;
        if (newY < 0) newY = NUM_ROWS - 1;
        if (newY >= NUM_ROWS) newY = 0;

        // check collision with self
        if (containsSnake(newX, newY)) {
            // game over – restart
            initGame();
            return;
        }

        // add new head
        snake.add(0, new Point(newX, newY));

        // check food
        if (food.x == newX && food.y == newY) {
            placeFood(); // grow (do not remove tail)
        } else {
            // remove tail
            snake.remove(snake.size() - 1);
        }
    }

    private void draw() {
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        // clear background
        canvas.drawColor(Color.BLACK);

        // draw food
        paint.setColor(Color.RED);
        drawCell(canvas, food.x, food.y);

        // draw snake
        paint.setColor(Color.GREEN);
        for (Point p : snake) {
            drawCell(canvas, p.x, p.y);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    private void drawCell(Canvas canvas, int col, int row) {
        int left = col * CELL_SIZE;
        int top = row * CELL_SIZE;
        Rect rect = new Rect(left, top, left + CELL_SIZE, top + CELL_SIZE);
        canvas.drawRect(rect, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Simple swipe detection to change direction
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0 && currentDir != Direction.LEFT) currentDir = Direction.RIGHT;
                    else if (dx < 0 && currentDir != Direction.RIGHT) currentDir = Direction.LEFT;
                } else {
                    if (dy > 0 && currentDir != Direction.UP) currentDir = Direction.DOWN;
                    else if (dy < 0 && currentDir != Direction.DOWN) currentDir = Direction.UP;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    // Simple point holder
    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    private enum Direction { UP, DOWN, LEFT, RIGHT }
}
