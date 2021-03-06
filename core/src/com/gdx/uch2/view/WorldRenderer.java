package com.gdx.uch2.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.gdx.uch2.entities.*;
import com.gdx.uch2.entities.Player.State;
import com.gdx.uch2.util.Constants;

/**
 * Gère l'affichage des différents éléments présents dans un World
 */
public class WorldRenderer {

    private final float camera_width;
    private final float camera_height;

    private World world;
    private OrthographicCamera cam;

    /** for debug rendering **/
    ShapeRenderer debugRenderer = new ShapeRenderer();

    /** Textures **/
    private TextureRegion boxTexture;
    private TextureRegion blockTexture;
    private TextureRegion gUpTexture;
    private TextureRegion gDownTexture;
    private TextureRegion lethalBlockTexture;
    private TextureRegion spawnTexture;
    private TextureRegion finishTexture;
    private TextureRegion protectedArea;
    private TextureRegion bomb;

    private TextureRegion playerFrame;
    private TextureRegion playerJumpLeft;
    private TextureRegion playerFallLeft;
    private TextureRegion playerJumpRight;
    private TextureRegion playerFallRight;
    private TextureRegion playerDeadRight;
    private TextureRegion playerDeadLeft;

    private TextureRegion opponentJumpLeft;
    private TextureRegion opponentFallLeft;
    private TextureRegion opponentJumpRight;
    private TextureRegion opponentFallRight;
    private TextureRegion opponentDeadRight;
    private TextureRegion opponentDeadLeft;

    /** Animations **/
    private Animation<TextureAtlas.AtlasRegion>  walkLeftAnimation;
    private Animation<TextureAtlas.AtlasRegion>  walkRightAnimation;
    private Animation<TextureAtlas.AtlasRegion>  idleRightAnimation;
    private Animation<TextureAtlas.AtlasRegion>  idleLeftAnimation;

    private Animation<TextureAtlas.AtlasRegion>  opponentWalkLeftAnimation;
    private Animation<TextureAtlas.AtlasRegion>  opponentWalkRightAnimation;
    private Animation<TextureAtlas.AtlasRegion>  opponentIdleRightAnimation;
    private Animation<TextureAtlas.AtlasRegion>  opponentIdleLeftAnimation;

    private Batch spriteBatch;
    private boolean debug = false;
    private int width;
    private int height;
    private float ppuX;	// pixels per unit on the X axis
    private float ppuY;	// pixels per unit on the Y axis

    /**
     * Donne une taille au renderer
     * @param w largeur
     * @param h hauteur
     */
    public void setSize (int w, int h) {
        this.width = w;
        this.height = h;
        ppuX = width / camera_width;
        ppuY = height / camera_height;
    }

    /**
     * @return True si on est en mode debug, false sinon
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Indique si on veut passer en mode debug
     * @param debug True, si on veut passer en debug, false sinon
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Constructeur
     * @param world le monde à afficher
     * @param batch le Batch à utiliser
     * @param debug Si on veut lancer en mode debug ou pas
     */
    public WorldRenderer(World world, Batch batch, boolean debug) {
        this.world = world;
        camera_width = world.getLevel().getWidth();
        camera_height = world.getLevel().getHeight();
        this.cam = new OrthographicCamera(camera_width, camera_height);
//        this.cam.position.set(world.getPlayer().getPosition().x, world.getPlayer().getPosition().y, 0);
		this.cam.position.set(camera_width / 2f, camera_height / 2f, 0);
        this.cam.update();

        this.debug = debug;
        spriteBatch = batch;
        loadTextures();
    }

