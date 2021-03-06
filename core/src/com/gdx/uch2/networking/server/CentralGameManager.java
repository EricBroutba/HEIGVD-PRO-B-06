package com.gdx.uch2.networking.server;

import com.gdx.uch2.entities.Level;
import com.gdx.uch2.networking.GamePhase;
import com.gdx.uch2.networking.PlayerContext;
import com.gdx.uch2.networking.messages.MessageType;
import com.gdx.uch2.networking.messages.ObjectPlacement;

import java.util.Arrays;

/**
 * Classe métier gérant le déroulement du jeu des clients en lisant et envoyant des messages
 */
public class CentralGameManager {

    private PlayerContext[] players;
    private int[] finished; // 0 = pas arrivé, 1 = arrivé, 2 = premier arrivé.
    private boolean[] dead;
    private boolean[] hasPlaced;
    private GamePhase currentPhase;
    private Level map;
    private int nbPlayersReady = 0;
    private int round;
    private final int nbRounds;
    private boolean isOver;
    private final int PTS_FIRST = 10, PTS_ARRIVED = 5;
    private int[] scoring;
    private boolean firstArrived;

    /**
     * Constructeur
     * @param map niveau dans lequel se joue la partie
     * @param nbRounds nombre de rounds que durera la partie
     */
    public CentralGameManager(Level map, int nbRounds){
        this.map = map;
        this.nbRounds = nbRounds;
        firstArrived = true;

    }

    /**
     * Initialise la partie
     * @param players
     */
    public void init(PlayerContext[] players) {
        this.players = players;
        finished = new int[players.length];
        Arrays.fill(finished, 0);
        dead = new boolean[players.length];
        scoring = new int[players.length];
        Arrays.fill(scoring, 0);
        hasPlaced = new boolean[players.length];
        firstArrived = true;

    }


    /**
     * Lit, interprète et traite les messages reçus
     * @param type type du message
     * @param context Contexte du client duquel provient le message
     */
    public void readMessage(MessageType type, PlayerContext context) {

        switch(type){
            case PlayerStateUpdate:
                processPlayerState(context);
                break;
            case BlockPosition:
                processBlockPosition(context);
                break;
            case BlockPlaced:
                processObjectPlacement(context);
                break;
            case ReachedEnd:
                processPlayerReachedEnd(context);
                break;
            case Death:
                processPlayerDeath(context);
                break;
            case AckGameStart:
                processAckGameStart(context);
                break;
            default:
                System.out.println("SRV: Type de messages inconnu : " + type);
                break;
        }
    }

    /**
     * Informe qu'un client a été déconnecté de la partie. Termine la partie instantanément.
     */
    public void disconnectedClient() {
        if (players != null) {
            endGame();
        }
    }

    /**
     *
     * @return True si la partie est terminée, false sinon
     */
    public boolean isOver() {
        return isOver;
    }

    private void startMovementPhase(){
        currentPhase = GamePhase.Moving;
        Arrays.fill(finished, 0);
        Arrays.fill(dead, false);
        Arrays.fill(hasPlaced, false);
        firstArrived = true;
    }

    private void startEditingPhase(){
        final int STARTER_ID = 0;

        currentPhase = GamePhase.Editing;

        //Send an object with Block = null to inform players that the editing phase is starting
        ObjectPlacement op = new ObjectPlacement(STARTER_ID, null);
        sendBlockToAllPlayers(op);
    }

    private void computePoints(){
        for(int i = 0; i < players.length; ++i){
            if(finished[i] > 0){
                if(finished[i] == 2){ // Premier
                    scoring[i] += PTS_FIRST;
                }else if (finished[i] == 1){
                    scoring[i] += PTS_ARRIVED;
                }
            }
        }

        for (PlayerContext ctx : players) {
            if (!ctx.getSocket().isClosed()) {
                ctx.out.writeMessage(scoring);
            }
        }
    }

    private void resetPlayersPositions(){
        ServerGameStateTickManager.getInstance().getGameState().setPositions(map.getSpawnPosition());
    }

    private void processPlayerDeath(PlayerContext ctx) {
        dead[ctx.getId()] = true;
        checkEndRound();
    }

    private void processPlayerReachedEnd(PlayerContext ctx){
        if(firstArrived){
            finished[ctx.getId()] = 2;
        }else{
            finished[ctx.getId()] = 1;
        }
        firstArrived = false;
        checkEndRound();
    }

    private void checkEndRound() {
        boolean allFinished = true;
        for (int i = 0; i < finished.length; ++i) {
            if (!players[i].getSocket().isClosed() && finished[i] == 0 && !dead[i]) {
                allFinished = false;
                break;
            }
        }


        if(allFinished){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                    resetPlayersPositions();
                    if (++round < nbRounds) {
                        computePoints();
                        startEditingPhase();
                    } else {
                        endGame();
                    }
                }
            }).start();
        }
    }

    private void endGame() {
        computePoints();
        for (PlayerContext p : players) {
            if (!p.getSocket().isClosed()) {
                p.out.writeMessage(MessageType.EndGame);
            }
        }

        isOver = true;
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
        GameServer.closeConnection();
    }

    private void processAckGameStart(PlayerContext ctx){
        nbPlayersReady++;
        if (nbPlayersReady == players.length) {
            startEditingPhase();
        }
    }

    private void processPlayerState(PlayerContext ctx){
        ServerGameStateTickManager.getInstance().setPlayerState(ctx.in.readPlayerState());

    }

    private void processObjectPlacement(PlayerContext ctx){

        if(currentPhase == GamePhase.Editing) {
            //Reads the message
            ObjectPlacement op = ctx.in.readObjectPlacement();

            hasPlaced[op.getPlayerID()] = true;

            boolean acc = true;
            for (int i = 0; i < hasPlaced.length; ++i) {
                acc = acc && hasPlaced[i];
            }

            if (acc) {
                startMovementPhase();
                sendBlockToAllPlayers(new ObjectPlacement(-1, op.getBlock()));
            } else {
                sendBlockToAllPlayers(new ObjectPlacement(0, op.getBlock()));
            }
        }
    }

    private void processBlockPosition(PlayerContext ctx) {
        ObjectPlacement op = ctx.in.readObjectPlacement();

        for (PlayerContext c : players) {
            if (c.getId() != ctx.getId()) {
                c.out.writeMessage(op, false);
            }
        }
    }

    private void sendBlockToAllPlayers(final ObjectPlacement op){
        for (PlayerContext ctx : players) {
            if (!ctx.getSocket().isClosed()) {
                ctx.out.writeMessage(op);
            }
        }
    }
}
