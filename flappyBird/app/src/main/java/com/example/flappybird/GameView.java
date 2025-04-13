package com.example.flappybird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isPlaying;
    private boolean isGameOver = false;
    private int score = 0;
    
    // Game objects
    private Bird bird;
    private ArrayList<Pipe> pipes = new ArrayList<>();
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Drawing tools
    private Paint paint;
    private SurfaceHolder surfaceHolder;
    
    // Game speed
    private int gameSpeed = 10;
    
    // Game state
    private GameState gameState = GameState.WAITING;
    
    // Game states
    enum GameState {
        WAITING,
        PLAYING,
        GAME_OVER
    }
    
    public GameView(Context context, int screenWidth, int screenHeight) {
        super(context);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Initialize drawing tools
        surfaceHolder = getHolder();
        paint = new Paint();
        
        // Initialize bird
        bird = new Bird(screenWidth / 4, screenHeight / 2, screenWidth / 15);
        
        // Initialize pipes
        initPipes();
    }
    
    private void initPipes() {
        pipes.clear();
        Random random = new Random();
        
        // Create initial pipes offscreen
        int pipeWidth = screenWidth / 6;
        int gap = screenHeight / 4;
        
        for (int i = 0; i < 3; i++) {
            int pipeX = screenWidth + i * (screenWidth / 2 + pipeWidth);
            int gapY = screenHeight / 6 + random.nextInt(screenHeight / 2);
            
            pipes.add(new Pipe(pipeX, gapY, pipeWidth, gap));
        }
    }
    
    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            sleep();
        }
    }
    
    private void update() {
        if (gameState == GameState.PLAYING) {
            // Update bird
            bird.update();
            
            // Check if bird hit the ground or ceiling
            if (bird.getY() <= 0 || bird.getY() >= screenHeight) {
                gameState = GameState.GAME_OVER;
                isGameOver = true;
                return;
            }
            
            // Update and check pipes
            for (Pipe pipe : pipes) {
                pipe.update(gameSpeed);
                
                // Check collision with pipe
                if (pipe.collidesWith(bird)) {
                    gameState = GameState.GAME_OVER;
                    isGameOver = true;
                    return;
                }
                
                // Add score when bird passes pipe
                if (!pipe.isPassed() && pipe.getX() + pipe.getWidth() < bird.getX()) {
                    pipe.setPassed(true);
                    score++;
                }
            }
            
            // Recycle pipes that went off screen
            for (int i = 0; i < pipes.size(); i++) {
                Pipe pipe = pipes.get(i);
                if (pipe.getX() + pipe.getWidth() < 0) {
                    // Move pipe to the end
                    Random random = new Random();
                    int farthestX = 0;
                    
                    for (Pipe p : pipes) {
                        farthestX = Math.max(farthestX, p.getX());
                    }
                    
                    pipe.setX(farthestX + screenWidth / 2);
                    pipe.setGapY(screenHeight / 6 + random.nextInt(screenHeight / 2));
                    pipe.setPassed(false);
                }
            }
        }
    }
    
    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            
            // Draw background
            canvas.drawColor(Color.parseColor("#87CEEB")); // Sky blue
            
            // Draw pipes
            for (Pipe pipe : pipes) {
                pipe.draw(canvas, paint);
            }
            
            // Draw bird
            bird.draw(canvas, paint);
            
            // Draw score
            paint.setTextSize(screenHeight / 15f);
            paint.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(score), screenWidth / 2f, screenHeight / 8f, paint);
            
            // Draw game state messages
            if (gameState == GameState.WAITING) {
                drawCenteredText(canvas, "Tap to Play", screenHeight / 2f);
            } else if (gameState == GameState.GAME_OVER) {
                drawCenteredText(canvas, "Game Over", screenHeight / 2f);
                drawCenteredText(canvas, "Tap to Restart", screenHeight / 2f + screenHeight / 10f);
                drawCenteredText(canvas, "Score: " + score, screenHeight / 2f - screenHeight / 10f);
            }
            
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    
    private void drawCenteredText(Canvas canvas, String text, float y) {
        paint.setTextSize(screenHeight / 15f);
        paint.setColor(Color.WHITE);
        float textWidth = paint.measureText(text);
        canvas.drawText(text, (screenWidth - textWidth) / 2f, y, paint);
    }
    
    private void sleep() {
        try {
            Thread.sleep(17); // ~60 FPS
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }
    
    public void pause() {
        try {
            isPlaying = false;
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameState == GameState.WAITING) {
                gameState = GameState.PLAYING;
            } else if (gameState == GameState.PLAYING) {
                bird.jump();
            } else if (gameState == GameState.GAME_OVER) {
                reset();
                gameState = GameState.WAITING;
            }
            return true;
        }
        return false;
    }
    
    private void reset() {
        isGameOver = false;
        score = 0;
        bird = new Bird(screenWidth / 4, screenHeight / 2, screenWidth / 15);
        initPipes();
    }
    
    // Bird class
    private class Bird {
        private float x, y;
        private float velocity = 0;
        private float gravity = 0.8f;
        private float jumpForce = -15;
        private int size;
        
        public Bird(float x, float y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
        
        public void update() {
            velocity += gravity;
            y += velocity;
        }
        
        public void jump() {
            velocity = jumpForce;
        }
        
        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(x, y, size, paint);
            
            // Draw eye
            paint.setColor(Color.BLACK);
            canvas.drawCircle(x + size / 2, y - size / 3, size / 5, paint);
            
            // Draw beak
            paint.setColor(Color.rgb(255, 165, 0)); // Orange
            canvas.drawRect(x + size / 2, y, x + size * 1.2f, y + size / 3, paint);
        }
        
        public float getX() {
            return x;
        }
        
        public float getY() {
            return y;
        }
        
        public int getSize() {
            return size;
        }
    }
    
    // Pipe class
    private class Pipe {
        private int x;
        private int gapY;
        private int width;
        private int gapHeight;
        private boolean passed = false;
        
        public Pipe(int x, int gapY, int width, int gapHeight) {
            this.x = x;
            this.gapY = gapY;
            this.width = width;
            this.gapHeight = gapHeight;
        }
        
        public void update(int speed) {
            x -= speed;
        }
        
        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.GREEN);
            
            // Draw top pipe
            Rect topPipe = new Rect(x, 0, x + width, gapY);
            canvas.drawRect(topPipe, paint);
            
            // Draw bottom pipe
            Rect bottomPipe = new Rect(x, gapY + gapHeight, x + width, screenHeight);
            canvas.drawRect(bottomPipe, paint);
        }
        
        public boolean collidesWith(Bird bird) {
            // Check if bird collides with top pipe
            if (bird.getX() + bird.getSize() > x && bird.getX() - bird.getSize() < x + width) {
                if (bird.getY() - bird.getSize() < gapY || bird.getY() + bird.getSize() > gapY + gapHeight) {
                    return true;
                }
            }
            return false;
        }
        
        public int getX() {
            return x;
        }
        
        public void setX(int x) {
            this.x = x;
        }
        
        public int getWidth() {
            return width;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public void setGapY(int gapY) {
            this.gapY = gapY;
        }
    }
} 