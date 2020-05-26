package com.gdx.uch2.networking.server;

import com.gdx.uch2.controller.LevelLoader;
import com.gdx.uch2.entities.Level;
import com.gdx.uch2.networking.messages.MessageType;
import com.gdx.uch2.networking.messages.PlayerContext;
import com.gdx.uch2.networking.client.ErrorHandler;
import com.gdx.uch2.networking.client.GameClient;
import com.gdx.uch2.util.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class GameServer implements Runnable {
    //2 premiers joueurs à se connecter.
    static PlayerContext[] players;
    private String[] nicknames;

    //indique si la partie est pleine
    private boolean full = false;
    private boolean gameStarted = false;

    private int port;
    private Level level;
    private int numlevel;
    private int nbPlayers;
    private int nbRounds;
    private CentralGameManager manager;
    private static ServerSocket serverSocket;

    public GameServer(int port, int noLevel, int nbPlayers, int nbRounds){
        this.port = port;
        this.numlevel = noLevel;
        this.level = LevelLoader.loadLevel(noLevel);
        this.nbPlayers = nbPlayers;
        players = new PlayerContext[nbPlayers];
        nicknames = new String[nbPlayers];
        this.nbRounds = nbRounds;
    }

    @Override
    public void run() {
        System.out.println("Start server");

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            ErrorHandler.getInstance().setError(ex.toString());
            return;
        }

        int id = -1;
        manager = new CentralGameManager(level, nbRounds);
        while (!full) {
            try {
                Socket clientSocket = serverSocket.accept();

                for (int i = 0; i < players.length; ++i) {
//                    if (players[i] != null) {
//                        players[i].out.writeMessage(MessageType.Ping);
//                        System.out.println(i + " : " + players[i].in.getType());
//                        if (players[i].in.e != null) {
//                            if (id < 0) {
//                                id = i;
//                            } else {
//                                players[i] = null;
//                            }
//                        }
//                    } else if (id < 0) {
//                        id = i;
//                    }

                    if (players[i] == null) {
                        id = i;
                        break;
                    }
                }

                PlayerContext ctx = new PlayerContext(id, clientSocket);
                players[id] = ctx;
                nicknames[id] = ctx.in.readString();

                Thread t = new Thread(new PlayerHandler(manager, ctx));
                t.start();

                if(id == nbPlayers - 1){
                    full = true;
                    serverSocket.close();
                    startGame();
                }

                id = -1;
            } catch (IOException ex) {
                break;
            }
        }

    }

    private void startGame(){
        if(players.length > nbPlayers) throw new RuntimeException("Nombre de joueurs trop élevé");

        gameStarted = true;
        System.out.println(players.length + " joueurs connectés. Lancer la partie.");

        manager.init(Arrays.copyOf(players, players.length));

        //Notifie les joueurs et ajoute un MovementHandler aux connexions avec les joueurs
        for(PlayerContext ctx : players){

            ctx.out.writeMessage(MessageType.GameStart);
            ctx.out.writeMessage(ctx.getId());
            ctx.out.writeMessage(numlevel);
            ctx.out.writeMessage(players.length - 1);

            for (PlayerContext oppCtx : players) {
                if (oppCtx.getId() != ctx.getId()) {
                    ctx.out.writeMessage(oppCtx.getId());
                    ctx.out.writeMessage(nicknames[oppCtx.getId()]);
                }
            }

            System.out.println("Message envoyé au joueur #" + ctx.getId());
        }

        //Démarre les ticks de serveur
        ServerGameStateTickManager.getInstance().setPlayers(Arrays.copyOf(players, players.length));
        ServerGameStateTickManager.getInstance().start(1000, Constants.TICK_DURATION, level.getSpawnPosition());
    }

    public static void closeConnection() {
        GameClient.closeConnection();
        try {
            System.out.println("SRV: Fermeture du serveur");
            if (serverSocket != null) {
                serverSocket.close();

                for (PlayerContext ctx : players) {
                    if (ctx != null) {
                        if (ctx.in != null) {
                            ctx.in.close();
                        }

                        if (ctx.out != null) {
                            ctx.out.close();
                        }

                        if (ctx.getSocket() != null) {
                            ctx.getSocket().close();
                        }
                    }
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 12345;
        int level = 1;

        new GameServer(port, level,2 ,10).run();
    }

}
