package com.mygdx.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.mygdx.game.Entity;
import com.mygdx.game.WalkingGame;
import com.mygdx.game.control.Button;
import com.mygdx.game.control.RectButton;
import com.mygdx.game.control.SkillButton;
import com.mygdx.game.skill.Skill;

public class BattleScreen extends ScreenAdapter implements InputProcessor {
    final int ONGOING = 0;
    final int VICTORY = 1;
    final int DEFEAT = 2;
    private Texture bg;
    int gameState;

    final WalkingGame game;
    OrthographicCamera camera;
    private final char[] pattern;
    private char[] sequence;
    private int pointer;
    Array<Button> buttons;
    Vector3 touch;
    Music battleMusic;

    Entity opponent;
    Entity self;
    Texture smallUp;
    Texture smallDown;
    Texture smallLeft;
    Texture smallRight;
    float firstPos;
    float speed;
    Texture healthBar;
    Texture healthBg;
    Texture battleBg;

    float selfStateTime;
    float opponentStateTime;
    Animation[] selfAnimation;
    Animation[] opponentAnimation;
    private Timer AI;

    public BattleScreen(final WalkingGame game,Entity self,Entity opponent) {
        gameState = ONGOING;
        this.game = game;
        this.self = self;
        this.opponent = opponent;
        pattern = new char[] {'h','j','k','l'};
        sequence = new char[9];
        touch = new Vector3();
        for (int i=0;i<sequence.length;i++)
            sequence[i] = pattern[MathUtils.random(3)];
        buttons = new Array<>();
        buttons.add(new RectButton(0,0,"Left.png","Left.png","Left.png","h"));
        buttons.add(new RectButton(120,0,"Down.png","Down.png","Down.png","j"));
        buttons.add(new RectButton(240,0,"Up.png","Up.png","Up.png","k"));
        buttons.add(new RectButton(360,0,"Right.png","Right.png","Right.png","l"));
        for (Skill skill:self.getSkills()){
            skill.setTarget(opponent);
        }
        buttons.add(new SkillButton(40,140,"skill1.png","skill1.png","skill1_u.png",self.getSkills().get(0).getName(),self.getSkills().get(0)));
        buttons.add(new SkillButton(200,140,"skill2.png","skill2.png","skill2_u.png",self.getSkills().get(1).getName(),self.getSkills().get(1)));
        buttons.add(new SkillButton(360,140,"skill3.png","skill3.png","skill3_u.png",self.getSkills().get(2).getName(),self.getSkills().get(2)));

        battleMusic = Gdx.audio.newMusic(Gdx.files.internal("Ectoplasm.mp3"));
        battleMusic.setLooping(true);
        battleMusic.play();


        camera = new OrthographicCamera();
        camera.setToOrtho(false,480,800);
        Gdx.input.setInputProcessor(this);

        smallDown = new Texture(Gdx.files.internal("smallDown.png"));
        smallLeft = new Texture(Gdx.files.internal("smallLeft.png"));
        smallRight = new Texture(Gdx.files.internal("smallRight.png"));
        smallUp = new Texture(Gdx.files.internal("smallUp.png"));
        pointer = 0;
        firstPos = camera.viewportWidth;
        speed = 100;

        healthBar = new Texture(Gdx.files.internal("newhealthbar480x30.png"));
        healthBg = new Texture("newhealthbartransparent480x30.png");
        battleBg = new Texture("newbattlebg480x800.jpg");
        bg = new Texture("background.jpg");

        selfStateTime = 0;
        opponentStateTime = 0;
        selfAnimation = new Animation[3];
        selfAnimation[self.WALK_STATE] = new Animation(0.5f,self.getWalkFrames());
        selfAnimation[self.WALK_STATE].setPlayMode(Animation.PlayMode.LOOP);
        selfAnimation[self.ATTACK_STATE] = new Animation(0.5f,self.getAttackFrames());
        selfAnimation[self.INJURE_STATE] = new Animation(0.5f,self.getInjureFrames());
        opponentAnimation = new Animation[3];
        opponentAnimation[opponent.WALK_STATE] = new Animation(0.5f,opponent.getWalkFrames());
        opponentAnimation[opponent.WALK_STATE].setPlayMode(Animation.PlayMode.LOOP);
        opponentAnimation[opponent.ATTACK_STATE] = new Animation(0.5f,opponent.getAttackFrames());
        opponentAnimation[opponent.INJURE_STATE] = new Animation(0.5f,opponent.getInjureFrames());

        startAI();

    }

