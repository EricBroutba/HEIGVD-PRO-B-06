package com.gdx.uch2.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Select;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.uch2.ScreenManager;
import com.gdx.uch2.entities.Player;
import com.gdx.uch2.networking.client.GameClient;
import com.gdx.uch2.networking.server.GameServer;

import javax.sound.sampled.Port;

public class CreateRoomMenu implements Screen {
    private Stage stage;

    public CreateRoomMenu(){
        // create stage and set it as input processor
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {
        // Create a table that fills the screen. Everything else will go inside this table.
        Table table = new Table();
        table.setFillParent(true);

        stage.addActor(table);

        // temporary until we have asset manager in
        Skin skin = new Skin(Gdx.files.internal("neon/skin/neon-ui.json"));

        // Create Image
        Image chickenImg = new Image(new Texture(Gdx.files.internal(("chicken.png"))));
        chickenImg.setWidth(129);
        chickenImg.setHeight(200);

        // Create Text
        Label titleLabel = new Label("Ultimate Chicken Horse 2", skin);
        titleLabel.setFontScale(2);
        final Label nicknameLabel = new Label("Nickname:", skin);
        nicknameLabel.setWidth(100);
        Label portLabel = new Label("Port:", skin);
        portLabel.setWidth(100);
        Label levelLabel = new Label("Level:", skin);
        levelLabel.setWidth(100);
        Label playersLabel = new Label("Players:", skin);
        playersLabel.setWidth(100);
        Label nbRoundsLabel = new Label("Number of rounds:", skin);
        nbRoundsLabel.setWidth(100);
        final Label errorLabel = new Label("", skin);
        errorLabel.setWidth(100);

        // Create TextField
        TextField.TextFieldFilter.DigitsOnlyFilter digitsFilter = new TextField.TextFieldFilter.DigitsOnlyFilter();
        final TextField nicknameTF = new TextField("Player 1", skin);
        final TextField portTF = new TextField("12345", skin);
        final TextField nbRoundsTF = new TextField("10", skin);
        nicknameTF.setMaxLength(20);
        portTF.setTextFieldFilter(digitsFilter);
        portTF.setMaxLength(5);
        nbRoundsTF.setMaxLength(2);
        nbRoundsTF.setTextFieldFilter(digitsFilter);
        final SelectBox<Integer> levelSB = new SelectBox<Integer>(skin);
        levelSB.setItems(1,2,3);
//        levelSB.setSelected(3);
        final SelectBox<Integer> playersSB = new SelectBox<>(skin);
        playersSB.setItems(2,3,4,5,6);

        // Title
        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(10);
        titleGroup.addActor(titleLabel);
        titleGroup.addActor(chickenImg);
        table.add(titleGroup).colspan(2).center();
        table.row();

        // Nickname
        HorizontalGroup nicknameGroup = new HorizontalGroup();
        nicknameGroup.space(10);
        nicknameGroup.addActor(nicknameLabel);
        nicknameGroup.addActor(nicknameTF);
        table.add(nicknameGroup).colspan(2).center();
        table.row();

        // Port
        HorizontalGroup portGroup = new HorizontalGroup();
        portGroup.space(10);
        portGroup.addActor(portLabel);
        portGroup.addActor(portTF);
        table.add(portGroup).colspan(2).center();
        table.row();

        // Nb rounds
        HorizontalGroup roundsGroup = new HorizontalGroup();
        roundsGroup.space(10);
        roundsGroup.addActor(nbRoundsLabel);
        roundsGroup.addActor(nbRoundsTF);
        table.add(roundsGroup).colspan(2).center();
        table.row();

        // Level & players
        HorizontalGroup levelGroup = new HorizontalGroup();
        levelGroup.space(10);
        levelGroup.addActor(levelLabel);
        levelGroup.addActor(levelSB);
        levelGroup.addActor(playersLabel);
        levelGroup.addActor(playersSB);
        table.add(levelGroup).colspan(2).center();
        table.row();

        //create buttons
        TextButton createButton = new TextButton("Create", skin);
        TextButton mainMenuButton = new TextButton("Go back", skin);

        //add buttons to table
        table.add(createButton).width(200).colspan(2);
        table.row();
        table.add(mainMenuButton).width(200).colspan(2);
        table.row();

        // Error label
        table.add(errorLabel).colspan(2).center();


        // create button listeners
        createButton.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                boolean somethingWentWrong = false;

                int port = 0, nbOfRounds = 0;
                String nickname = null;
                try {
                    port = Integer.parseInt(String.valueOf(portTF.getText()));
                    nbOfRounds = Integer.parseInt(nbRoundsTF.getText());
                    nickname = nicknameTF.getText();

                    if (port < 1025 || port > 65535 || nbOfRounds == 0 || nickname.length() == 0) {
                        somethingWentWrong = true;
                    }
                } catch (NumberFormatException e) {
                    somethingWentWrong = true;
                }

                if (somethingWentWrong) {
                    errorLabel.setText("One or more invalid arguments");
                    return;
                }

                int level = levelSB.getSelected();

                System.out.println("création de la partie");

                Thread tServer = new Thread(new GameServer(port, level));
                tServer.start();
                Thread tClient = new Thread(new GameClient("localhost", port, nickname));
                tClient.start();
                Screen s = new WaitingRoomMenu(nickname);
                ScreenManager.getInstance().setPlacementScreen(s);
                ScreenManager.getInstance().showScreen(s);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        mainMenuButton.addListener(new InputListener(){
            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                Screen s = new MainMenu();
                ScreenManager.getInstance().setPlacementScreen(s);
                ScreenManager.getInstance().showScreen(s);
            }
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });

    }

    @Override
    public void render(float delta) {
        // clear the screen ready for next set of images to be drawn
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // tell our stage to do actions and draw itself
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}

