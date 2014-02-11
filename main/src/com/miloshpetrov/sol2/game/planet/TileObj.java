package com.miloshpetrov.sol2.game.planet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.*;
import com.miloshpetrov.sol2.game.dra.*;

import java.util.ArrayList;
import java.util.List;

public class TileObj implements SolObj {

  private final Planet myPlanet;
  private final float myToPlanetRelAngle;
  private final float myDist;
  private final List<Dra> myDras;
  private final Body myBody;
  private final Vector2 myPos;
  private final float myRadius;

  // for far objs {
  private final float mySize;
  private final Tile myTile;
  // }

  private float myAngle;

  public TileObj(Planet planet, float toPlanetRelAngle, float dist, float size, RectSprite sprite, Body body, Tile tile) {
    myTile = tile;
    myDras = new ArrayList<Dra>();

    myPlanet = planet;
    myToPlanetRelAngle = toPlanetRelAngle;
    myDist = dist;
    mySize = size;
    myBody = body;
    myPos = new Vector2();

    myDras.add(sprite);
    myRadius = DraMan.radiusFromDras(myDras);
    setDependentParams();
  }

  @Override
  public void update(SolGame game) {
    setDependentParams();

    if (myBody != null) {
      float ts = game.getTimeStep();
      Vector2 spd = SolMath.getVec(myPos);
      spd.sub(myBody.getPosition());
      spd.div(ts);
      myBody.setLinearVelocity(spd);
      SolMath.free(spd);
      float bodyAngle = myBody.getAngle() * SolMath.radDeg;
      float av = SolMath.norm(myAngle - bodyAngle) * SolMath.degRad / ts;
      myBody.setAngularVelocity(av);
    }
  }

  private void setDependentParams() {
    float toPlanetAngle = myPlanet.getAngle() + myToPlanetRelAngle;
    SolMath.fromAl(myPos, toPlanetAngle, myDist, true);
    myPos.add(myPlanet.getPos());
    myAngle = toPlanetAngle + 90;
  }

  @Override
  public boolean shouldBeRemoved(SolGame game) {
    return false;
  }

  @Override
  public void onRemove(SolGame game) {
    if (myBody != null) myBody.getWorld().destroyBody(myBody);
  }

  @Override
  public float getRadius() {
    return myRadius;
  }

  @Override
  public void receiveDmg(float dmg, SolGame game, Vector2 pos) {
  }

  @Override
  public boolean receivesGravity() {
    return false;
  }

  @Override
  public void receiveAcc(Vector2 acc, SolGame game) {
  }

  @Override
  public Vector2 getPos() {
    return myPos;
  }

  @Override
  public FarObj toFarObj() {
    return new FarTileObj(myPlanet, myToPlanetRelAngle, myDist, mySize, myRadius, myTile);
  }

  @Override
  public List<Dra> getDras() {
    return myDras;
  }

  @Override
  public float getAngle() {
    return myAngle;
  }

  @Override
  public Vector2 getSpd() {
    return null;
  }

  @Override
  public void handleContact(SolObj other, Contact contact, ContactImpulse impulse, boolean isA, float absImpulse,
    SolGame game)
  {
  }

  @Override
  public String toDebugString() {
    return null;
  }

  public Planet getPlanet() {
    return myPlanet;
  }
}