    @Override
    public void render(float delta){
        if (gameState!=ONGOING){
            if (gameState==VICTORY)
                game.setScreen(new ResultScreen(game,true));
            else
                game.setScreen(new ResultScreen(game,false));
            dispose();
        }
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        game.batch.draw(battleBg,0,0,0,190,480,800);
        game.batch.draw(bg,0,0,0,10,480,340);

       for (Button button:buttons) {
           game.batch.draw(button.getImage(), button.x, button.y);
           if (button instanceof SkillButton){
               ((SkillButton) button).update();
               int cooldowntime = ((SkillButton) button).getCooldowntime();
               if (cooldowntime>0) {
                   game.font.draw(game.batch, String.valueOf(cooldowntime), button.x+button.getImage().getWidth()/2, button.y + button.getImage().getHeight()/2);
               }
           }
       }

        drawSequence();
        drawCharacter(delta);
//        game.batch.draw(healthBar, 0, 240, camera.viewportWidth * self.getHealthPercentage(), 30);
        game.batch.draw(healthBg,0,240);
        game.batch.draw(healthBar, 0, 240,0,0, 75 + (int) ((camera.viewportWidth-75) * self.getHealthPercentage()), 30);
        game.font.draw(game.batch,self.getName(),0,500);
        game.font.draw(game.batch,opponent.getName(),camera.viewportWidth-game.font.getBounds(opponent.getName()).width,500);
        game.batch.end();

//        if (Gdx.input.isTouched()){
//            game.setScreen(new BattleScreen(game));
//            dispose();
//            forward();
//        }

    }

    private void drawCharacter(float delta) {
        selfStateTime += delta;
        if (selfAnimation[self.getCurrentState()].isAnimationFinished(selfStateTime))
            self.changeState(self.WALK_STATE);
        if (self.isStateChanged())
            selfStateTime = 0;
        game.batch.draw(selfAnimation[self.getCurrentState()].getKeyFrame(selfStateTime),0,340);
        opponentStateTime += delta;
        if (opponentAnimation[opponent.getCurrentState()].isAnimationFinished(opponentStateTime))
            opponent.changeState(self.WALK_STATE);
        if (opponent.isStateChanged())
            opponentStateTime = 0;
        game.batch.draw(opponentAnimation[opponent.getCurrentState()].getKeyFrame(opponentStateTime),320,340);
    }

    private void drawSequence() {
        firstPos -= speed*Gdx.graphics.getDeltaTime();
        if (firstPos<0)
            forward();
        float accumulator = firstPos;
        for (int i=pointer;i<pointer+sequence.length;i++){
            switch (sequence[i%sequence.length]){
                case 'h':
                    game.batch.draw(smallLeft,accumulator,270);
//                    game.batch.draw(smallLeft,accumulator,270,0,40,60,30);
                    break;
                case 'j':
                    game.batch.draw(smallDown,accumulator,270);
                    break;
                case 'k':
                    game.batch.draw(smallUp,accumulator,270);
                    break;
                case 'l':
                    game.batch.draw(smallRight,accumulator,270);
                    break;
                default:
                    game.batch.draw(smallLeft,accumulator,270);
                    break;
            }
            accumulator += smallDown.getWidth();
        }
    }

    @Override
    public void dispose(){
        battleMusic.dispose();
        smallDown.dispose();
        smallLeft.dispose();
        smallRight.dispose();
        smallUp.dispose();
        healthBg.dispose();
        healthBar.dispose();
        battleBg.dispose();
        for (int i=0;i<buttons.size;i++){
            buttons.removeIndex(i);
        }
        AI.stop();
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        camera.unproject(touch.set(screenX,screenY,0));
        for (Button select:buttons) {
            if (select.isClicked(touch.x, touch.y))
                if (select instanceof SkillButton){
                    String skill = select.onRelease();
                    if (skill!=null&&!skill.equals("Defend")) {
                        self.changeState(self.ATTACK_STATE);
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                opponent.changeState(opponent.INJURE_STATE);
                            }
                        }, selfAnimation[self.getCurrentState()].getFrameDuration());
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                if (opponent.isDead())
                                    gameState = VICTORY;
                            }
                        },opponentAnimation[opponent.getCurrentState()].getAnimationDuration());
                    }
                } else if (select.onRelease().charAt(0) == sequence[this.pointer]&&firstPos<camera.viewportWidth) {
                    self.addEnergy(self.getConcentration());
                    forward();
                } else if (select.onRelease().charAt(0)!= sequence[this.pointer]){
                    self.dealRealDamage(5);
                }
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    public void forward(){
        sequence[pointer] = pattern[MathUtils.random(3)];
        pointer = (pointer+1)%sequence.length;
        firstPos += smallDown.getWidth();

    }

    public void startAI(){
        AI = new Timer();
        AI.schedule(new Timer.Task() {
            @Override
            public void run() {
                opponent.changeState(opponent.ATTACK_STATE);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        self.changeState(self.INJURE_STATE);
                    }
                }, opponentAnimation[opponent.getCurrentState()].getFrameDuration());
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        self.dealDamage(opponent.getStrength());
                        if (self.isDead())
                            gameState = DEFEAT;
                    }
                }, opponentAnimation[opponent.getCurrentState()].getAnimationDuration());
            }
        }, opponent.getConcentration(), opponent.getConcentration());
    }
}
