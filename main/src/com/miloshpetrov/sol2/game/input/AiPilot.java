package com.miloshpetrov.sol2.game.input;

import com.badlogic.gdx.math.Vector2;
import com.miloshpetrov.sol2.common.SolMath;
import com.miloshpetrov.sol2.game.Fraction;
import com.miloshpetrov.sol2.game.SolGame;
import com.miloshpetrov.sol2.game.gun.GunItem;
import com.miloshpetrov.sol2.game.planet.Planet;
import com.miloshpetrov.sol2.game.planet.PlanetBind;
import com.miloshpetrov.sol2.game.ship.*;

public class AiPilot implements Pilot {

  private final MoveDestProvider myDestProvider;
  private final boolean myCollectsItems;
  private final Mover myMover;
  private final Shooter myShooter;
  private final Fraction myFraction;
  private final boolean myShootAtObstacles;
  private final String myMapHint;
  private final BattleDestProvider myBattleDestProvider;
  private final float myDetectionDist;

  private PlanetBind myPlanetBind;

  public AiPilot(MoveDestProvider destProvider, boolean collectsItems, Fraction fraction,
    boolean shootAtObstacles, String mapHint, float detectionDist)
  {
    myDestProvider = destProvider;
    myDetectionDist = detectionDist;
    myMover = new Mover();
    myShooter = new Shooter();
    myBattleDestProvider = new BattleDestProvider();
    myCollectsItems = collectsItems;
    myFraction = fraction;
    myShootAtObstacles = shootAtObstacles;
    myMapHint = mapHint;
  }

  @Override
  public void update(SolGame game, SolShip ship, SolShip nearestEnemy) {
    myPlanetBind = null;
    Vector2 shipPos = ship.getPos();
    HullConfig hullConfig = ship.getHull().config;
    float maxIdleDist = hullConfig.getMaxIdleDist();
    myDestProvider.update(game, shipPos, maxIdleDist, hullConfig);

    boolean canShoot = canShoot0(ship);
    Planet np = game.getPlanetMan().getNearestPlanet();
    float shootDist = np.isNearGround(shipPos) ? game.getCam().getGroundViewDist() : game.getCam().getSpaceViewDist();

    Vector2 dest = null;
    boolean shouldStopNearDest = false;
    boolean hasEngine = ship.getHull().getEngine() != null;
    if (hasEngine) {
      Boolean battle = null;
      if (nearestEnemy != null) battle = myDestProvider.shouldBattle(canShoot);
      if (battle != null) {
        dest = myBattleDestProvider.getDest(ship, nearestEnemy, shootDist, np, battle);
        shouldStopNearDest = myBattleDestProvider.shouldStopNearDest();
      } else {
        dest = myDestProvider.getDest();
        shouldStopNearDest = myDestProvider.shouldStopNearDest();
      }
    }

    myMover.update(game, ship, dest, myDestProvider, np, maxIdleDist, hasEngine);
    boolean moverActive = myMover.isActive();

    Vector2 enemyPos = nearestEnemy == null ? null : nearestEnemy.getPos();
    Vector2 enemySpd = nearestEnemy == null ? null : nearestEnemy.getSpd();
    myShooter.update(ship, enemyPos, moverActive, canShoot, enemySpd, shootDist);
    if (hasEngine && !moverActive && !isShooterRotated()) {
      myMover.rotateOnIdle(ship, np, dest, shouldStopNearDest, maxIdleDist);
    }
  }

  private boolean canShoot0(SolShip ship) {
    GunItem g1 = ship.getHull().getGunMount(false).getGun();
    if (g1 != null && g1.canShoot()) return true;
    GunItem g2 = ship.getHull().getGunMount(true).getGun();
    if (g2 != null && (g2.canShoot())) return true;
    return false;
  }

  private boolean isShooterRotated() {
    return myShooter.isLeft() || myShooter.isRight();
  }

  @Override
  public boolean isUp() {
    return myMover.isUp();
  }

  @Override
  public boolean isLeft() {
    return myMover.isLeft() || myShooter.isLeft();
  }

  @Override
  public boolean isRight() {
    return myMover.isRight() || myShooter.isRight();
  }

  @Override
  public boolean isShoot() {
    return myShooter.isShoot();
  }

  @Override
  public boolean isShoot2() {
    return myShooter.isShoot2();
  }

  @Override
  public boolean collectsItems() {
    return myCollectsItems;
  }

  @Override
  public boolean isSpec() {
    return false;
  }

  @Override
  public Fraction getFraction() {
    return myFraction;
  }

  @Override
  public boolean shootsAtObstacles() {
    return myShootAtObstacles;
  }

  @Override
  public float getDetectionDist() {
    return myDetectionDist;
  }

  @Override
  public String getMapHint() {
    return myMapHint;
  }

  @Override
  public void updateFar(SolGame game, FarShip farShip) {
    Vector2 shipPos = farShip.getPos();
    HullConfig hullConfig = farShip.getHullConfig();
    float maxIdleDist = hullConfig.getMaxIdleDist();
    myDestProvider.update(game, shipPos, maxIdleDist, hullConfig);
    Vector2 dest = myDestProvider.getDest();

    Vector2 spd = farShip.getSpd();
    float angle = farShip.getAngle();
    if (dest == null || farShip.getEngine() == null) {
      if (myPlanetBind == null) {
        myPlanetBind = PlanetBind.tryBind(game, shipPos, angle);
      }
      if (myPlanetBind != null) {
        myPlanetBind.setDiff(spd, shipPos);
        spd.scl(1/game.getTimeStep());
        angle += myPlanetBind.getAngleDiff(angle);
      }
    } else {
      float toDestLen = shipPos.dst(dest);
      if (myDestProvider.shouldStopNearDest() && toDestLen < maxIdleDist) {
        spd.set(0, 0);
        // what about angle?
      } else {
        angle = SolMath.angle(shipPos, dest);
        if (myDestProvider.shouldAvoidBigObjs()) {
          angle = myMover.getBigObjAvoider().avoid(game, shipPos, dest, angle);
        }
        SolMath.fromAl(spd, angle, myDestProvider.getDesiredSpdLen());
      }
    }

    farShip.setSpd(spd);
    farShip.setAngle(angle);

    Vector2 newPos = SolMath.getVec(spd);
    newPos.scl(game.getTimeStep());
    newPos.add(shipPos);
    farShip.setPos(newPos);
    SolMath.free(newPos);
  }

}