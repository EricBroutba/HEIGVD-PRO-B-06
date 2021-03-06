package com.gdx.uch2.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.gdx.uch2.entities.Block;
import com.gdx.uch2.entities.Player;
import com.gdx.uch2.entities.Player.State;
import com.gdx.uch2.entities.World;
import com.gdx.uch2.networking.GamePhase;
import com.gdx.uch2.networking.client.GameClientHandler;
import com.gdx.uch2.networking.client.MessageSender;
import com.gdx.uch2.networking.messages.PlayerState;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe permettant de contrôler les mouvements et actions d'un joueur
 */
public class PlayerController {

    /**
     * Touches sur lesquelles l'utilisateur pour contrôler le joueur
     */
    enum Keys {
        LEFT, RIGHT, JUMP
    }

    private static final long LONG_JUMP_PRESS 	= 220;
    private static final float ACCELERATION 	= 20;
    private static final float GRAVITY 			= -36f;
    private static final float MAX_JUMP_SPEED	= 10f;
    private static final float DAMP 			= 0.8f;
    private static final float MAX_VEL 			= 5.6f;
    private static final float MAX_FALL_VEL 	= -30;
    private static final float MAX_SLIDING_VEL = -14;
    private static final float SLIDING_JUMP_RECOIL = 2f;
    private static final float SLIDING_FRICTION =  20f;
    private static final long SLIDING_JUMP_RECOIL_TIME = 0;

    private World 	world;
    private Player 	player;
    private long	jumpPressedTime;
    private boolean jumpingActive;
    private boolean jumpingPressed;
    private float jumpingPosition;
    private boolean grounded = false;
    private long recoilBeginTime;
    private boolean finished = false;
    private float epsilon = 0.02f;

