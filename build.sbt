name := "NashornMonads"

version := "1.0"

scalaVersion := "2.11.7"

def copyToGenerated(src: File, outDir: File, streams: TaskStreams): File = {
  val genDir = outDir / "lib" / "generated"
  IO.createDirectory(genDir)
  val genFile = genDir / src.getName
  streams.log.info(s"Copying $src to $genFile")
  IO.copyFile(src, genFile)
  genFile
}

// Copy jvm-npm.js into the main resources folder
resourceGenerators in Compile <+= (resourceManaged in Compile, streams, baseDirectory) map { (outDir: File, streams, baseDir) =>
  val jvmNpmJs = copyToGenerated(baseDir / "jvm-npm" / "src" / "main" / "javascript" / "jvm-npm.js", outDir, streams)
  Seq(jvmNpmJs)
}

// Add node_modules as an unmanaged resources, so that we can load npm modules (e.g. Q) at runtime
unmanagedResourceDirectories in Compile += baseDirectory.value / "node_modules"


libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"