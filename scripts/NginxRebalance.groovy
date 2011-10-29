includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_Env.groovy")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")
includeTargets << new File("${basedir}/scripts/_NginxCommon.groovy")

target(default: "Will move all nginx servers back to their preferred host.") {
  depends(password, passphrase)

  rebalanceNginxInstances()
}
