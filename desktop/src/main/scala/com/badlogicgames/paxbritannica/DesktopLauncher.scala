package com.badlogicgames.paxbritannica

import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import de.swagner.paxbritannica.PaxBritannica

object DesktopLauncher extends App {
  val config: LwjglApplicationConfiguration = new LwjglApplicationConfiguration
  config.width = 1024
  config.height = 550
  config.title = "Pax Britannica"
  new LwjglApplication(new PaxBritannica, config)
}
