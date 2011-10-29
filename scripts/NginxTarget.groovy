includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_Env.groovy")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")
includeTargets << new File("${basedir}/scripts/_NginxCommon.groovy")

target(default: "Point the given nginx at the given target tomcat") {
  depends(password, passphrase)
  //TODO, require NGINX and TOMCAT instance to point at.

  promptForValue("nginxServer", "NGINX server(s) to retarget, currently available ${nginx.collect{ it.name } }")
  promptForValue("tomcatServer", "Tomcat (prod) to point at, currently available ${deployServers.collect{ it.name } }")

  nginxServer.split(",").collect { it.trim() }.each { server ->
    writeNginxConfig(nginx.find { it.name == server}, deployServers.find { it.name == tomcatServer})
  }
  println "-------------------------------------------------------------"
  println "$nginxServer, now targetting ${tomcatServer}"
}
