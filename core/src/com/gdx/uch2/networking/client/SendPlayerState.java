package com.gdx.uch2.networking.client;

import com.gdx.uch2.networking.PlayerContext;

import java.util.Timer;
import java.util.TimerTask;

public class SendPlayerState extends TimerTask {

    private PlayerContext ctx;
    private Timer timer;

    public SendPlayerState(Timer timer, PlayerContext ctx){
        this.timer = timer;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        ctx.out.writeMessage(MessageSender.getInstance().getCurrentState());

        if (ctx.out.e != null) {
            timer.cancel();
            timer.purge();
        }
    }
}