    private void loadTextures() {
        TextureAtlas playerAtlas = new TextureAtlas(Gdx.files.internal(Constants.PLAYER_1_ATLAS));
        TextureAtlas blocksAtlas = new TextureAtlas(Gdx.files.internal(Constants.BLOCKS_ATLAS));
        TextureAtlas opponentsAtlas = new TextureAtlas(Gdx.files.internal(Constants.OPPONENTS_ATLAS));

        boxTexture = blocksAtlas.findRegion("box");
        blockTexture = blocksAtlas.findRegion("stone");
        gUpTexture = blocksAtlas.findRegion("liquidWater");
        gDownTexture = blocksAtlas.findRegion("liquidLava");
        lethalBlockTexture = blocksAtlas.findRegion("boxExplosive");
        spawnTexture = blocksAtlas.findRegion("signRight");
        finishTexture = blocksAtlas.findRegion("signExit");
        protectedArea = blocksAtlas.findRegion("castleCenter");
        bomb = blocksAtlas.findRegion("antiblock");

        Array<TextureAtlas.AtlasRegion> idleRightFrames = new Array<TextureAtlas.AtlasRegion>();
        Array<TextureAtlas.AtlasRegion> idleLeftFrames = new Array<TextureAtlas.AtlasRegion>();
        for(int i = 1; i <= 4; i++){
            idleRightFrames.add(playerAtlas.findRegion("idle" + i));
            TextureAtlas.AtlasRegion tmp = new TextureAtlas.AtlasRegion(idleRightFrames.get(i-1));
            tmp.flip(true, false);
            idleLeftFrames.add(tmp);
        }
        idleRightAnimation = new Animation<>(Constants.LOOP_SPEED, idleRightFrames, Animation.PlayMode.LOOP);
        idleLeftAnimation = new Animation<>(Constants.LOOP_SPEED, idleLeftFrames, Animation.PlayMode.LOOP);

        Array<TextureAtlas.AtlasRegion> walkingRightFrames = new Array<TextureAtlas.AtlasRegion>();
        Array<TextureAtlas.AtlasRegion> walkingLeftFrames = new Array<TextureAtlas.AtlasRegion>();
        for(int i = 1; i <= 7; i++){
            walkingRightFrames.add(playerAtlas.findRegion("walk-right" + i));
            TextureAtlas.AtlasRegion tmp = new TextureAtlas.AtlasRegion(walkingRightFrames.get(i-1));
            tmp.flip(true, false);
            walkingLeftFrames.add(tmp);
        }
        walkRightAnimation = new Animation<>(Constants.LOOP_SPEED, walkingRightFrames, Animation.PlayMode.LOOP);
        walkLeftAnimation = new Animation<>(Constants.LOOP_SPEED, walkingLeftFrames, Animation.PlayMode.LOOP);

        playerJumpRight = playerAtlas.findRegion("jump1");
        playerJumpLeft = new TextureRegion(playerJumpRight);
        playerJumpLeft.flip(true, false);
        playerFallRight = playerAtlas.findRegion("jump2");
        playerFallLeft = new TextureRegion(playerFallRight);
        playerFallLeft.flip(true, false);
        playerDeadRight = playerAtlas.findRegion("dead");
        playerDeadLeft = new TextureRegion((playerDeadRight));
        playerDeadLeft.flip(true, false);


        // For opponents
        Array<TextureAtlas.AtlasRegion> opponentIdleRightFrames = new Array<TextureAtlas.AtlasRegion>();
        Array<TextureAtlas.AtlasRegion> opponentIdleLeftFrames = new Array<TextureAtlas.AtlasRegion>();
        for(int i = 1; i <= 4; i++){
            opponentIdleRightFrames.add(opponentsAtlas.findRegion("idle" + i));
            TextureAtlas.AtlasRegion tmp = new TextureAtlas.AtlasRegion(opponentIdleRightFrames.get(i-1));
            tmp.flip(true, false);
            opponentIdleLeftFrames.add(tmp);
        }

        opponentIdleRightAnimation = new Animation<>(Constants.LOOP_SPEED, opponentIdleRightFrames, Animation.PlayMode.LOOP);
        opponentIdleLeftAnimation = new Animation<>(Constants.LOOP_SPEED, opponentIdleLeftFrames, Animation.PlayMode.LOOP);

        Array<TextureAtlas.AtlasRegion> opponentWalkingRightFrames = new Array<TextureAtlas.AtlasRegion>();
        Array<TextureAtlas.AtlasRegion> opponentWalkingLeftFrames = new Array<TextureAtlas.AtlasRegion>();
        for(int i = 1; i <= 7; i++){
            opponentWalkingRightFrames.add(opponentsAtlas.findRegion("walk-right" + i));
            TextureAtlas.AtlasRegion tmp = new TextureAtlas.AtlasRegion(opponentWalkingRightFrames.get(i-1));
            tmp.flip(true, false);
            opponentWalkingLeftFrames.add(tmp);
        }
        opponentWalkRightAnimation = new Animation<>(Constants.LOOP_SPEED, opponentWalkingRightFrames, Animation.PlayMode.LOOP);
        opponentWalkLeftAnimation = new Animation<>(Constants.LOOP_SPEED, opponentWalkingLeftFrames, Animation.PlayMode.LOOP);

        opponentJumpRight = opponentsAtlas.findRegion("jump1");
        opponentJumpLeft = new TextureRegion(opponentJumpRight);
        opponentJumpLeft.flip(true, false);
        opponentFallRight = opponentsAtlas.findRegion("jump2");
        opponentFallLeft = new TextureRegion(opponentFallRight);
        opponentFallLeft.flip(true, false);
        opponentDeadRight = opponentsAtlas.findRegion("dead");
        opponentDeadLeft = new TextureRegion((opponentDeadRight));
        opponentDeadLeft.flip(true, false);
    }


    /**
     * Render les objets
     */
    public void renderPlayers() {
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        drawOnlinePlayers();
        drawPlayer();
        spriteBatch.end();
        if (debug) {
            drawDebug();
            drawCollisionBlocks();
        }
    }

    /**
     * Render l'image de fond
     */
    public void renderBackground() {
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        drawBlocks();
        spriteBatch.end();
    }

    /**
     * Render un block
     * @param block le block à render
     * @param pos la position du block
     */
    public void renderBlock(Block.Type block, Vector2 pos) {
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        drawBlock(block, pos);
        spriteBatch.end();
    }

