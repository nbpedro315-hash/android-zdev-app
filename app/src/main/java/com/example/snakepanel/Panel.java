package com.example.snakepanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Panel extends SurfaceView implements SurfaceHolder.Callback {

    private final GameThread gameThread;
    private final Paint paint = new Paint();
    private final Random rand = new Random();

    // Game state
    private List<Point> snake = new ArrayList<>();
    private Point food;
    private Direction currentDir = Direction.RIGHT;
    private int cellSize = 40;
    private int cols, rows;
    private boolean gameOver = false;

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        gameThread = new GameThread(getHolder());
        initGame();
    }

    private void initGame() {
        snake.clear();
        snake.add(new Point(5, 5));
        snake.add(new Point(4, 5));
        snake.add(new Point(3, 5));
        spawnFood();
        currentDir = Direction.RIGHT;
        gameOver = false;
    }

    private void spawnFood() {
        int x, y;
        do {
            x = rand.nextInt(cols);
            y = rand.nextInt(rows);
            food = new Point(x, y);
        } while (snake.contains(food));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        gameThread.setRunning(false);
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // retry
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        cols = width / cellSize;
        rows = height / cellSize;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // Simple swipe detection (optional)
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            // Determine direction based on quadrant relative to center
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float dx = x - cx;
            float dy = y - cy;
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0 && currentDir != Direction.LEFT) currentDir = Direction.RIGHT;
                else if (dx < 0 && currentDir != Direction.RIGHT) currentDir = Direction.LEFT;
            } else {
                if (dy > 0 && currentDir != Direction.UP) currentDir = Direction.DOWN;
                else if (dy < 0 && currentDir != Direction.DOWN) currentDir = Direction.UP;
            }
            performClick();
        }
        return true;
    }

    private void update() {
        if (gameOver) return;

        // Move snake
        Point head = snake.get(0);
        Point newHead = new Point(head.x, head.y);
        switch (currentDir) {
            case UP:    newHead.y--; break;
            case DOWN:  newHead.y++; break;
            case LEFT:  newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }

        // Wrap around edges
        if (newHead.x < 0) newHead.x = cols - 1;
        if (newHead.x >= cols) newHead.x = 0;
        if (newHead.y < 0) newHead.y = rows - 1;
        if (newHead.y >= rows) newHead.y = 0;

        // Check self collision
        if (snake.contains(newHead)) {
            gameOver = true;
            return;
        }

        // Add new head
        snake.add(0, newHead);

        // Check food
        if (newHead.equals(food)) {
            spawnFood(); // grow, don't remove tail
        } else {
            // remove tail
            snake.remove(snake.size() - 1);
        }
    }

    private void render(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        paint.setColor(Color.GREEN);
        for (Point p : snake) {
            canvas.drawRect(p.x * cellSize, p.y * cellSize,
                    (p.x + 1) * cellSize, (p.y + 1) * cellSize, paint);
        }
        paint.setColor(Color.RED);
        canvas.drawRect(food.x * cellSize, food.y * cellSize,
                (food.x + 1) * cellSize, (food.y + 1) * cellSize, paint);

        if (gameOver) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(80);
            canvas.drawText("Game Over", getWidth() / 4f, getHeight() / 2f, paint);
        }
    }

    private class GameThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private boolean running = false;
        private final long frameTime = 150; // ms per tick

        GameThread(SurfaceHolder holder) {
            this.surfaceHolder = holder;
        }

        void setRunning(boolean run) {
            running = run;
        }

        @Override
        public void run() {
            while (running) {
                long start = System.currentTimeMillis();
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        update();
                        if (canvas != null) render(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                long diff = System.currentTimeMillis() - start;
                try {
                    Thread.sleep(Math.max(frameTime - diff, 0));
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    private enum Direction {UP, DOWN, LEFT, RIGHT}

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
