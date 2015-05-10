import android.Keys._
import android.Plugin.androidBuild
import sbt.Keys._
import sbt._

object Settings {

  import LibgdxBuild.libgdxVersion

  lazy val nativeExtractions = SettingKey[Seq[(String, NameFilter, File)]](
    "native-extractions", "(jar name partial, sbt.NameFilter of files to extract, destination directory)"
  )

  lazy val core = plugins.JvmPlugin.projectSettings ++ Seq(
    version := (version in LocalProject("all-platforms")).value,
    libgdxVersion := (libgdxVersion in LocalProject("all-platforms")).value,
    scalaVersion := (scalaVersion in LocalProject("all-platforms")).value,
    resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/public",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % libgdxVersion.value
    ),
    javacOptions ++= Seq(
      "-Xlint",
      "-encoding", "UTF-8",
      "-source", "1.8",
      "-target", "1.8"
    ),
    scalacOptions ++= Seq(
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      "-language:implicitConversions"
    ),
    exportJars := true
  )

  lazy val desktop = core ++ Seq(
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % libgdxVersion.value,
      "com.badlogicgames.gdx" % "gdx-platform" % libgdxVersion.value classifier "natives-desktop"
    ),
    fork in run := true,
    baseDirectory in run := file("android/assets")
  )

  lazy val android = core ++ Tasks.natives ++ androidBuild ++ Seq(
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx-backend-android" % libgdxVersion.value,
      "com.badlogicgames.gdx" % "gdx-platform" % libgdxVersion.value % "natives" classifier "natives-armeabi-v7a"
    ),
    platformTarget in Android := "android-22",
    proguardOptions in Android ++= Seq(
      "-dontobfuscate",
      "-dontoptimize",
      "-keepattributes Signature",
      "-dontwarn scala.collection.**", // required from Scala 2.11.4
      "-dontwarn java.awt.**",
      "-dontwarn javax.swing.**",
      "-dontwarn com.badlogic.gdx.jnigen.**",

      "-keep class com.badlogic.gdx.Application",
      "-keep class com.badlogic.gdx.backends.android.surfaceview.GLSurfaceViewAPI18*",
      "-keep class com.badlogic.gdx.backends.android.AndroidApplicationConfiguration",

      "-dontwarn android.support.**",
      "-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication",
      "-dontwarn com.badlogic.gdx.utils.GdxBuild",
      "-dontwarn com.badlogic.gdx.jnigen.BuildTarget*",
      "-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {<init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);}" ,
      "-keep class com.badlogic.gdx.**",
      "-keepclasseswithmembers class com.badlogic.gdx.graphics.FPSLogger { *;}"
    ),

    nativeExtractions <<= baseDirectory { base => Seq(
      ("gdx-platform-1.\\d.\\d-natives-armeabi-v7a.jar", new ExactFilter("libgdx.so"), base / "libs" / "armeabi-v7a")
    )
    }
  )
}

object Tasks {

  import Settings.nativeExtractions

  lazy val extractNatives = TaskKey[Unit]("extract-natives", "Extracts native files")

  lazy val natives = Seq(
    ivyConfigurations += config("natives"),
    nativeExtractions := Seq.empty,
    extractNatives <<= (nativeExtractions, update) map { (ne, up) =>
      val jars = up.select(configurationFilter("natives"))
      ne foreach { case (jarName, fileFilter, outputPath) =>
        jars find (_.getName.matches(jarName)) map { jar =>
          IO.unzip(jar, outputPath, fileFilter)
        }
      }
    },
    compile in Compile <<= (compile in Compile) dependsOn extractNatives
  )
}

object LibgdxBuild extends Build {
  lazy val libgdxVersion = settingKey[String]("version of Libgdx library")

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = Settings.core
  )

  lazy val desktop = Project(
    id = "desktop",
    base = file("desktop"),
    settings = Settings.desktop
  ).dependsOn(core)

  lazy val android = Project(
    id = "android",
    base = file("android"),
    settings = Settings.android
  ).dependsOn(core)

  lazy val all = Project(
    id = "all-platforms",
    base = file("."),
    settings = Settings.core
  ).aggregate(core, desktop)
}
