includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_Env.groovy")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")
includeTargets << new File("${basedir}/scripts/_NginxCommon.groovy")

target(default: "Will discover which nginx servers are pointing at the given tomcat and point them to a different one") {
  depends(password, passphrase)

  promptForValue("tomcatServer", "Tomcat (prod) to isolate, currently available ${deployServers.collect{ it.name } }")
  promptForValue("fallbackTomcatServer", "Tomcat (prod) to point traffic at, currently available ${deployServers.collect{ it.name } }")

  def tomcatServerHost = deployServers.find { it.name == tomcatServer}
  def fallbackServerHost = deployServers.find { it.name == fallbackTomcatServer}

  isolateTomcat(tomcatServerHost, fallbackServerHost)
}
