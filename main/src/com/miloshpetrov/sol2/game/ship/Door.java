package com.miloshpetrov.sol2.game.ship;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJoint;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.*;
import com.miloshpetrov.sol2.game.dra.Dra;
import com.miloshpetrov.sol2.game.dra.RectSprite;
import com.miloshpetrov.sol2.game.input.Pilot;

import java.util.ArrayList;

public class Door {
  public static final float SPD_LEN = .4f;
  public static final float SENSOR_DIST = 3f;
  public static final float DOOR_LEN = 1.1f;
  public static final float MAX_OPEN_AWAIT = DOOR_LEN / SPD_LEN;
  private final PrismaticJoint myJoint;
  private final RectSprite myS;
  private float myOpenAwait;

  public Door(PrismaticJoint joint, RectSprite s) {
    myJoint = joint;
    myS = s;
  }

  public void update(SolGame game, SolShip ship) {
    Vector2 doorPos = getBody().getPosition();
    boolean shouldOpen = shouldOpen(game, ship, doorPos);
    if (shouldOpen) {
      myOpenAwait = MAX_OPEN_AWAIT;
      myJoint.setMotorSpeed(SPD_LEN);
    } else if (myOpenAwait > 0) {
      myOpenAwait -= game.getTimeStep();
      if (myOpenAwait < 0) {
        myJoint.setMotorSpeed(-SPD_LEN);
      }
    }

    Vector2 shipPos = ship.getPos();
    float shipAngle = ship.getAngle();
    SolMath.toRel(doorPos, myS.getRelPos(), shipAngle, shipPos);
  }

  private boolean shouldOpen(SolGame game, SolShip ship, Vector2 doorPos) {
    Fraction frac = ship.getPilot().getFraction();
    FractionMan fracMan = game.getFractionMan();
    for (SolObj o : game.getObjMan().getObjs()) {
      if (o == ship) continue;
      if (!(o instanceof SolShip)) continue;
      SolShip ship2 = (SolShip) o;
      Pilot pilot2 = ship2.getPilot();
      if (!pilot2.isUp()) continue;
      if (fracMan.areEnemies(pilot2.getFraction(), frac)) continue;
      if (ship2.getPos().dst(doorPos) < SENSOR_DIST) return true;
    }
    return false;
  }

  public void collectDras(ArrayList<Dra> dras) {
    dras.add(myS);
  }

  public Body getBody() {
    return myJoint.getBodyB();
  }

  public void onRemove(SolGame game) {
    World w = game.getObjMan().getWorld();
    Body doorBody = getBody();
    w.destroyJoint(myJoint);
    w.destroyBody(doorBody);
  }
}