    private Sound jumpingSound = Gdx.audio.newSound(Gdx.files.internal("sound/jump.mp3"));
    private Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("sound/death.mp3"));
    private Sound finishSound = Gdx.audio.newSound(Gdx.files.internal("sound/finish.mp3"));

    private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };

    private Map<PlayerController.Keys, Boolean> keys = new HashMap<>();

    private Array<Block> collidable = new Array<>();

    /**
     * Constructeur
     * @param world Monde dans lequel évolue le joueur
     */
    public PlayerController(World world) {
        this.world = world;
        this.player = world.getPlayer();
        keys.put(Keys.LEFT, false);
        keys.put(Keys.RIGHT, false);
        keys.put(Keys.JUMP, false);
    }

    // ** Key presses and touches **************** //

    /**
     * Indique que le joueur appuie sur la touche pour aller à gauche
     */
    public void leftPressed() {
        keys.get(keys.put(Keys.LEFT, true));
    }

    /**
     * Indique que le joueur appuie sur la touche pour aller à droite
     */
    public void rightPressed() {
        keys.get(keys.put(Keys.RIGHT, true));
    }

    /**
     * Indique que le joueur appuie sur la touche pour sauter
     */
    public void jumpPressed() {
        jumpingPressed = true;
        keys.get(keys.put(Keys.JUMP, true));
    }

    /**
     * Indique que le joueur relâche la touche pour aller à gauche
     */
    public void leftReleased() {
        keys.get(keys.put(Keys.LEFT, false));
    }

    /**
     * Indique que le joueur relâche la touche pour aller à droite
     */
    public void rightReleased() {
        keys.get(keys.put(Keys.RIGHT, false));
    }

    /**
     * Indique que le joueur relâche la touche pour sauter
     */
    public void jumpReleased() {
        keys.get(keys.put(Keys.JUMP, false));
        jumpingActive = false;
    }

    /**
     * Indique que le joueur appuie sur la touche pour abandonner le round
     */
    public void giveUp() {
        if (!finished)
            player.kill();
    }

    /**
     * Méthode d'update principale, gère tous les déplacements/actions du personnage selon les inputs de l'utilisateur
     * @param delta temps écoulé depuis le dernier appel à cette méthode
     */
    public void update(float delta) {
        State bak = player.getState();
        if (!finished && !player.isDead()) {
            // Processing the input - setting the states of Player
            processInput();
        }

        if (player.getPosition().y < -3) {
            player.kill();
        } else {
            // Fix resume mess after long inactivity
            delta = Math.min(delta, 0.025f);

            // Setting initial vertical acceleration
            player.getAcceleration().y = GRAVITY;

            // Need to update player here to apply effects before other operations
            player.update(delta);

            if (player.getState() == State.SLIDING && player.getVelocity().y < 0) {
                player.getAcceleration().y += SLIDING_FRICTION;
            }

            // Convert acceleration to frame time
            player.getAcceleration().scl(delta);

            // apply acceleration to change velocity
            player.getVelocity().add(player.getAcceleration().x, player.getAcceleration().y);

            // checking collisions with the surrounding blocks depending on Player's velocity
            checkCollisionWithBlocks(delta);

            // If Player is grounded then reset the state to IDLE
            if (grounded && (player.getState().equals(State.JUMPING) || player.getState() == State.SLIDING)) {
                player.setState(State.IDLE);
            }

            // apply damping to halt Player nicely
            if (player.getState() == State.IDLE || finished) {
                player.getVelocity().x *= DAMP;
            }

            // ensure terminal velocity is not exceeded
            if (player.getVelocity().x > MAX_VEL) {
                player.getVelocity().x = MAX_VEL;
            }
            if (player.getVelocity().x < -MAX_VEL) {
                player.getVelocity().x = -MAX_VEL;
            }

            if (player.getState() == State.SLIDING && player.getVelocity().y < MAX_SLIDING_VEL) {
                player.getVelocity().y = MAX_SLIDING_VEL;
            } else if (player.getVelocity().y < MAX_FALL_VEL) {
                player.getVelocity().y = MAX_FALL_VEL;
            }
        }

        if (player.isDead()) {
            finish();
        }

        MessageSender.getInstance().setCurrentState(
                new PlayerState(MessageSender.getInstance().getPlayerID(),
                        player.isDead() ? State.DEAD : player.getState(),
                        player.getPosition().x, player.getPosition().y, System.nanoTime()));
    }

    //Mort ou arrivé
    private void finish() {
        if(!finished){
            if (player.isDead()) {
                deathSound.play(0.6f);
                MessageSender.getInstance().sendDeath();
            } else {
                finishSound.play(0.7f);
                MessageSender.getInstance().sendFinish();
            }
            finished = true;
            player.setState(State.IDLE);
        }
    }

    //vérification des collisions
    private void checkCollisionWithBlocks(float delta) {
        // scale velocity to frame units
        player.getVelocity().scl(delta);

        // Obtain the rectangle from the pool instead of instantiating it
        Rectangle playerRect = rectPool.obtain();
        // set the rectangle to player's bounding box
        playerRect.set(player.getBounds().x, player.getBounds().y, player.getBounds().width, player.getBounds().height);

        // Check victory
        for(Block finishblock : world.getLevel().getFinishBlocks()){
            if (playerRect.overlaps(finishblock.getBounds())) {
                finish();
            }
        }



        boolean fakeVeloctiy = false;
        if (player.getState() == State.SLIDING && player.getVelocity().x == 0) {
            player.getVelocity().x = player.isFacingLeft() ? -0.1f : 0.1f;
            fakeVeloctiy = true;
        }

        // we first check the movement on the horizontal X axis
        int startX, endX;
        int startY = (int) player.getBounds().y;
        int endY = (int) (player.getBounds().y + player.getBounds().height);
        // if Player is heading left then we check if he collides with the block on his left
        // we check the block on his right otherwise
        if (player.getVelocity().x < 0) {
            startX = endX = (int) Math.floor(player.getBounds().x + player.getVelocity().x);
        } else {
            startX = endX = (int) Math.floor(player.getBounds().x + player.getBounds().width + player.getVelocity().x);
        }

        // get the block(s) player can collide with
        populateCollidableBlocks(startX, startY, endX, endY);

        // simulate player's movement on the X

        playerRect.x += player.getVelocity().x;

        // clear collision boxes in world
        world.getCollisionRects().clear();

        boolean collide = false;
        // if player collides, make his horizontal velocity 0
        for (Block block : collidable) {
            if (block == null) continue;
            if (!block.isSolid()) continue;
            if (playerRect.overlaps(block.getBounds())) {
                collide = true;

                if (player.getPosition().x < jumpingPosition - epsilon
                        || player.getPosition().x > jumpingPosition + epsilon) {
                    jumpingActive = false;
                }
                if(!grounded) {
                    player.setState(State.SLIDING);

                    if (player.getVelocity().x < 0) {
                        player.setFacingLeft(true);
                    } else if (player.getVelocity().x > 0){
                        player.setFacingLeft(false);
                    }
                }

                // Apply block action if any
                block.action(player);

                world.getCollisionRects().add(block.getBounds());

                // Fix oscillating state at colliding
                if (player.getVelocity().x < 0) {
                    player.translate(new Vector2(block.getBounds().x + block.getBounds().width - player.getBounds().x, 0));
                } else if (player.getVelocity().x > 0){
                    player.translate(new Vector2(block.getBounds().x - player.getBounds().x - player.getBounds().width, 0));
                }

                player.getVelocity().x = 0;
            }
        }

        if (fakeVeloctiy) {
            player.getVelocity().x = 0;
        }

        // reset the x position of the collision box
        playerRect.x = player.getBounds().x;

        // the same thing but on the vertical Y axis
        startX = (int) player.getBounds().x;
        endX = (int) (player.getBounds().x + player.getBounds().width);
        if (player.getVelocity().y <= 0) {
            startY = endY = (int) Math.floor(player.getBounds().y + player.getVelocity().y);
        } else {
            startY = endY = (int) Math.floor(player.getBounds().y + player.getBounds().height + player.getVelocity().y);
        }

        playerRect.y += player.getVelocity().y;

        populateCollidableBlocks(startX, startY, endX, endY);

        State backup = player.getState();
        if (player.getState() != State.SLIDING || !collide) {
            player.setState(State.JUMPING);
        }
        grounded = false;
        for (Block block : collidable) {
            if (block == null) continue;
            if (!block.isSolid()) continue;
            if (playerRect.overlaps(block.getBounds())) {
                jumpingActive = false;
                if (player.getVelocity().y < 0) {
                    // Fix oscillating state at landing
                    player.translate(new Vector2(0, block.getBounds().y + block.getBounds().height - player.getBounds().y));

                    player.setState(backup);
                    grounded = true;
                }

                player.getVelocity().y = 0;

                // Apply block action if any
                block.action(player);

                world.getCollisionRects().add(block.getBounds());
            }
        }
        // reset the collision box's position on Y
        playerRect.y = player.getBounds().y;


        player.translate(player.getVelocity());

        // un-scale velocity (not in frame time)
        player.getVelocity().scl(1 / delta);

    }


    private void populateCollidableBlocks(int startX, int startY, int endX, int endY) {
        collidable.clear();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x < world.getLevel().getWidth() && y >=0 && y < world.getLevel().getHeight()) {
                    collidable.add(world.getLevel().get(x, y));
                }
            }
        }
    }


    private boolean processInput() {
        if(GameClientHandler.currentPhase == GamePhase.Moving){
            if (keys.get(Keys.JUMP)) {
                if (jumpingPressed && (!player.getState().equals(State.JUMPING))) {
                    jumpingSound.play(0.4f);
                    jumpingActive = true;
                    jumpingPressed = false;
                    jumpPressedTime = System.currentTimeMillis();
                    jumpingPosition = player.getPosition().x;

                    player.getVelocity().y = MAX_JUMP_SPEED;
                    grounded = false;

                    if (player.getState() == State.SLIDING) {
                        jumpingPosition = -1;
                        recoilBeginTime = System.currentTimeMillis();
                        player.getAcceleration().x = 0;

                        if (player.isFacingLeft()) {
                            if (!keys.get(Keys.LEFT)) {
                                player.setFacingLeft(false);
                            }
                            player.getVelocity().x = MAX_JUMP_SPEED;
                        } else {
                            if (!keys.get(Keys.RIGHT)) {
                                player.setFacingLeft(true);
                            }
                            player.getVelocity().x = -MAX_JUMP_SPEED;
                        }
                    }

                    player.setState(State.JUMPING);
                } else if (jumpingActive){
                    if ((System.currentTimeMillis() - jumpPressedTime) >= LONG_JUMP_PRESS) {
                        jumpingActive = false;
                    } else {
                        player.getVelocity().y = MAX_JUMP_SPEED;
                    }
                }
            }

            float tmp;
            boolean flag = player.isFacingLeft();
            if (keys.get(Keys.LEFT)) {
                // left is pressed
                player.setFacingLeft(true);
                if (!player.getState().equals(State.JUMPING)) {
                    player.setState(State.WALKING);
                }
                tmp = -ACCELERATION;
            } else if (keys.get(Keys.RIGHT)) {
                // left is pressed
                player.setFacingLeft(false);
                if (!player.getState().equals(State.JUMPING)) {
                    player.setState(State.WALKING);

                }
                tmp = ACCELERATION;
            } else {
                if (!player.getState().equals(State.JUMPING) && player.getState() != State.SLIDING) {
                    player.setState(State.IDLE);
                }
                tmp = 0;

            }

            if (System.currentTimeMillis() - recoilBeginTime >= SLIDING_JUMP_RECOIL_TIME
                    || flag != player.isFacingLeft()) {
                player.getAcceleration().x = tmp;
            }
        }
        return false;
    }

}
