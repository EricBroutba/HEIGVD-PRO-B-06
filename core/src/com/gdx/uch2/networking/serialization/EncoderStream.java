package com.gdx.uch2.networking.serialization;

import com.gdx.uch2.entities.Block;
import com.gdx.uch2.networking.GameState;
import com.gdx.uch2.networking.MessageType;
import com.gdx.uch2.networking.ObjectPlacement;
import com.gdx.uch2.networking.PlayerState;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

public class EncoderStream {
    private Semaphore mutex;
    private DataOutputStream stream;

    public EncoderStream(OutputStream stream) {
        this.mutex = new Semaphore(1);
        this.stream = new DataOutputStream( new BufferedOutputStream(stream));
    }

    public void writeMessage(MessageType messageType) {
        writeMessage(messageType.ordinal());
    }

    public void writeMessage(int i) {
        try {
            mutex.acquire();
            try {
                stream.writeInt(i);
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writeMessage(PlayerState playerState){
        try {
            mutex.acquire();
            writeMessage(playerState, true);
            stream.flush();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }

    }

    private void writeMessage(PlayerState playerState, boolean writeMessageType){
        try {
            // PlayerStateUpdate.
            if (writeMessageType) stream.writeInt(MessageType.PlayerStateUpdate.ordinal());
            stream.writeInt(playerState.getPlayerID());
            stream.writeFloat(playerState.getPosX());
            stream.writeFloat(playerState.getPosY());
            stream.writeLong(playerState.getTime());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeMessage(GameState gameState){
        try {
            mutex.acquire();
            try {
                // GameStateUpdate.
                stream.writeInt(MessageType.GameStateUpdate.ordinal());
                // Taille.
                stream.writeInt(gameState.getPlayerStates().size());
                for (PlayerState playerState : gameState.getPlayerStates().values()) {
                    writeMessage(playerState, false);
                }
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writeMessage(ObjectPlacement objectPlacement){
        try {
            mutex.acquire();
            try {
                // BlockPlaced.
                stream.writeInt(MessageType.BlockPlaced.ordinal());
                stream.writeInt(objectPlacement.getPlayerID());
                // Bloc.
                Block b = objectPlacement.getBlock();
                if (b == null) {
                    stream.writeInt(-1);
                } else {
                    stream.writeInt(b.getType().ordinal());
                    stream.writeFloat(objectPlacement.getBlock().getPosition().x);
                    stream.writeFloat(objectPlacement.getBlock().getPosition().y);
                }
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush(){
        try {
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}