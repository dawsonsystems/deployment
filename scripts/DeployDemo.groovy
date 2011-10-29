includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")

target(default: "Deploy a build to demo") {
  depends(password, passphrase)

  doDeploy(tomcatServer("demo"))

}

