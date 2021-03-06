package com.gdx.uch2.networking.client;

import com.badlogic.gdx.math.Vector2;
import com.gdx.uch2.entities.Block;
import com.gdx.uch2.entities.OnlinePlayerManager;
import com.gdx.uch2.entities.Player;
import com.gdx.uch2.entities.World;
import com.gdx.uch2.networking.GamePhase;
import com.gdx.uch2.networking.PlayerContext;
import com.gdx.uch2.networking.messages.MessageType;
import com.gdx.uch2.networking.messages.ObjectPlacement;
import com.gdx.uch2.networking.messages.PlayerState;

/**
 * Classe Traitant les informations reçues du serveur
 */
public class GameClientHandler {

    /**
     * Phase de jeu actuelle
     */
    static public GamePhase currentPhase;

    static private boolean isOver;
    static private boolean roundOver;
    static private int nRound;
    private final PlayerContext ctx;

    /**
     * Constructeur
     * @param ctx Contexte du joueur
     */
    public GameClientHandler(PlayerContext ctx) {
        this.ctx = ctx;
        reset();
    }

    /**
     * Lit et traite un message en fonction du type du message
     * @param type le type du message
     */
    public void readMessage(MessageType type) {

        switch(type){
            case GameStateUpdate:
                processGameStateUpdate();
                break;
            case BlockPlaced:
                processBlockPlacement();
                break;
            case BlockPosition:
                processBlockPosition();
                break;
            case EndGame:
                isOver = true;
                break;
            case Score:
                processScoreUpdate();
                break;
            default:
                System.out.println("CLI: Message non traitable par le client : " + type);
                break;
        }

    }

    /**
     * Indique si la partie est terminée
     * @return true si la partie est terminée, false sinon
     */
    public static boolean isOver() {
        return isOver;
    }

    /**
     * @return le numéro de round depuis le début de la partie;
     */
    public static int getnRound() { return nRound; }

    /**
     * Réinitialise les champs static
     */
    public static void reset() {
        currentPhase = null;
        isOver = false;
        roundOver = false;
        nRound = 0;
    }

    /**
     * Indique si le round est terminé
     * @return true si le round est terminé, false sinon
     */
    public static boolean isRoundOver() {
        return roundOver;
    }

    /**
     * Traite un message de type GameStateUpdate
     */
    private void processGameStateUpdate(){
        OnlinePlayerManager.getInstance().update(ctx.in.readGameState());
    }

    /**
     * Traite un message de type Score
     */
    private void processScoreUpdate(){
        OnlinePlayerManager.getInstance().setScores(ctx.in.readScore());
    }



    private void processBlockPlacement(){
        ObjectPlacement op = ctx.in.readObjectPlacement();

        if(op.getBlock() == null) {
            roundOver = true;
            startEditingPhase();
        }else{
            roundOver = false;
            Block newBlock = op.getBlock();
            if(newBlock.getType() == Block.Type.ANTIBLOCK){
                World.currentWorld.removeBlock((int)newBlock.getPosition().x, (int)newBlock.getPosition().y);
            }else{
                World.currentWorld.placeBlock(newBlock);
            }

        }

        if(op.getPlayerID() == -1){
            OnlinePlayerManager.getInstance().resetPlacementBlocks();
            startMovementPhase();
        }
    }

    private void processBlockPosition() {
       ObjectPlacement op = ctx.in.readObjectPlacement();
       OnlinePlayerManager.getInstance().setBlockPosition(op.getPlayerID(), op.getBlock());
    }


    private void startMovementPhase(){
        currentPhase = GamePhase.Moving;
        ++nRound;
    }

    private void startEditingPhase(){
        currentPhase = GamePhase.Editing;
        MessageSender.getInstance().setCanPlace(true);
        Vector2 pos = World.currentWorld.getLevel().getSpawnPosition();
        MessageSender.getInstance().setContext(ctx);
        MessageSender.getInstance().setCurrentState(new PlayerState(ctx.getId(),
                Player.State.IDLE, pos.x, pos.y, 0));
    }
}