    private void drawBlock(Block.Type block, Vector2 pos) {
        TextureRegion texture;
        switch (block){
            case BOX: texture = boxTexture; break;
            case BLOCK: texture = blockTexture; break;
            case LETHAL: texture = lethalBlockTexture; break;
            case G_UP: texture = gUpTexture; break;
            case G_DOWN: texture = gDownTexture; break;
            case PROTECTED_AREA: texture = protectedArea; break;
            case ANTIBLOCK: texture = bomb; break;
            default: texture = boxTexture; break;
        }
        spriteBatch.draw(texture, pos.x, pos.y, Block.SIZE, Block.SIZE);
    }

    // Scaling pixel -> level unit

    /**
     * Echelonne un vecteur
     * @param v le vecteur à échelonner
     */
    public void scale(Vector2 v) {
        v.x = v.x / ppuX;
        v.y = v.y / ppuY;
    }

    public void unscale(Vector2 v) {
        v.x = v.x * ppuX;
        v.y = v.y * ppuY;
    }


    private void drawBlocks() {

        for(Block[] blockArray : World.currentWorld.getLevel().getBlocks()){
            for(Block block : blockArray){
                if(block != null)
                    drawBlock(block.getType(), block.getPosition());
            }
        }

        spriteBatch.draw(spawnTexture, world.getLevel().getSpawnPosition().x, world.getLevel().getSpawnPosition().y, Block.SIZE, Block.SIZE);

        for(Vector2 finishPos : world.getLevel().getFinishPositions()){
            spriteBatch.draw(finishTexture, finishPos.x, finishPos.y, Block.SIZE, Block.SIZE);
        }


    }

    private void drawOnlinePlayers() {
        for (OnlinePlayer player : OnlinePlayerManager.getInstance().getPlayers()) {
            TextureRegion playerFrame = (TextureRegion) (player.isFacingLeft() ? opponentIdleLeftAnimation.getKeyFrame(player.getLocalTime(), true) : opponentIdleRightAnimation.getKeyFrame(player.getLocalTime(), true));
            if (player.isDead()) {
                playerFrame = player.isFacingLeft() ? opponentDeadLeft : opponentDeadRight;
            } else if (player.getState().equals(State.WALKING)) {
                playerFrame = (TextureRegion) (player.isFacingLeft() ? opponentWalkLeftAnimation.getKeyFrame(player.getLocalTime(), true) : opponentWalkRightAnimation.getKeyFrame(player.getLocalTime(), true));
            } else if (player.getState().equals(State.JUMPING) || player.getState().equals(State.SLIDING)) {
                if (!player.isFalling()) {
                    playerFrame = player.isFacingLeft() ? opponentJumpLeft : opponentJumpRight;
                } else {
                    playerFrame = player.isFacingLeft() ? opponentFallLeft : opponentFallRight;
                }
            }

            spriteBatch.draw(playerFrame, player.getPosition().x, player.getPosition().y, Player.SIZE, Player.SIZE);
        }
    }

    private void drawPlayer() {
        Player player = world.getPlayer();
        playerFrame = (TextureRegion) (player.isFacingLeft() ? idleLeftAnimation.getKeyFrame(player.getStateTime(), true) : idleRightAnimation.getKeyFrame(player.getStateTime(), true));
        if (player.isDead()) {
            playerFrame = player.isFacingLeft() ? playerDeadLeft : playerDeadRight;
        } else if(player.getState().equals(State.WALKING)) {
            playerFrame = (TextureRegion) (player.isFacingLeft() ? walkLeftAnimation.getKeyFrame(player.getStateTime(), true) : walkRightAnimation.getKeyFrame(player.getStateTime(), true));
        } else if (player.getState().equals(State.JUMPING) || player.getState().equals(State.SLIDING)) {
            if (player.getVelocity().y > 0) {
                playerFrame = player.isFacingLeft() ? playerJumpLeft : playerJumpRight;
            } else {
                playerFrame = player.isFacingLeft() ? playerFallLeft : playerFallRight;
            }
        }

        spriteBatch.draw(playerFrame, player.getPosition().x, player.getPosition().y, Player.SIZE, Player.SIZE);
    }

    private void drawDebug() {
        // render blocks
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        for (Block block : world.getDrawableBlocks((int) camera_width, (int) camera_height)) {
            Rectangle rect = block.getBounds();
            debugRenderer.setColor(new Color(1, 0, 0, 1));
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }
        // render Player
        Player player = world.getPlayer();
        Rectangle rect = player.getBounds();
        debugRenderer.setColor(new Color(0, 1, 0, 1));
        debugRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        debugRenderer.end();
    }

    private void drawCollisionBlocks() {
        debugRenderer.setProjectionMatrix(cam.combined);
        debugRenderer.begin(ShapeType.Line);
        debugRenderer.setColor(Color.WHITE);
        for (Rectangle rect : world.getCollisionRects()) {
            debugRenderer.rect(rect.x, rect.y, rect.width, rect.height, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        }
        debugRenderer.end();

    }
}