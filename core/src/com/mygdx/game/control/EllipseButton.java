package com.mygdx.game.control;

import com.badlogic.gdx.math.Circle;

public abstract class EllipseButton extends Button{
    Eclipse  object;

    public EllipseButton(float x, float y, String activeimg, String onclickimg, String inactiveimg) {
        super(activeimg, onclickimg, inactiveimg);
        object = new Circle(x,y,active.getHeight());
    }

    @Override
    public boolean isClicked(float x, float y) {
        return object.contains(x,y);
    }

}